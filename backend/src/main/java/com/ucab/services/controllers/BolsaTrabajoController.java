package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            Integer rif = Integer.parseInt(body.get("rif").toString());
            String cargo = body.get("cargo").toString();
            String perfil = body.get("perfilBuscado").toString();
            String beneficios = body.get("beneficios").toString();
            String responsabilidades = body.get("responsabilidades").toString();

            String sql = "INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades) " +
                         "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, rif, perfil, cargo, beneficios, responsabilidades);

            return ResponseEntity.ok(Map.of("mensaje", "Oportunidad publicada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al publicar: " + e.getMessage()));
        }
    }

    @PostMapping("/postular")
    public ResponseEntity<?> postular(@RequestBody Map<String, Object> body) {
        try {
            Integer rif = Integer.parseInt(body.get("rif").toString());
            Integer ci = Integer.parseInt(body.get("ci").toString());
            String curriculum = body.get("curriculum").toString();
            String fechaHoraStr = body.get("fechaHoraOferta").toString();
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraStr.replace(" ", "T"));

            String sql = "INSERT INTO SePostula(RIF, FechaHoraOferta, CI, Curriculum) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, rif, fechaHora, ci, curriculum);

            return ResponseEntity.ok(Map.of("mensaje", "Postulación enviada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al postular: " + e.getMessage()));
        }
    }

    @GetMapping("/postulaciones")
    public ResponseEntity<List<Map<String, Object>>> getPostulaciones(
            @RequestParam Integer rif, @RequestParam String fechaHoraOferta) {
        LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraOferta.replace(" ", "T"));
        String sql = "SELECT sp.CI, sp.Curriculum, sp.Resultado, sp.FechaHoraOferta, " +
                     "m.PrimerNombre, m.PrimerApellido " +
                     "FROM SePostula sp " +
                     "JOIN Miembro m ON sp.CI = m.CI " +
                     "WHERE sp.RIF = ? AND sp.FechaHoraOferta = ? " +
                     "ORDER BY sp.CI";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, rif, fechaHora);
        return ResponseEntity.ok(result);
    }
}
