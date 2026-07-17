package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bolsa")
@CrossOrigin(origins = "*")
public class BolsaTrabajoController {

    private final JdbcTemplate jdbcTemplate;

    public BolsaTrabajoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/oportunidades")
    public ResponseEntity<List<Map<String, Object>>> getOportunidades() {
        String sql = "SELECT o.RIF, o.FechaHoraOferta, o.PerfilBuscado, o.Cargo, " +
                     "o.Beneficios, o.Responsabilidades, o.EstatusVacante, " +
                     "org.RazonSocial " +
                     "FROM OportunidadLaboral o " +
                     "JOIN OrganizacionExterna org ON o.RIF = org.RIF " +
                     "ORDER BY o.FechaHoraOferta DESC";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/oportunidades")
    public ResponseEntity<?> crearOportunidad(@RequestBody Map<String, Object> body) {
        try {
            String rif = body.get("rif").toString();
            String cargo = body.get("cargo").toString();
            String perfil = body.get("perfilBuscado").toString();
            String beneficios = body.get("beneficios").toString();
            String responsabilidades = body.get("responsabilidades").toString();

            Map<String, Object> inserted = jdbcTemplate.queryForMap(
                "INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades) " +
                "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?) RETURNING FechaHoraOferta",
                rif, perfil, cargo, beneficios, responsabilidades);

            return ResponseEntity.ok(Map.of("mensaje", "Oportunidad publicada correctamente.",
                "fechaHoraOferta", String.valueOf(inserted.get("fechahoraoferta"))));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al publicar: " + e.getMessage()));
        }
    }

    @PostMapping("/postular")
    public ResponseEntity<?> postular(@RequestBody Map<String, Object> body) {
        try {
            String rif = body.get("rif").toString();
            Integer ci = Integer.parseInt(body.get("ci").toString());
            String curriculumBase64 = body.get("curriculum").toString();
            byte[] curriculum = Base64.getDecoder().decode(curriculumBase64);
            // El usuario envía el cargo (no el timestamp) para buscar la oportunidad exacta
            String cargo = String.valueOf(body.getOrDefault("cargo", ""));

            // Validar que el postulante sea un Egresado
            List<Map<String, Object>> egresado = jdbcTemplate.queryForList(
                "SELECT IndiceAcademicoFinal FROM Egresado WHERE CI = ?", ci);
            if (egresado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo los egresados pueden postularse a ofertas laborales."));
            }

            // Buscar la oportunidad por RIF + Cargo y obtener su timestamp exacto
            String exactWhere = "WHERE RIF = ?";
            if (!cargo.isEmpty() && !cargo.equals("null")) {
                exactWhere += " AND Cargo = ?";
            }
            String exactSql = "SELECT FechaHoraOferta FROM OportunidadLaboral " + exactWhere
                + " ORDER BY FechaHoraOferta DESC LIMIT 1";
            List<Map<String, Object>> ops;
            if (!cargo.isEmpty() && !cargo.equals("null")) {
                ops = jdbcTemplate.queryForList(exactSql, rif, cargo);
            } else {
                ops = jdbcTemplate.queryForList(exactSql, rif);
            }
            if (ops.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Oportunidad laboral no encontrada"));
            }
            Object exactTs = ops.get(0).get("fechahoraoferta");
            LocalDateTime exactFechaHora;
            if (exactTs instanceof java.sql.Timestamp) {
                exactFechaHora = ((java.sql.Timestamp) exactTs).toLocalDateTime();
            } else {
                exactFechaHora = LocalDateTime.parse(exactTs.toString().substring(0, 19).replace(" ", "T"));
            }

            String sql = "INSERT INTO SePostula(RIF, FechaHoraOferta, CI, Curriculum) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, rif, exactFechaHora, ci, curriculum);

            return ResponseEntity.ok(Map.of("mensaje", "Postulación enviada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al postular: " + e.getMessage()));
        }
    }

    @GetMapping("/postulaciones")
    public ResponseEntity<List<Map<String, Object>>> getPostulaciones(
            @RequestParam String rif, @RequestParam(required = false) String cargo,
            @RequestParam(required = false) String fechaHoraOferta) {
        // Si recibimos cargo, buscar el timestamp exacto por RIF + cargo
        String exactWhere = "WHERE RIF = ?";
        if (cargo != null && !cargo.isBlank()) {
            exactWhere += " AND Cargo = ?";
        } else if (fechaHoraOferta != null && !fechaHoraOferta.isBlank()) {
            // Fallback: buscar por timestamp aproximado
            LocalDateTime fh;
            try {
                fh = LocalDateTime.parse(fechaHoraOferta.replace(" ", "T").substring(0, 19));
            } catch (Exception e) {
                fh = OffsetDateTime.parse(fechaHoraOferta).toLocalDateTime();
            }
            java.sql.Timestamp paramTs = java.sql.Timestamp.valueOf(fh);
            exactWhere += " AND ? BETWEEN FechaHoraOferta - INTERVAL '1 millisecond' AND FechaHoraOferta + INTERVAL '1 millisecond'";
            List<Map<String, Object>> ops = jdbcTemplate.queryForList(
                "SELECT FechaHoraOferta FROM OportunidadLaboral " + exactWhere, rif, paramTs);
            if (ops.isEmpty()) return ResponseEntity.ok(List.of());
            Object exactTs = ops.get(0).get("fechahoraoferta");
            LocalDateTime exactFechaHora = (exactTs instanceof java.sql.Timestamp)
                ? ((java.sql.Timestamp) exactTs).toLocalDateTime()
                : LocalDateTime.parse(exactTs.toString().substring(0, 19).replace(" ", "T"));
            String sql = "SELECT sp.CI, encode(sp.Curriculum, 'base64') AS Curriculum, sp.Resultado, sp.FechaHoraOferta, "
                + "m.PrimerNombre, m.PrimerApellido FROM SePostula sp "
                + "JOIN Miembro m ON sp.CI = m.CI WHERE sp.RIF = ? AND sp.FechaHoraOferta = ? ORDER BY sp.CI";
            return ResponseEntity.ok(jdbcTemplate.queryForList(sql, rif, exactFechaHora));
        } else {
            return ResponseEntity.ok(List.of());
        }

        // Buscar el timestamp exacto por RIF + cargo
        String exactSql = "SELECT FechaHoraOferta FROM OportunidadLaboral " + exactWhere + " ORDER BY FechaHoraOferta DESC LIMIT 1";
        List<Map<String, Object>> ops;
        if (cargo != null && !cargo.isBlank()) {
            ops = jdbcTemplate.queryForList(exactSql, rif, cargo);
        } else {
            ops = jdbcTemplate.queryForList(exactSql, rif);
        }
        if (ops.isEmpty()) return ResponseEntity.ok(List.of());
        Object exactTs = ops.get(0).get("fechahoraoferta");
        LocalDateTime exactFechaHora = (exactTs instanceof java.sql.Timestamp)
            ? ((java.sql.Timestamp) exactTs).toLocalDateTime()
            : LocalDateTime.parse(exactTs.toString().substring(0, 19).replace(" ", "T"));
        String sql = "SELECT sp.CI, encode(sp.Curriculum, 'base64') AS Curriculum, sp.Resultado, sp.FechaHoraOferta, "
            + "m.PrimerNombre, m.PrimerApellido FROM SePostula sp "
            + "JOIN Miembro m ON sp.CI = m.CI WHERE sp.RIF = ? AND sp.FechaHoraOferta = ? ORDER BY sp.CI";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, rif, exactFechaHora);
        return ResponseEntity.ok(result);
    }
}
