package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/beneficiarios")
@CrossOrigin(origins = "*")
public class BeneficiarioController {

    private final JdbcTemplate jdbcTemplate;

    public BeneficiarioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String obtenerRolActivo(Integer ci) {
        try {
            String sql = "SELECT CASE " +
                         "  WHEN b.CI IS NOT NULL THEN 'Estudiante (Becario)' " +
                         "  WHEN pre.CI IS NOT NULL THEN 'Estudiante (Preparador)' " +
                         "  WHEN est.CI IS NOT NULL THEN 'Estudiante' " +
                         "  WHEN pr.CI IS NOT NULL THEN 'Profesor' " +
                         "  WHEN pa.CI IS NOT NULL THEN 'Personal Administrativo' " +
                         "  WHEN e.CI IS NOT NULL THEN 'Empleado' " +
                         "  ELSE 'Miembro' " +
                         "END FROM Miembro m " +
                         "LEFT JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
                         "LEFT JOIN Estudiante est ON est.CI = r.CI AND est.FechaInicio = r.FechaInicio " +
                         "LEFT JOIN Becario b ON b.CI = est.CI AND b.FechaInicio = est.FechaInicio " +
                         "LEFT JOIN Preparador pre ON pre.CI = est.CI AND pre.FechaInicio = est.FechaInicio " +
                         "LEFT JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
                         "LEFT JOIN Profesor pr ON pr.CI = e.CI AND pr.FechaInicio = e.FechaInicio " +
                         "LEFT JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio " +
                         "WHERE m.CI = ?";
            List<String> roles = jdbcTemplate.queryForList(sql, String.class, ci);
            return roles.isEmpty() ? "Miembro" : roles.get(0);
        } catch (Exception e) {
            return "Miembro";
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBeneficiarios(@RequestParam Integer ci) {
        String sql = "SELECT encode(b.DocumentoIdentidad, 'escape') as DocumentoIdentidad, b.Parentesco, b.PrimerNombre, b.PrimerApellido, " +
                     "b.SegundoNombre, b.SegundoApellido, b.FechaNacimiento, b.FechaInicio, b.FechaFin, " +
                     "b.CentroEducativoInicial, encode(b.EsquemaVacunacion, 'base64') as EsquemaVacunacion, " +
                     "encode(b.ConstanciaEstudiosUniversitarios, 'base64') as ConstanciaEstudiosUniversitarios, " +
                     "encode(b.CertificadoSolteria, 'base64') as CertificadoSolteria " +
                     "FROM Beneficiario b " +
                     "JOIN Registra r ON b.DocumentoIdentidad = r.DocumentoIdentidad " +
                     "WHERE r.CI = ? " +
                     "ORDER BY b.FechaInicio DESC";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, ci);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> crearBeneficiario(@RequestBody Map<String, Object> body) {
        try {
            Integer ci = Integer.parseInt(body.get("ci").toString());
            String parentesco = body.get("parentesco").toString();
            String rol = obtenerRolActivo(ci);

            if (("hijo".equalsIgnoreCase(parentesco) || "conyugue".equalsIgnoreCase(parentesco))
                && !"Profesor".equals(rol) && !"Personal Administrativo".equals(rol)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo profesores y personal administrativo pueden registrar hijos o cónyuges."));
            }

            String documentoIdentidad = body.get("documentoIdentidad").toString();
            String primerNombre = body.get("primerNombre").toString();
            String primerApellido = body.get("primerApellido").toString();
            String fechaNacimientoStr = body.get("fechaNacimiento").toString();
            LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoStr);
            LocalDate fechaInicio = LocalDate.now();

            long edad = java.time.temporal.ChronoUnit.YEARS.between(fechaNacimiento, LocalDate.now());
            boolean requiereConstancia = "hijo".equalsIgnoreCase(parentesco) && edad >= 18;

            String segundoNombre = body.getOrDefault("segundoNombre", "").toString();
            if (segundoNombre.isBlank()) segundoNombre = null;
            String segundoApellido = body.getOrDefault("segundoApellido", "").toString();
            if (segundoApellido.isBlank()) segundoApellido = null;

            String centroEducativo = null;
            Object esquemaVacunacion = null;
            Object constanciaEstudios = null;
            Object certificadoSolteria = null;

            if ("hijo".equalsIgnoreCase(parentesco)) {
                centroEducativo = body.getOrDefault("centroEducativoInicial", "").toString();
                if (centroEducativo.isBlank()) centroEducativo = null;
            }

            String sql = "WITH b AS (INSERT INTO Beneficiario " +
                "(DocumentoIdentidad, Parentesco, PrimerNombre, PrimerApellido, SegundoNombre, SegundoApellido, " +
                "FechaNacimiento, FechaInicio, CentroEducativoInicial, EsquemaVacunacion, " +
                "ConstanciaEstudiosUniversitarios, CertificadoSolteria) " +
                "VALUES (decode(?, 'escape'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING DocumentoIdentidad) " +
                "INSERT INTO Registra(DocumentoIdentidad, CI) SELECT DocumentoIdentidad, ? FROM b";
            jdbcTemplate.update(sql,
                documentoIdentidad, parentesco, primerNombre, primerApellido,
                segundoNombre, segundoApellido, fechaNacimiento, fechaInicio,
                centroEducativo, esquemaVacunacion, constanciaEstudios, certificadoSolteria, ci);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Familiar registrado correctamente.");
            respuesta.put("documentoIdentidad", documentoIdentidad);
            if (requiereConstancia) {
                respuesta.put("advertencia", "El hijo(a) tiene " + edad + " años o más. Se requiere constancia de estudio para continuar como beneficiario.");
            }
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al registrar familiar: " + e.getMessage()));
        }
    }
}
