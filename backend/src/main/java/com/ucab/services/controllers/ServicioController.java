package com.ucab.services.controllers;

import com.ucab.services.models.Servicio;
import com.ucab.services.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "*")
public class ServicioController {

    @Autowired
    private ServicioRepository servicioRepository;

    private final JdbcTemplate jdbcTemplate;

    public ServicioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Servicio> getServicios(@RequestParam(required = false) String categoria) {
        if (categoria != null && !categoria.isEmpty()) {
            return servicioRepository.findByNombreCategoria(categoria);
        }
        return servicioRepository.findAll();
    }

    @PostMapping("/contratar")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> contratarServicio(@RequestBody Map<String, Object> body) {
        try {
            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));

            Object idpObj = body.get("idPrestadora");
            int idPrestadora;
            if (idpObj != null) {
                idPrestadora = Integer.parseInt(idpObj.toString());
            } else {
                var servicios = jdbcTemplate.queryForList(
                    "SELECT IDPrestadora FROM Servicio WHERE NombreCategoria = ? AND Descripcion = ?",
                    nombreCategoria, descripcion);
                if (servicios.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Servicio no encontrado"));
                }
                idPrestadora = (int) servicios.get(0).get("idprestadora");
            }

            // Asegurar IDComprador
            Integer idComprador;
            try {
                idComprador = jdbcTemplate.queryForObject(
                    "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                idComprador = null;
            }
            if (idComprador == null) {
                int newId = (int) (System.currentTimeMillis() % 1000000);
                jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
                jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
                idComprador = newId;
            }

            // Obtener PrecioBase
            Double precioBase = jdbcTemplate.queryForObject(
                "SELECT PrecioBase FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
                Double.class, nombreCategoria, idPrestadora, descripcion);

            // Asegurar Tarifa
            Integer tarifaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Tarifa WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ? AND Perfil = 'miembro activo' AND FechaFin IS NULL",
                Integer.class, nombreCategoria, idPrestadora, descripcion);
            if (tarifaCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) VALUES (?, ?, ?, CURRENT_DATE, 'miembro activo', ?)",
                    nombreCategoria, idPrestadora, descripcion, precioBase);
            }

            // Insertar Tramite
            var tramiteInsertado = jdbcTemplate.queryForMap(
                "INSERT INTO Tramite (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, Estado) VALUES (?, ?, ?, ?, NOW(), 'activo') RETURNING FechaCreacion",
                ci, idPrestadora, nombreCategoria, descripcion);

            Object fcObj = tramiteInsertado.get("fechacreacion");
            java.sql.Timestamp fc;
            if (fcObj instanceof java.sql.Timestamp) {
                fc = (java.sql.Timestamp) fcObj;
            } else if (fcObj instanceof java.sql.Date) {
                fc = new java.sql.Timestamp(((java.sql.Date) fcObj).getTime());
            } else {
                fc = java.sql.Timestamp.valueOf(fcObj.toString());
            }

            // Buscar PersonalAdministrativo
            var admins = jdbcTemplate.queryForList(
                "SELECT pa.CI FROM Miembro m " +
                "JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
                "JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
                "JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio LIMIT 1");
            int responsableCi = admins.isEmpty() ? 0 : (int) admins.get(0).get("ci");

            // Insertar PasoActividad
            jdbcTemplate.update(
                "INSERT INTO PasoActividad (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial, DescripcionInteraccion, Estado, ResponsableAsignado, FechaInicio, DuracionEstimada) VALUES (?, ?, ?, ?, ?, 1, ?, 'en curso', ?, CURRENT_DATE, 0)",
                ci, idPrestadora, nombreCategoria, descripcion, fc, "Contratación de servicio: " + descripcion, responsableCi);

            // Insertar LineaCargo
            jdbcTemplate.update(
                "INSERT INTO LineaCargo (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea, FechaCargo, Cantidad, Concepto, ImpuestosLey) VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, 1, ?, 0)",
                ci, idPrestadora, nombreCategoria, descripcion, fc, fc, descripcion);

            // Datos opcionales del acompañante
            boolean llevaAcompanante = "true".equals(String.valueOf(body.get("llevaAcompanante")));
            if (llevaAcompanante) {
                String acompNom = String.valueOf(body.getOrDefault("acompananteNombres", ""));
                String acompApe = String.valueOf(body.getOrDefault("acompananteApellidos", ""));
                String acompCed = String.valueOf(body.getOrDefault("acompananteCedula", ""));
                if (!acompCed.isEmpty() && !acompNom.isEmpty() && !acompApe.isEmpty()) {
                    int acompCi;
                    try {
                        acompCi = Integer.parseInt(acompCed.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        acompCi = (int) (System.currentTimeMillis() % 1000000);
                    }
                    java.sql.Date fcDate = new java.sql.Date(fc.getTime());
                    jdbcTemplate.update(
                        "INSERT INTO Acompañante (CI, CIMiembro, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, PrimerNombre, PrimerApellido) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (CI) DO UPDATE SET PrimerNombre = EXCLUDED.PrimerNombre, PrimerApellido = EXCLUDED.PrimerApellido, Estado = 'activo'",
                        acompCi, ci, idPrestadora, nombreCategoria, descripcion, fcDate, acompNom, acompApe);
                }
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "Servicio contratado correctamente.",
                "fechaCreacion", fc.toString(),
                "ci", ci,
                "idPrestadora", idPrestadora,
                "nombreCategoria", nombreCategoria,
                "descripcion", descripcion,
                "monto", precioBase));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al contratar el servicio: " + e.getMessage()));
        }
    }
}
