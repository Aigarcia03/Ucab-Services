package com.ucab.services.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/viajes")
@CrossOrigin(origins = "*")
public class ViajeController {

    private final JdbcTemplate jdbcTemplate;

    public ViajeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getViajes(
            @RequestParam(required = false) String sede) {
        String sql;
        String baseSql = "SELECT v.FechaHoraInicio, v.FechaHoraFin, v.Destino, " +
                  "m.Placa, m.TipoVehiculo, m.Capacidad, m.Disponibilidad, " +
                  "m.Capacidad AS CuposDisponibles, " +
                  "s.Ubicacion AS SedeOrigen " +
                  "FROM Viaje v " +
                  "JOIN MedioTransporte m ON v.Placa = m.Placa AND v.CarnetDeConducir = m.CarnetDeConducir " +
                  "JOIN Sede s ON m.Ubicacion = s.Ubicacion ";
        if (sede != null && !sede.isEmpty()) {
            sql = baseSql + "WHERE s.Ubicacion = ? ORDER BY v.FechaHoraInicio DESC";
            return ResponseEntity.ok(jdbcTemplate.queryForList(sql, sede));
        } else {
            sql = baseSql + "ORDER BY v.FechaHoraInicio DESC";
            return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
        }
    }

    @GetMapping("/sedes")
    public ResponseEntity<List<Map<String, Object>>> getSedes() {
        String sql = "SELECT Ubicacion FROM Sede ORDER BY Ubicacion";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(result);
    }
}
