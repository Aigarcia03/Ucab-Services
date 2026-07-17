package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tramites")
@CrossOrigin(origins = "*")
public class TramiteController {

    private final JdbcTemplate jdbcTemplate;

    public TramiteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPorMiembro(@RequestParam int ci) {
        String sql = "SELECT t.*, s.Descripcion as ServicioDescripcion, s.PrecioBase " +
                     "FROM Tramite t " +
                     "LEFT JOIN Servicio s ON t.NombreCategoria = s.NombreCategoria AND t.IDPrestadora = s.IDPrestadora AND t.Descripcion = s.Descripcion " +
                     "WHERE t.CI = ? ORDER BY t.FechaCreacion DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, ci));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearTramite(@RequestBody Map<String, Object> body) {
        try {
            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            LocalDate fechaCreacion = LocalDate.now();

            // 1. Crear trámite
            jdbcTemplate.update(
                "INSERT INTO Tramite (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, Estado) VALUES (?, ?, ?, ?, ?, 'activo')",
                ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

            // 2. Asegurar IDComprador del miembro
            Integer idComprador = jdbcTemplate.queryForObject(
                "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
            if (idComprador == null) {
                int newId = (int) (System.currentTimeMillis() % 1000000);
                jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
                jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
                idComprador = newId;
            }

            // 3. Cerrar EstadoCuenta (creado automáticamente por trigger)
            jdbcTemplate.update(
                "UPDATE EstadoCuenta SET EstadoFiscal = 'cerrado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

            // 4. Crear Tarifa si no existe (necesaria para trigger de LineaCargo)
            Double precioBase = jdbcTemplate.queryForObject(
                "SELECT PrecioBase FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
                Double.class, nombreCategoria, idPrestadora, descripcion);

            Integer tarifaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Tarifa WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ? AND Perfil = 'miembro activo' AND FechaFin IS NULL",
                Integer.class, nombreCategoria, idPrestadora, descripcion);
            if (tarifaCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) VALUES (?, ?, ?, ?, 'miembro activo', ?)",
                    nombreCategoria, idPrestadora, descripcion, fechaCreacion, precioBase);
            }

            // 5. Crear LineaCargo (trigger asigna PrecioUnitario desde Tarifa)
            jdbcTemplate.update(
                "INSERT INTO LineaCargo (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea, FechaCargo, Cantidad, Concepto, ImpuestosLey) VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, 1, ?, 0)",
                ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion, fechaCreacion, descripcion);

            // 6. Crear Factura (trigger calcula Deuda desde LineaCargo, queda 'pendiente')
            jdbcTemplate.update(
                "INSERT INTO Factura (IDComprador, CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) VALUES (?, ?, ?, ?, ?, ?, ?)",
                idComprador, ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion, fechaCreacion);

            return ResponseEntity.ok(Map.of("mensaje", "Trámite creado correctamente. Factura generada como pendiente."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al crear trámite: " + e.getMessage()));
        }
    }

    @PostMapping("/pasos")
    public ResponseEntity<Map<String, Object>> crearPaso(@RequestBody Map<String, Object> body) {
        try {
            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            LocalDate fechaCreacion = LocalDate.parse(String.valueOf(body.get("fechaCreacion")));
            String descripcionInteraccion = String.valueOf(body.get("descripcionInteraccion"));

            Integer maxOrden = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(OrdenSecuencial), 0) + 1 FROM PasoActividad WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                Integer.class, ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

            jdbcTemplate.update(
                "INSERT INTO PasoActividad (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial, DescripcionInteraccion, Estado) VALUES (?, ?, ?, ?, ?, ?, ?, 'en curso')",
                ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion, maxOrden, descripcionInteraccion);

            return ResponseEntity.ok(Map.of("message", "Paso agregado correctamente", "orden", maxOrden));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/pasos/{orden}")
    public ResponseEntity<Map<String, Object>> actualizarPaso(@PathVariable int orden, @RequestBody Map<String, Object> body) {
        try {
            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            LocalDate fechaCreacion = LocalDate.parse(String.valueOf(body.get("fechaCreacion")));
            String estado = String.valueOf(body.getOrDefault("estado", "completado"));

            int updated = jdbcTemplate.update(
                "UPDATE PasoActividad SET Estado = ?, FechaFin = CASE WHEN ? = 'completado' THEN CURRENT_DATE ELSE FechaFin END WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ? AND OrdenSecuencial = ?",
                estado, estado, ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion, orden);

            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Paso no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Paso actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
