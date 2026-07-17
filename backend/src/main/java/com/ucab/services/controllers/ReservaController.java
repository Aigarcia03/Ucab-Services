package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final JdbcTemplate jdbcTemplate;

    public ReservaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/espacios")
    public ResponseEntity<List<Map<String, Object>>> listarEspacios() {
        String sql = "SELECT e.NroIdentificador, e.Direccion, e.Nombre, e.CapacidadMaxima, e.TipoDeMobiliario, " +
                     "ed.Nombre as EdificacionNombre, ed.Ubicacion as SedeUbicacion " +
                     "FROM EspacioFisico e " +
                     "JOIN Edificacion ed ON e.Direccion = ed.Direccion AND e.Nombre = ed.Nombre " +
                     "ORDER BY ed.Ubicacion, ed.Nombre, e.NroIdentificador";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/bloques")
    public ResponseEntity<List<Map<String, Object>>> listarBloques(@RequestParam(required = false) Integer nroIdentificador,
                                                                    @RequestParam(required = false) String direccion,
                                                                    @RequestParam(required = false) String nombre) {
        String sql = "SELECT b.*, e.TipoDeMobiliario, e.CapacidadMaxima " +
                     "FROM BloqueHorario b " +
                     "JOIN EspacioFisico e ON b.NroIdentificador = e.NroIdentificador AND b.Direccion = e.Direccion AND b.Nombre = e.Nombre " +
                     "WHERE b.Disponibilidad = true " +
                     "AND b.FechaHoraInicio > CURRENT_TIMESTAMP " +
                     "ORDER BY b.FechaHoraInicio";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/solicitar")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> solicitarReserva(@RequestBody Map<String, Object> body) {
        int ci = Integer.parseInt(String.valueOf(body.get("ci")));
        int nroIdentificador = Integer.parseInt(String.valueOf(body.get("nroIdentificador")));
        String direccion = String.valueOf(body.get("direccion"));
        String nombre = String.valueOf(body.get("nombre"));
        String fhiStr = String.valueOf(body.get("fechaHoraInicio"));
        LocalDateTime fechaHoraInicio;
        if (fhiStr.endsWith("Z") || fhiStr.contains("+")) {
            fechaHoraInicio = OffsetDateTime.parse(fhiStr.endsWith("Z") ? fhiStr.substring(0, fhiStr.length()-1) + "+00:00" : fhiStr)
                .atZoneSameInstant(ZoneOffset.ofHours(-4)).toLocalDateTime();
        } else {
            fechaHoraInicio = LocalDateTime.parse(fhiStr);
        }
        String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
        String descripcion = String.valueOf(body.get("descripcion"));

        int idPrestadora;
        Object idpObj = body.get("idPrestadora");
        if (idpObj != null) {
            idPrestadora = Integer.parseInt(idpObj.toString());
        } else {
            var servicios = jdbcTemplate.queryForList(
                "SELECT IDPrestadora FROM Servicio WHERE NombreCategoria = ? AND Descripcion = ?",
                nombreCategoria, descripcion);
            if (servicios.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No existe el servicio '" + descripcion + "' en la categoría '" + nombreCategoria + "'."));
            }
            idPrestadora = (int) servicios.get(0).get("idprestadora");
        }

        // Validar bloque disponible (antes de cualquier escritura)
        List<Map<String, Object>> bloque = jdbcTemplate.queryForList(
            "SELECT Disponibilidad, FechaHoraInicio FROM BloqueHorario WHERE NroIdentificador = ? AND Direccion = ? AND Nombre = ? AND FechaHoraInicio = ?",
            nroIdentificador, direccion, nombre, fechaHoraInicio);
        if (bloque.isEmpty() || !Boolean.TRUE.equals(bloque.get(0).get("disponibilidad"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El bloque seleccionado ya no está disponible."));
        }

        // Buscar PersonalAdministrativo (ANTES de escribir)
        var admins = jdbcTemplate.queryForList(
            "SELECT pa.CI FROM Miembro m " +
            "JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
            "JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
            "JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio LIMIT 1");
        if (admins.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No hay Personal Administrativo disponible."));
        }
        int responsableCi = (int) admins.get(0).get("ci");

        // Asegurar IDComprador (ANTES de escribir)
        Integer idComprador = jdbcTemplate.queryForObject(
            "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
        if (idComprador == null) {
            int newId = (int) (System.currentTimeMillis() % 1000000);
            jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
            jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
            idComprador = newId;
        }

        // Obtener PrecioBase y verificar Tarifa (ANTES de escribir)
        Double precioBase = jdbcTemplate.queryForObject(
            "SELECT PrecioBase FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
            Double.class, nombreCategoria, idPrestadora, descripcion);
        Integer tarifaCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM Tarifa WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ? AND Perfil = 'miembro activo' AND FechaFin IS NULL",
            Integer.class, nombreCategoria, idPrestadora, descripcion);

        // === AQUÍ EMPIEZAN LAS ESCRITURAS ===

        // Insertar Tramite y obtener el FechaCreacion realmente almacenado (puede ser
        // modificado por triggers, ej. de DATE a TIMESTAMP o viceversa)
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

        // Datos opcionales del acompañante
        boolean llevaAcompanante = "true".equals(String.valueOf(body.get("llevaAcompanante")));
        String acompNom = String.valueOf(body.getOrDefault("acompananteNombres", ""));
        String acompApe = String.valueOf(body.getOrDefault("acompananteApellidos", ""));
        String acompCed = String.valueOf(body.getOrDefault("acompananteCedula", ""));
        String detalles = "Solicitud de reserva de espacio";
        if (llevaAcompanante) {
            detalles += " | Acompañante: " + acompNom + " " + acompApe + " (Cédula: " + acompCed + ")";
        }
        if ("true".equals(String.valueOf(body.get("llevaEquipamiento")))) {
            detalles += " | Lleva equipamiento propio";
        }

        jdbcTemplate.update(
            "INSERT INTO PasoActividad (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial, DescripcionInteraccion, Estado, ResponsableAsignado, FechaInicio, DuracionEstimada) VALUES (?, ?, ?, ?, ?, 1, ?, 'en curso', ?, CURRENT_DATE, 0)",
            ci, idPrestadora, nombreCategoria, descripcion, fc, detalles, responsableCi);

        if (tarifaCount == 0) {
            jdbcTemplate.update(
                "INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) VALUES (?, ?, ?, CURRENT_DATE, 'miembro activo', ?)",
                nombreCategoria, idPrestadora, descripcion, precioBase);
        }

        jdbcTemplate.update(
            "INSERT INTO LineaCargo (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea, FechaCargo, Cantidad, Concepto, ImpuestosLey) VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, 1, ?, 0)",
            ci, idPrestadora, nombreCategoria, descripcion, fc, fc, descripcion);

        // Insertar acompañante en tabla dedicada si lleva
        if (llevaAcompanante && !acompCed.isEmpty() && !acompNom.isEmpty() && !acompApe.isEmpty()) {
            int acompCi;
            try {
                acompCi = Integer.parseInt(acompCed.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                acompCi = (int) (System.currentTimeMillis() % 1000000);
            }
            Date fcDate = new Date(fc.getTime());
            jdbcTemplate.update(
                "INSERT INTO Acompañante (CI, CIMiembro, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, PrimerNombre, PrimerApellido) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (CI) DO UPDATE SET PrimerNombre = EXCLUDED.PrimerNombre, PrimerApellido = EXCLUDED.PrimerApellido, Estado = 'activo'",
                acompCi, ci, idPrestadora, nombreCategoria, descripcion, fcDate, acompNom, acompApe);
        }

        jdbcTemplate.update(
            "UPDATE BloqueHorario SET Disponibilidad = false WHERE NroIdentificador = ? AND Direccion = ? AND Nombre = ? AND FechaHoraInicio = ?",
            nroIdentificador, direccion, nombre, fechaHoraInicio);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Reserva creada correctamente. Puedes pagar ahora o después.",
            "fechaCreacion", fc.toString(),
            "ci", ci,
            "idPrestadora", idPrestadora,
            "nombreCategoria", nombreCategoria,
            "descripcion", descripcion));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Error al crear la reserva: " + e.getMessage()));
    }
}
