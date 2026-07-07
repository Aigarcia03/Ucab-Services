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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getBeneficiarios(@RequestParam Integer ci) {
        String sql = "SELECT b.DocumentoIdentidad, b.Parentesco, b.PrimerNombre, b.PrimerApellido, " +
                     "b.SegundoNombre, b.SegundoApellido, b.FechaNacimiento, b.FechaInicio, b.FechaFin, " +
                     "b.CentroEducativoInicial, b.EsquemaVacunacion, " +
                     "b.ConstanciaEstudiosUniversitarios, b.CertificadoSolteria " +
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
            String documentoIdentidad = body.get("documentoIdentidad").toString();
            String parentesco = body.get("parentesco").toString();
            String primerNombre = body.get("primerNombre").toString();
            String primerApellido = body.get("primerApellido").toString();
            String fechaNacimientoStr = body.get("fechaNacimiento").toString();
            LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoStr);
            LocalDate fechaInicio = LocalDate.now();

            String segundoNombre = body.getOrDefault("segundoNombre", "").toString();
            if (segundoNombre.isBlank()) segundoNombre = null;
            String segundoApellido = body.getOrDefault("segundoApellido", "").toString();
            if (segundoApellido.isBlank()) segundoApellido = null;

            String centroEducativo = null;
            String esquemaVacunacion = null;
            String constanciaEstudios = null;
            String certificadoSolteria = null;

            if ("hijo".equalsIgnoreCase(parentesco)) {
                centroEducativo = body.getOrDefault("centroEducativoInicial", "").toString();
                if (centroEducativo.isBlank()) centroEducativo = null;
                esquemaVacunacion = body.getOrDefault("esquemaVacunacion", "").toString();
                if (esquemaVacunacion.isBlank()) esquemaVacunacion = null;
            } else if ("conyugue".equalsIgnoreCase(parentesco)) {
                constanciaEstudios = body.getOrDefault("constanciaEstudiosUniversitarios", "").toString();
                if (constanciaEstudios.isBlank()) constanciaEstudios = null;
                certificadoSolteria = body.getOrDefault("certificadoSolteria", "").toString();
                if (certificadoSolteria.isBlank()) certificadoSolteria = null;
            }

            String sqlBeneficiario = "INSERT INTO Beneficiario " +
                "(DocumentoIdentidad, Parentesco, PrimerNombre, PrimerApellido, SegundoNombre, SegundoApellido, " +
                "FechaNacimiento, FechaInicio, CentroEducativoInicial, EsquemaVacunacion, " +
                "ConstanciaEstudiosUniversitarios, CertificadoSolteria) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sqlBeneficiario,
                documentoIdentidad, parentesco, primerNombre, primerApellido,
                segundoNombre, segundoApellido, fechaNacimiento, fechaInicio,
                centroEducativo, esquemaVacunacion, constanciaEstudios, certificadoSolteria);

            String sqlRegistra = "INSERT INTO Registra(DocumentoIdentidad, CI) VALUES (?, ?)";
            jdbcTemplate.update(sqlRegistra, documentoIdentidad, ci);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Familiar registrado correctamente.",
                "documentoIdentidad", documentoIdentidad
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al registrar familiar: " + e.getMessage()));
        }
    }
}
