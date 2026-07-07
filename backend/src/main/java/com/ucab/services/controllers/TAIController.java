package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tai")
@CrossOrigin(origins = "*")
public class TAIController {

    private final JdbcTemplate jdbcTemplate;

    public TAIController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getTAI(@RequestParam Integer ci) {
        String sql = "SELECT t.POS, t.FechaHoraPago, t.UID, t.IDPresencial, " +
                     "f.Numero AS FacturaNumero, f.Estatus, f.Deuda, f.MontoAcumulado, f.FechaHoraEmision, " +
                     "f.FechaHoraPago AS FacturaFechaPago, m.MontoRecibido " +
                     "FROM TAI t " +
                     "LEFT JOIN PagoPresencial pp ON t.IDPresencial = pp.IDPresencial " +
                     "LEFT JOIN MetodoPago m ON pp.IDMetodo = m.IDMetodo " +
                     "LEFT JOIN Paga p ON m.IDMetodo = p.IDMetodo " +
                     "LEFT JOIN Factura f ON p.NumeroFactura = f.Numero " +
                     "WHERE t.UID = ? " +
                     "ORDER BY t.FechaHoraPago DESC";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, ci);

        Map<String, Object> response = new HashMap<>();
        response.put("registros", results);

        if (!results.isEmpty()) {
            Map<String, Object> info = new HashMap<>();
            info.put("pos", results.get(0).get("pos"));
            info.put("uid", results.get(0).get("uid"));
            response.put("tai", info);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/abono")
    public ResponseEntity<?> crearAbono(@RequestBody Map<String, Object> body) {
        try {
            Integer ci = Integer.parseInt(body.get("ci").toString());
            Integer pos = Integer.parseInt(body.get("pos").toString());
            Integer monto = Integer.parseInt(body.get("monto").toString());
            LocalDateTime ahora = LocalDateTime.now();

            jdbcTemplate.update(
                "INSERT INTO Tasa(FechaHoraVigencia, CambioReal) VALUES (?, ?)",
                ahora, 1.0
            );

            jdbcTemplate.update(
                "INSERT INTO MetodoPago(IDMetodo, FechaHoraVigencia, MontoRecibido) VALUES (?, ?, ?)",
                (int) (System.currentTimeMillis() % 1000000), ahora, monto
            );

            Integer idMetodo = jdbcTemplate.queryForObject(
                "SELECT MAX(IDMetodo) FROM MetodoPago WHERE FechaHoraVigencia = ?", Integer.class, ahora
            );

            jdbcTemplate.update(
                "INSERT INTO PagoPresencial(IDPresencial, IDMetodo) VALUES (?, ?)",
                (int) ((System.currentTimeMillis() + 1) % 1000000), idMetodo
            );

            Integer idPresencial = jdbcTemplate.queryForObject(
                "SELECT MAX(IDPresencial) FROM PagoPresencial WHERE IDMetodo = ?", Integer.class, idMetodo
            );

            jdbcTemplate.update(
                "INSERT INTO TAI(POS, FechaHoraPago, UID, IDPresencial) VALUES (?, ?, ?, ?)",
                pos, ahora, ci, idPresencial
            );

            return ResponseEntity.ok(Map.of(
                "mensaje", "Abono registrado correctamente.",
                "pos", pos, "monto", monto, "fecha", ahora.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al registrar el abono: " + e.getMessage()));
        }
    }
}
