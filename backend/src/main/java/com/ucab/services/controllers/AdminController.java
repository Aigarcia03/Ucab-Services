package com.ucab.services.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final JdbcTemplate jdbcTemplate;

    public AdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ─── HU-03: Modificar estatus de miembros ───────────────────────────
    @GetMapping("/miembros")
    public ResponseEntity<List<Map<String, Object>>> listarMiembros() {
        String sql = "SELECT m.CI, m.PrimerNombre, m.PrimerApellido, m.CorreoInstitucional, m.Categoria, m.EstadoCuenta, " +
                     "CASE " +
                     "  WHEN b.CI IS NOT NULL THEN 'Estudiante (Becario)' " +
                     "  WHEN pre.CI IS NOT NULL THEN 'Estudiante (Preparador)' " +
                     "  WHEN est.CI IS NOT NULL THEN 'Estudiante' " +
                     "  WHEN pr.CI IS NOT NULL THEN 'Profesor' " +
                     "  WHEN pa.CI IS NOT NULL THEN 'Personal Administrativo' " +
                     "  WHEN e.CI IS NOT NULL THEN 'Empleado' " +
                     "  ELSE 'Miembro' " +
                     "END AS activeRole " +
                     "FROM Miembro m " +
                     "LEFT JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
                     "LEFT JOIN Estudiante est ON est.CI = r.CI AND est.FechaInicio = r.FechaInicio " +
                     "LEFT JOIN Becario b ON b.CI = est.CI AND b.FechaInicio = est.FechaInicio " +
                     "LEFT JOIN Preparador pre ON pre.CI = est.CI AND pre.FechaInicio = est.FechaInicio " +
                     "LEFT JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
                     "LEFT JOIN Profesor pr ON pr.CI = e.CI AND pr.FechaInicio = e.FechaInicio " +
                     "LEFT JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio " +
                     "ORDER BY m.CI";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PutMapping("/miembros/{ci}/estatus")
    public ResponseEntity<Map<String, Object>> actualizarEstatus(@PathVariable int ci, @RequestBody Map<String, String> body) {
        String nuevoEstatus = body.get("estatus");
        if (nuevoEstatus == null || (!nuevoEstatus.equals("activa") && !nuevoEstatus.equals("suspendida") && !nuevoEstatus.equals("bloqueada"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estatus inválido. Use: activa, suspendida o bloqueada"));
        }
        try {
            int updated = jdbcTemplate.update("UPDATE Miembro SET EstadoCuenta = ? WHERE CI = ?", nuevoEstatus, ci);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Miembro no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Estatus actualizado a " + nuevoEstatus));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HU-04: Inactivar miembro (validar pendientes) ─────────────────
    @DeleteMapping("/miembros/{ci}")
    public ResponseEntity<Map<String, Object>> inactivarMiembro(@PathVariable int ci) {
        try {
            // Verificar facturas pendientes
            Integer facturasPendientes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Factura f JOIN EstadoCuenta ec ON f.CI = ec.CI AND f.IDPrestadora = ec.IDPrestadora AND f.NombreCategoria = ec.NombreCategoria AND f.Descripcion = ec.Descripcion AND f.FechaCreacion = ec.FechaCreacion AND f.MesAñoApertura = ec.MesAñoApertura WHERE ec.CI = ? AND f.Estatus = 'pendiente'",
                Integer.class, ci);
            if (facturasPendientes != null && facturasPendientes > 0) {
                return ResponseEntity.status(409).body(Map.of("error", "El miembro tiene " + facturasPendientes + " factura(s) pendiente(s). No se puede inactivar."));
            }
            // Verificar trámites activos
            Integer tramitesActivos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Tramite WHERE CI = ? AND Estado = 'activo'", Integer.class, ci);
            if (tramitesActivos != null && tramitesActivos > 0) {
                return ResponseEntity.status(409).body(Map.of("error", "El miembro tiene " + tramitesActivos + " trámite(s) activo(s). No se puede inactivar."));
            }
            jdbcTemplate.update("UPDATE Miembro SET EstadoCuenta = 'suspendida' WHERE CI = ?", ci);
            return ResponseEntity.ok(Map.of("message", "Miembro inactivado (suspendido) correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Finalizar rol activo ────────────────────────────────────────
    @PutMapping("/miembros/{ci}/rol/finalizar")
    public ResponseEntity<Map<String, Object>> finalizarRol(@PathVariable int ci, @RequestBody Map<String, String> body) {
        try {
            String fechaFinStr = body.get("fechaFin");
            if (fechaFinStr == null) return ResponseEntity.badRequest().body(Map.of("error", "Debe especificar 'fechaFin'"));
            LocalDate fechaFin = LocalDate.parse(fechaFinStr);
            int updated = jdbcTemplate.update("UPDATE Rol SET FechaFin = ? WHERE CI = ? AND FechaFin IS NULL", fechaFin, ci);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "No hay rol activo para este miembro"));
            return ResponseEntity.ok(Map.of("message", "Rol finalizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al finalizar rol: " + e.getMessage()));
        }
    }

    // ─── Asignar rol académico ────────────────────────────────────────
    @PostMapping("/miembros/{ci}/rol")
    public ResponseEntity<Map<String, Object>> asignarRol(@PathVariable int ci, @RequestBody Map<String, String> body) {
        try {
            String rol = body.get("rol");
            String fechaInicioStr = body.get("fechaInicio");
            if (rol == null || fechaInicioStr == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Debe especificar 'rol' y 'fechaInicio'"));
            LocalDate fechaInicio = LocalDate.parse(fechaInicioStr);
            java.sql.Date sqlFechaInicio = java.sql.Date.valueOf(fechaInicio);

            // Cerrar rol activo anterior si existe
            jdbcTemplate.update("UPDATE Rol SET FechaFin = ? WHERE CI = ? AND FechaFin IS NULL", fechaInicio, ci);

            // Construir SQL y parámetros según el rol
            Object result = generarSqlYParams(rol, body, ci, sqlFechaInicio);
            if (result instanceof String) return ResponseEntity.badRequest().body(Map.of("error", (String) result));

            Object[] data = (Object[]) result;
            String sql = (String) data[0];
            Object[] params = (Object[]) data[1];
            jdbcTemplate.update(sql, params);

            return ResponseEntity.ok(Map.of("message", "Rol " + rol + " asignado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al asignar rol: " + e.getMessage()));
        }
    }

    private Object generarSqlYParams(String rol, Map<String, String> body, int ci, java.sql.Date fechaInicio) {
        try {
            switch (rol) {
                case "Estudiante": {
                    String esc = body.getOrDefault("escuela", "");
                    String fac = body.getOrDefault("facultadAdscripcion", "");
                    int sem = Integer.parseInt(body.getOrDefault("semestre", "1"));
                    int uca = Integer.parseInt(body.getOrDefault("unidadesCreditoAprobadas", "0"));
                    double prom = Double.parseDouble(body.getOrDefault("promedioPonderado", "0"));
                    String sql = "WITH r AS (INSERT INTO Rol (CI, FechaInicio) VALUES (?, ?) RETURNING CI, FechaInicio) "
                        + "INSERT INTO Estudiante (CI, FechaInicio, Semestre, Escuela, UnidadesCreditoAprobadas, PromedioPonderado, FacultadAdscripcion) "
                        + "SELECT CI, FechaInicio, ?, ?, ?, ?, ? FROM r";
                    return new Object[]{sql, new Object[]{ci, fechaInicio, sem, esc, uca, prom, fac}};
                }
                case "Estudiante (Becario)": {
                    String esc = body.getOrDefault("escuela", "");
                    String fac = body.getOrDefault("facultadAdscripcion", "");
                    int sem = Integer.parseInt(body.getOrDefault("semestre", "1"));
                    int uca = Integer.parseInt(body.getOrDefault("unidadesCreditoAprobadas", "0"));
                    double prom = Double.parseDouble(body.getOrDefault("promedioPonderado", "0"));
                    String tipoBeca = body.getOrDefault("tipoBeca", "ayuda económica");
                    String sql = "WITH r AS (INSERT INTO Rol (CI, FechaInicio) VALUES (?, ?) RETURNING CI, FechaInicio), "
                        + "e AS (INSERT INTO Estudiante (CI, FechaInicio, Semestre, Escuela, UnidadesCreditoAprobadas, PromedioPonderado, FacultadAdscripcion) "
                        + "SELECT CI, FechaInicio, ?, ?, ?, ?, ? FROM r RETURNING CI, FechaInicio) "
                        + "INSERT INTO Becario (CI, FechaInicio, TipoBeca) SELECT CI, FechaInicio, ? FROM e";
                    return new Object[]{sql, new Object[]{ci, fechaInicio, sem, esc, uca, prom, fac, tipoBeca}};
                }
                case "Estudiante (Preparador)": {
                    String esc = body.getOrDefault("escuela", "");
                    String fac = body.getOrDefault("facultadAdscripcion", "");
                    int sem = Integer.parseInt(body.getOrDefault("semestre", "1"));
                    int uca = Integer.parseInt(body.getOrDefault("unidadesCreditoAprobadas", "0"));
                    double prom = Double.parseDouble(body.getOrDefault("promedioPonderado", "0"));
                    String asig = body.getOrDefault("asignatura", "");
                    int horas = Integer.parseInt(body.getOrDefault("horasAyudantia", "0"));
                    String sql = "WITH r AS (INSERT INTO Rol (CI, FechaInicio) VALUES (?, ?) RETURNING CI, FechaInicio), "
                        + "e AS (INSERT INTO Estudiante (CI, FechaInicio, Semestre, Escuela, UnidadesCreditoAprobadas, PromedioPonderado, FacultadAdscripcion) "
                        + "SELECT CI, FechaInicio, ?, ?, ?, ?, ? FROM r RETURNING CI, FechaInicio) "
                        + "INSERT INTO Preparador (CI, FechaInicio, Asignatura, HorasAyudantia) SELECT CI, FechaInicio, ?, ? FROM e";
                    return new Object[]{sql, new Object[]{ci, fechaInicio, sem, esc, uca, prom, fac, asig, horas}};
                }
                case "Profesor": {
                    int carga = Integer.parseInt(body.getOrDefault("cargaHorariaSemanal", "0"));
                    String escal = body.getOrDefault("escalafonDocente", "");
                    String cod = body.getOrDefault("codigoInvestigador", "");
                    String sql = "WITH r AS (INSERT INTO Rol (CI, FechaInicio) VALUES (?, ?) RETURNING CI, FechaInicio), "
                        + "em AS (INSERT INTO Empleado (CI, FechaInicio, CargaHorariaSemanal) SELECT CI, FechaInicio, ? FROM r RETURNING CI, FechaInicio) "
                        + "INSERT INTO Profesor (CI, FechaInicio, EscalafonDocente, CodigoInvestigador) SELECT CI, FechaInicio, ?, ? FROM em";
                    return new Object[]{sql, new Object[]{ci, fechaInicio, carga, escal, cod}};
                }
                case "Personal Administrativo": {
                    int carga = Integer.parseInt(body.getOrDefault("cargaHorariaSemanal", "0"));
                    String unidad = body.getOrDefault("unidadAdscripcionPresupuestaria", "");
                    String cargo = body.getOrDefault("cargoAdministrativo", "");
                    String sql = "WITH r AS (INSERT INTO Rol (CI, FechaInicio) VALUES (?, ?) RETURNING CI, FechaInicio), "
                        + "em AS (INSERT INTO Empleado (CI, FechaInicio, CargaHorariaSemanal) SELECT CI, FechaInicio, ? FROM r RETURNING CI, FechaInicio) "
                        + "INSERT INTO PersonalAdministrativo (CI, FechaInicio, UnidadAdscripcionPresupuestaria, CargoAdministrativo) SELECT CI, FechaInicio, ?, ? FROM em";
                    return new Object[]{sql, new Object[]{ci, fechaInicio, carga, unidad, cargo}};
                }
                default:
                    return "Rol inválido: " + rol;
            }
        } catch (NumberFormatException e) {
            return "Formato numérico inválido: " + e.getMessage();
        }
    }

    // ─── HU-05: CRUD Sedes ──────────────────────────────────────────────
    @GetMapping("/sedes")
    public ResponseEntity<List<Map<String, Object>>> listarSedes() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT * FROM Sede ORDER BY Ubicacion"));
    }

    @PostMapping("/sedes")
    public ResponseEntity<Map<String, Object>> crearSede(@RequestBody Map<String, String> body) {
        String ubicacion = body.get("ubicacion");
        if (ubicacion == null || ubicacion.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Ubicación requerida"));
        if (!ubicacion.equals("Montalbán") && !ubicacion.equals("Guayana")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ubicación debe ser Montalbán o Guayana"));
        }
        try {
            jdbcTemplate.update("INSERT INTO Sede (Ubicacion) VALUES (?)", ubicacion);
            return ResponseEntity.ok(Map.of("message", "Sede creada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sedes/{ubicacion}")
    public ResponseEntity<Map<String, Object>> eliminarSede(@PathVariable String ubicacion) {
        try {
            int updated = jdbcTemplate.update("DELETE FROM Sede WHERE Ubicacion = ?", ubicacion);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Sede no encontrada"));
            return ResponseEntity.ok(Map.of("message", "Sede eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se puede eliminar: " + e.getMessage()));
        }
    }

    // ─── HU-05: CRUD Edificaciones ──────────────────────────────────────
    @GetMapping("/edificaciones")
    public ResponseEntity<List<Map<String, Object>>> listarEdificaciones() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT e.*, s.Ubicacion as SedeUbicacion FROM Edificacion e JOIN Sede s ON e.Ubicacion = s.Ubicacion ORDER BY e.Nombre"));
    }

    @PostMapping("/edificaciones")
    public ResponseEntity<Map<String, Object>> crearEdificacion(@RequestBody Map<String, String> body) {
        String direccion = body.get("direccion");
        String nombre = body.get("nombre");
        String ubicacion = body.get("ubicacion");
        if (direccion == null || nombre == null || ubicacion == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dirección, nombre y ubicación requeridos"));
        }
        try {
            jdbcTemplate.update("INSERT INTO Edificacion (Direccion, Nombre, Ubicacion) VALUES (?, ?, ?)", direccion, nombre, ubicacion);
            return ResponseEntity.ok(Map.of("message", "Edificación creada correctamente. Advertencia: no tiene espacios físicos asociados.", "sinEspacios", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/edificaciones")
    public ResponseEntity<Map<String, Object>> eliminarEdificacion(@RequestBody Map<String, String> body) {
        try {
            int updated = jdbcTemplate.update("DELETE FROM Edificacion WHERE Direccion = ? AND Nombre = ?", body.get("direccion"), body.get("nombre"));
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Edificación no encontrada"));
            return ResponseEntity.ok(Map.of("message", "Edificación eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se puede eliminar: " + e.getMessage()));
        }
    }

    // ─── HU-05: CRUD Espacios Físicos ───────────────────────────────────
    @GetMapping("/espacios")
    public ResponseEntity<List<Map<String, Object>>> listarEspacios() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT e.*, ed.Nombre as EdificacionNombre, ed.Ubicacion as SedeUbicacion FROM EspacioFisico e JOIN Edificacion ed ON e.Direccion = ed.Direccion AND e.Nombre = ed.Nombre ORDER BY e.NroIdentificador"));
    }

    @PostMapping("/espacios")
    public ResponseEntity<Map<String, Object>> crearEspacio(@RequestBody Map<String, Object> body) {
        try {
            int nroId = Integer.parseInt(String.valueOf(body.get("nroIdentificador")));
            String direccion = String.valueOf(body.get("direccion"));
            String nombre = String.valueOf(body.get("nombre"));
            int capacidad = Integer.parseInt(String.valueOf(body.get("capacidadMaxima")));
            String mobiliario = String.valueOf(body.get("tipoDeMobiliario"));
            if (capacidad <= 0) return ResponseEntity.badRequest().body(Map.of("error", "Capacidad debe ser mayor a 0"));
            jdbcTemplate.update("INSERT INTO EspacioFisico (NroIdentificador, Direccion, Nombre, CapacidadMaxima, TipoDeMobiliario) VALUES (?, ?, ?, ?, ?)",
                nroId, direccion, nombre, capacidad, mobiliario);
            return ResponseEntity.ok(Map.of("message", "Espacio físico creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/espacios")
    public ResponseEntity<Map<String, Object>> eliminarEspacio(@RequestBody Map<String, Object> body) {
        try {
            int nroId = Integer.parseInt(String.valueOf(body.get("nroIdentificador")));
            String direccion = String.valueOf(body.get("direccion"));
            String nombre = String.valueOf(body.get("nombre"));
            int updated = jdbcTemplate.update("DELETE FROM EspacioFisico WHERE NroIdentificador = ? AND Direccion = ? AND Nombre = ?",
                nroId, direccion, nombre);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Espacio no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Espacio físico eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/espacios")
    public ResponseEntity<Map<String, Object>> actualizarEspacio(@RequestBody Map<String, Object> body) {
        try {
            int nroId = Integer.parseInt(String.valueOf(body.get("nroIdentificador")));
            String direccion = String.valueOf(body.get("direccion"));
            String nombre = String.valueOf(body.get("nombre"));
            int capacidad = Integer.parseInt(String.valueOf(body.get("capacidadMaxima")));
            String mobiliario = String.valueOf(body.get("tipoDeMobiliario"));
            if (capacidad <= 0) return ResponseEntity.badRequest().body(Map.of("error", "Capacidad debe ser mayor a 0"));
            int updated = jdbcTemplate.update(
                "UPDATE EspacioFisico SET CapacidadMaxima = ?, TipoDeMobiliario = ? WHERE NroIdentificador = ? AND Direccion = ? AND Nombre = ?",
                capacidad, mobiliario, nroId, direccion, nombre);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Espacio no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Espacio físico actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HU-06: CRUD Cursos ─────────────────────────────────────────────
    @GetMapping("/cursos")
    public ResponseEntity<List<Map<String, Object>>> listarCursosAdmin() {
        String sql = "SELECT c.Materia, c.FechaInicio, c.FechaFin, " +
                     "(SELECT m.PrimerNombre || ' ' || m.PrimerApellido FROM Asiste a JOIN Miembro m ON a.CIProfesor = m.CI WHERE a.Materia = c.Materia AND a.FechaInicio = c.FechaInicio LIMIT 1) AS ProfesorNombre " +
                     "FROM Curso c ORDER BY c.FechaInicio DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/cursos/{materia}/{fechaInicio}/estudiantes-admin")
    public ResponseEntity<List<Map<String, Object>>> listarEstudiantesCurso(@PathVariable String materia, @PathVariable String fechaInicio) {
        String sql = "SELECT a.CIEstudiante, m.PrimerNombre, m.PrimerApellido, a.Nota " +
                     "FROM Asiste a JOIN Miembro m ON a.CIEstudiante = m.CI " +
                     "WHERE a.Materia = ? AND a.FechaInicio = ? AND a.CIEstudiante <> a.CIProfesor";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, materia, LocalDate.parse(fechaInicio)));
    }

    @PostMapping("/cursos")
    public ResponseEntity<Map<String, Object>> crearCurso(@RequestBody Map<String, Object> body) {
        String materia = String.valueOf(body.get("materia"));
        String fechaInicioStr = String.valueOf(body.get("fechaInicio"));
        String fechaFinStr = body.get("fechaFin") != null ? String.valueOf(body.get("fechaFin")) : null;
        if (fechaFinStr != null && fechaFinStr.isBlank()) fechaFinStr = null;
        if (materia.isBlank() || fechaInicioStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Materia y fecha de inicio requeridas"));
        }
        try {
            LocalDate fechaInicio = LocalDate.parse(fechaInicioStr);
            LocalDate fechaFin = fechaFinStr != null ? LocalDate.parse(fechaFinStr) : null;
            jdbcTemplate.update("INSERT INTO Curso (Materia, FechaInicio, FechaFin) VALUES (?, ?, ?)",
                materia, fechaInicio, fechaFin);
            // Asignar profesor al mismo tiempo (insert en Asiste como referencia)
            Object ciProfObj = body.get("ciProfesor");
            if (ciProfObj != null && !String.valueOf(ciProfObj).isBlank()) {
                int ciProfesor = Integer.parseInt(String.valueOf(ciProfObj));
                Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Profesor p JOIN Miembro m ON p.CI = m.CI WHERE p.CI = ? AND m.EstadoCuenta = 'activa'",
                    Integer.class, ciProfesor);
                if (existe != null && existe > 0) {
                    jdbcTemplate.update("INSERT INTO Asiste (CIEstudiante, CIProfesor, Materia, FechaInicio, Nota) VALUES (?, ?, ?, ?, 0)",
                        ciProfesor, ciProfesor, materia, fechaInicio);
                }
            }
            return ResponseEntity.ok(Map.of("message", "Curso creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/cursos")
    public ResponseEntity<Map<String, Object>> eliminarCurso(@RequestBody Map<String, String> body) {
        try {
            int updated = jdbcTemplate.update("DELETE FROM Curso WHERE Materia = ? AND FechaInicio = ?",
                body.get("materia"), LocalDate.parse(body.get("fechaInicio")));
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Curso no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Curso eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cursos/inscribir")
    public ResponseEntity<Map<String, Object>> inscribirEstudiante(@RequestBody Map<String, Object> body) {
        try {
            int ciProfesor = Integer.parseInt(String.valueOf(body.get("ciProfesor")));
            // Validar que el profesor existe y está activo
            Integer existe = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Profesor p JOIN Miembro m ON p.CI = m.CI WHERE p.CI = ? AND m.EstadoCuenta = 'activa'",
                Integer.class, ciProfesor);
            if (existe == null || existe == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El profesor no existe o no está activo"));
            }
            int ciEstudiante = Integer.parseInt(String.valueOf(body.get("ciEstudiante")));
            String materia = String.valueOf(body.get("materia"));
            LocalDate fechaInicio = LocalDate.parse(String.valueOf(body.get("fechaInicio")));
            jdbcTemplate.update("INSERT INTO Asiste (CIEstudiante, CIProfesor, Materia, FechaInicio, Nota) VALUES (?, ?, ?, ?, 0)",
                ciEstudiante, ciProfesor, materia, fechaInicio);
            return ResponseEntity.ok(Map.of("message", "Estudiante inscrito correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Listar profesores y estudiantes para asignación ─────────────
    @GetMapping("/profesores")
    public ResponseEntity<List<Map<String, Object>>> listarProfesores() {
        String sql = "SELECT p.CI, m.PrimerNombre, m.PrimerApellido, m.CorreoInstitucional, p.FechaInicio as RolFechaInicio " +
                     "FROM Profesor p JOIN Miembro m ON p.CI = m.CI ORDER BY m.PrimerApellido";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/estudiantes")
    public ResponseEntity<List<Map<String, Object>>> listarEstudiantes() {
        String sql = "SELECT e.CI, m.PrimerNombre, m.PrimerApellido, m.CorreoInstitucional, e.FechaInicio as RolFechaInicio " +
                     "FROM Estudiante e JOIN Miembro m ON e.CI = m.CI ORDER BY m.PrimerApellido";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/estudiantes-por-fecha")
    public ResponseEntity<List<Map<String, Object>>> listarEstudiantesPorFecha(@RequestParam String fechaInicio) {
        String sql = "SELECT e.CI, m.PrimerNombre, m.PrimerApellido " +
                     "FROM Estudiante e JOIN Miembro m ON e.CI = m.CI " +
                     "WHERE e.FechaInicio = ? ORDER BY m.PrimerApellido";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, LocalDate.parse(fechaInicio)));
    }

    // ─── HU-08: CRUD Servicios ──────────────────────────────────────────
    @GetMapping("/servicios")
    public ResponseEntity<List<Map<String, Object>>> listarServicios() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT s.*, c.Nombre as CategoriaNombre, c.CostoMaximo FROM Servicio s JOIN Categoria c ON s.NombreCategoria = c.Nombre ORDER BY s.NombreCategoria"));
    }

    @PostMapping("/servicios")
    public ResponseEntity<Map<String, Object>> crearServicio(@RequestBody Map<String, Object> body) {
        try {
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String descripcion = String.valueOf(body.get("descripcion"));
            double precioBase = Double.parseDouble(String.valueOf(body.get("precioBase")));
            double ajuste = Double.parseDouble(String.valueOf(body.get("ajuste")));
            String ubicacion = String.valueOf(body.get("ubicacion"));
            jdbcTemplate.update("INSERT INTO Servicio (NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion) VALUES (?, ?, ?, ?, ?, ?)",
                nombreCategoria, idPrestadora, descripcion, precioBase, ajuste, ubicacion);
            return ResponseEntity.ok(Map.of("message", "Servicio creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/servicios")
    public ResponseEntity<Map<String, Object>> eliminarServicio(@RequestBody Map<String, Object> body) {
        try {
            int updated = jdbcTemplate.update("DELETE FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
                String.valueOf(body.get("nombreCategoria")), Integer.parseInt(String.valueOf(body.get("idPrestadora"))), String.valueOf(body.get("descripcion")));
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Servicio no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Servicio eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HU-08: Tarifas ─────────────────────────────────────────────────
    @GetMapping("/tarifas")
    public ResponseEntity<List<Map<String, Object>>> listarTarifas() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT * FROM Tarifa ORDER BY FechaInicio DESC"));
    }

    @PostMapping("/tarifas")
    public ResponseEntity<Map<String, Object>> crearTarifa(@RequestBody Map<String, String> body) {
        try {
            jdbcTemplate.update("INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) VALUES (?, ?, ?, ?, ?, ?)",
                body.get("nombreCategoria"), Integer.parseInt(body.get("idPrestadora")), body.get("descripcion"),
                LocalDate.parse(body.get("fechaInicio")), body.get("perfil"), Double.parseDouble(body.get("costoFinal")));
            return ResponseEntity.ok(Map.of("message", "Tarifa creada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HU-10: Trámites ─────────────────────────────────────────────────
    @GetMapping("/tramites")
    public ResponseEntity<List<Map<String, Object>>> listarTramites() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT t.*, m.PrimerNombre, m.PrimerApellido FROM Tramite t JOIN Miembro m ON t.CI = m.CI ORDER BY t.FechaCreacion DESC"));
    }

    // ─── HU-11: Pasos de actividad ──────────────────────────────────────
    private Map<String, Object> buscarTramiteExacto(int ci, int idPrestadora, String nombreCategoria, String descripcion, Object fc) {
        String sql = "SELECT * FROM Tramite WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? " +
            "AND CAST(? AS TIMESTAMP) BETWEEN FechaCreacion - INTERVAL '1 millisecond' AND FechaCreacion + INTERVAL '1 millisecond' " +
            "ORDER BY FechaCreacion DESC LIMIT 1";
        List<Map<String, Object>> tramites = jdbcTemplate.queryForList(sql, ci, idPrestadora, nombreCategoria, descripcion, fc);
        return tramites.isEmpty() ? null : tramites.get(0);
    }

    @GetMapping("/pasos")
    public ResponseEntity<List<Map<String, Object>>> listarPasos(@RequestParam int ci, @RequestParam int idPrestadora,
                                                                  @RequestParam String nombreCategoria, @RequestParam String descripcion,
                                                                  @RequestParam String fechaCreacion) {
        Map<String, Object> tramite = buscarTramiteExacto(ci, idPrestadora, nombreCategoria, descripcion, parseFechaCreacionObj(fechaCreacion));
        if (tramite == null) return ResponseEntity.ok(List.of());
        Object exactFc = tramite.get("fechacreacion");
        String sql = "SELECT * FROM PasoActividad WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ? ORDER BY OrdenSecuencial";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, ci, idPrestadora, nombreCategoria, descripcion, exactFc));
    }

    @PostMapping("/pasos")
    public ResponseEntity<Map<String, Object>> crearPaso(@RequestBody Map<String, Object> body) {
        try {
            Object adminCiObj = body.get("adminCi");
            if (adminCiObj == null || !esPersonalAdministrativo(Integer.parseInt(adminCiObj.toString())))
                return ResponseEntity.status(403).body(Map.of("error", "Solo personal administrativo puede crear pasos"));

            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            Object fc = parseFechaCreacionObj(body.get("fechaCreacion"));

            Map<String, Object> tramite = buscarTramiteExacto(ci, idPrestadora, nombreCategoria, descripcion, fc);
            if (tramite == null) return ResponseEntity.badRequest().body(Map.of("error", "Trámite no encontrado"));
            Object exactFc = tramite.get("fechacreacion");

            String descripcionInteraccion = String.valueOf(body.get("descripcionInteraccion"));

            Integer maxOrden = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(OrdenSecuencial), 0) + 1 FROM PasoActividad WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                Integer.class, ci, idPrestadora, nombreCategoria, descripcion, exactFc);

            jdbcTemplate.update(
                "INSERT INTO PasoActividad (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial, DescripcionInteraccion, ResponsableAsignado, FechaInicio, DuracionEstimada) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, 0)",
                ci, idPrestadora, nombreCategoria, descripcion, exactFc, maxOrden, descripcionInteraccion, "");

            return ResponseEntity.ok(Map.of("message", "Paso creado correctamente", "orden", maxOrden));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/pasos/{orden}")
    public ResponseEntity<Map<String, Object>> actualizarPaso(@PathVariable int orden, @RequestBody Map<String, Object> body) {
        try {
            Object adminCiObj = body.get("adminCi");
            if (adminCiObj == null || !esPersonalAdministrativo(Integer.parseInt(adminCiObj.toString())))
                return ResponseEntity.status(403).body(Map.of("error", "Solo personal administrativo puede actualizar pasos"));

            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            Object fc = parseFechaCreacionObj(body.get("fechaCreacion"));

            Map<String, Object> tramite = buscarTramiteExacto(ci, idPrestadora, nombreCategoria, descripcion, fc);
            if (tramite == null) return ResponseEntity.status(404).body(Map.of("error", "Paso no encontrado"));
            Object exactFc = tramite.get("fechacreacion");

            int updated = jdbcTemplate.update(
                "UPDATE PasoActividad SET Estado = ?, FechaFin = CASE WHEN ? = 'completado' THEN CURRENT_DATE ELSE FechaFin END, ResponsableAsignado = ? WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ? AND OrdenSecuencial = ?",
                String.valueOf(body.get("estado")), String.valueOf(body.get("estado")), String.valueOf(body.get("responsable")),
                ci, idPrestadora, nombreCategoria, descripcion, exactFc, orden);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Paso no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Paso actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private Object parseFechaCreacionObj(Object raw) {
        if (raw == null) return java.time.LocalDate.now();
        String s = raw.toString().strip();
        try {
            return java.sql.Timestamp.valueOf(s);
        } catch (IllegalArgumentException e1) {
            try {
                // ISO-8601 without timezone: 2026-07-16T20:20:09.009
                return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(s));
            } catch (Exception e2) {
                try {
                    // ISO-8601 with timezone: 2026-07-16T20:20:09.009+00:00
                    return java.sql.Timestamp.from(java.time.Instant.parse(s));
                } catch (Exception e3) {
                    return java.sql.Date.valueOf(s);
                }
            }
        }
    }

    private boolean esPersonalAdministrativo(int ci) {
        try {
            String sql = "SELECT CASE WHEN pa.CI IS NOT NULL THEN 1 ELSE 0 END FROM Miembro m " +
                         "LEFT JOIN Rol r ON m.CI = r.CI AND r.FechaFin IS NULL " +
                         "LEFT JOIN Empleado e ON e.CI = r.CI AND e.FechaInicio = r.FechaInicio " +
                         "LEFT JOIN PersonalAdministrativo pa ON pa.CI = e.CI AND pa.FechaInicio = e.FechaInicio " +
                         "WHERE m.CI = ?";
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class, ci);
            return result != null && result == 1;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── HU-12: Facturas ─────────────────────────────────────────────────
    @GetMapping("/facturas")
    public ResponseEntity<List<Map<String, Object>>> listarFacturas() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT f.*, m.PrimerNombre, m.PrimerApellido FROM Factura f LEFT JOIN Miembro m ON f.CI = m.CI ORDER BY f.FechaHoraEmision DESC"));
    }

    @PostMapping("/facturas/{numero}/pago")
    public ResponseEntity<Map<String, Object>> registrarPagoPresencial(@PathVariable int numero, @RequestBody Map<String, String> body) {
        try {
            String adminCiStr = body.get("adminCi");
            if (adminCiStr == null || !esPersonalAdministrativo(Integer.parseInt(adminCiStr)))
                return ResponseEntity.status(403).body(Map.of("error", "Solo personal administrativo puede registrar pagos"));

            String metodo = body.get("metodo");
            if (metodo == null) return ResponseEntity.badRequest().body(Map.of("error", "Debe especificar 'metodo'"));

            int idMetodo = (int) (System.currentTimeMillis() % 1000000);
            int idPresencial = (int) ((System.currentTimeMillis() + 1) % 1000000);
            double monto = Double.parseDouble(body.getOrDefault("monto", "0"));

            String sql;
            switch (metodo) {
                case "tarjeta": {
                    long nro = Long.parseLong(body.get("nroTarjeta"));
                    String cia = body.get("companiaEmisora");
                    String moneda = body.get("monedaLiquidacion");
                    String red = body.get("tipoRed");
                    String fecVen = body.get("fechaVencimiento");
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Tarjeta (NroTarjeta, CompañiaEmisora, MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial) "
                        + "SELECT ?, ?, ?, ?, CAST(? AS DATE), IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial, nro, cia, moneda, red, fecVen);
                    break;
                }
                case "movil": {
                    long nroRef = Long.parseLong(body.get("nroReferencia"));
                    String tel = body.get("telefonoEmisor");
                    String banco = body.get("banco");
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO PagoMovil (NroReferencia, TelefonoEmisor, Banco, IDPresencial) "
                        + "SELECT ?, ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial, nroRef, tel, banco);
                    break;
                }
                case "efectivo": {
                    String monedaCurso = body.get("monedaDeCurso");
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Efectivo (MonedaDeCurso, IDPresencial) SELECT ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial, monedaCurso);
                    break;
                }
                case "tai": {
                    long pos = Long.parseLong(body.get("pos"));
                    long uid = Long.parseLong(body.get("uid"));
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO TAI (POS, UID, IDPresencial) SELECT ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial, pos, uid);
                    break;
                }
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Método de pago presencial no soportado: " + metodo));
            }

            // Vincular pago con factura (usa la vista con trigger para auto-asignar IDComprador)
            jdbcTemplate.update("INSERT INTO PagaFactura(IDMetodo, NumeroFactura) VALUES (?, ?)", idMetodo, numero);

            // Cerrar el trámite asociado a la factura
            Map<String, Object> factura = jdbcTemplate.queryForMap(
                "SELECT CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion FROM Factura WHERE Numero = ?", numero);
            jdbcTemplate.update(
                "UPDATE Tramite SET Estado = 'finalizado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                factura.get("ci"), factura.get("idprestadora"), factura.get("nombrecategoria"),
                factura.get("descripcion"), factura.get("fechacreacion"));

            return ResponseEntity.ok(Map.of("message", "Pago presencial registrado correctamente. Trámite finalizado."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al registrar pago: " + e.getMessage()));
        }
    }

    @PostMapping("/tramites/{ci}/{fechaCreacion}/pago")
    public ResponseEntity<Map<String, Object>> pagarTramite(@PathVariable int ci, @PathVariable String fechaCreacion, @RequestBody Map<String, String> body) {
        try {
            Object adminCiObj = body.get("adminCi");
            if (adminCiObj == null || !esPersonalAdministrativo(Integer.parseInt(adminCiObj.toString())))
                return ResponseEntity.status(403).body(Map.of("error", "Solo personal administrativo puede registrar pagos"));

            Object fc = parseFechaCreacionObj(fechaCreacion);
            String metodo = body.get("metodo");

            List<Map<String, Object>> tramites = jdbcTemplate.queryForList(
                "SELECT * FROM Tramite WHERE CI = ? " +
                "AND CAST(? AS TIMESTAMP) BETWEEN FechaCreacion - INTERVAL '1 millisecond' AND FechaCreacion + INTERVAL '1 millisecond' " +
                "ORDER BY FechaCreacion DESC LIMIT 1", ci, fc);
            if (tramites.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Trámite no encontrado"));
            Map<String, Object> tramite = tramites.get(0);
            fc = tramite.get("fechacreacion");
            String nombreCategoria = String.valueOf(tramite.get("nombrecategoria"));
            String descripcion = String.valueOf(tramite.get("descripcion"));
            int idPrestadora = Integer.parseInt(String.valueOf(tramite.get("idprestadora")));

            // Asegurar IDComprador del miembro
            Integer idComprador = jdbcTemplate.queryForObject(
                "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
            if (idComprador == null) {
                int newId = (int) (System.currentTimeMillis() % 1000000);
                jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
                jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
                idComprador = newId;
            }

            // Cerrar EstadoCuenta
            jdbcTemplate.update(
                "UPDATE EstadoCuenta SET EstadoFiscal = 'cerrado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                ci, idPrestadora, nombreCategoria, descripcion, fc);

            // Crear Tarifa dinámica si no existe para este servicio + perfil
            String tarifaCheck = "SELECT COUNT(*) FROM Tarifa WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ? AND Perfil = 'miembro activo' AND FechaFin IS NULL";
            Integer count = jdbcTemplate.queryForObject(tarifaCheck, Integer.class, nombreCategoria, idPrestadora, descripcion);
            if (count == 0) {
                Double precioBase = jdbcTemplate.queryForObject(
                    "SELECT PrecioBase FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
                    Double.class, nombreCategoria, idPrestadora, descripcion);
                jdbcTemplate.update(
                    "INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) VALUES (?, ?, ?, ?, 'miembro activo', ?)",
                    nombreCategoria, idPrestadora, descripcion, fc, precioBase);
            }

            // Crear LineaCargo (triggers auto-asignan PrecioUnitario y validan)
            jdbcTemplate.update(
                "INSERT INTO LineaCargo (CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea, FechaCargo, Cantidad, Concepto, ImpuestosLey) VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, 1, ?, 0)",
                ci, idPrestadora, nombreCategoria, descripcion, fc, fc, descripcion);

            // Crear Factura (trigger calcula Deuda desde LineaCargo)
            jdbcTemplate.update(
                "INSERT INTO Factura (IDComprador, CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) VALUES (?, ?, ?, ?, ?, ?, ?)",
                idComprador, ci, idPrestadora, nombreCategoria, descripcion, fc, fc);

            // Obtener NumeroFactura recién creada
            Integer numeroFactura = jdbcTemplate.queryForObject(
                "SELECT MAX(Numero) FROM Factura WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                Integer.class, ci, idPrestadora, nombreCategoria, descripcion, fc);

            // Crear MetodoPago + PagoPresencial + subtipo
            double monto = Double.parseDouble(body.getOrDefault("monto", "0"));
            int idMetodo = (int) (System.currentTimeMillis() % 1000000);
            int idPresencial = (int) ((System.currentTimeMillis() + 1) % 1000000);

            String sql;
            switch (metodo) {
                case "tarjeta":
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Tarjeta (NroTarjeta, CompañiaEmisora, MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial) "
                        + "SELECT ?, ?, ?, ?, CAST(? AS DATE), IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial,
                        Long.parseLong(body.get("nroTarjeta")), body.get("companiaEmisora"),
                        body.get("monedaLiquidacion"), body.get("tipoRed"), body.get("fechaVencimiento"));
                    break;
                case "movil":
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO PagoMovil (NroReferencia, TelefonoEmisor, Banco, IDPresencial) "
                        + "SELECT ?, ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial,
                        Long.parseLong(body.get("nroReferencia")), body.get("telefonoEmisor"), body.get("banco"));
                    break;
                case "efectivo":
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Efectivo (MonedaDeCurso, IDPresencial) SELECT ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial, body.get("monedaDeCurso"));
                    break;
                case "tai":
                    sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO TAI (POS, UID, IDPresencial) SELECT ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, (int) monto, idPresencial,
                        Long.parseLong(body.get("pos")), Long.parseLong(body.get("uid")));
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Método no soportado: " + metodo));
            }

            // Vincular pago con factura
            jdbcTemplate.update("INSERT INTO PagaFactura(IDMetodo, NumeroFactura) VALUES (?, ?)", idMetodo, numeroFactura);

            // Cerrar trámite
            jdbcTemplate.update(
                "UPDATE Tramite SET Estado = 'finalizado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                ci, idPrestadora, nombreCategoria, descripcion, fc);

            return ResponseEntity.ok(Map.of("message", "Pago registrado y trámite finalizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al registrar pago: " + e.getMessage()));
        }
    }

    // ─── HU-15: TAI ──────────────────────────────────────────────────────
    @GetMapping("/tai")
    public ResponseEntity<List<Map<String, Object>>> listarTAI() {
        String sql = "SELECT t.POS, t.FechaHoraPago, t.UID, t.IDPresencial, t.Estado, " +
                     "m.PrimerNombre, m.PrimerApellido " +
                     "FROM TAI t LEFT JOIN Miembro m ON t.UID = m.CI ORDER BY t.POS";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PutMapping("/tai/bloquear")
    public ResponseEntity<Map<String, Object>> bloquearTAI(@RequestBody Map<String, Object> body) {
        try {
            long pos = Long.parseLong(String.valueOf(body.get("pos")));
            String estado = String.valueOf(body.get("estado"));
            if (!estado.equals("activo") && !estado.equals("bloqueado")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido. Use: activo o bloqueado"));
            }
            int updated = jdbcTemplate.update("UPDATE TAI SET Estado = ? WHERE POS = ?", estado, pos);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "TAI no encontrado"));
            return ResponseEntity.ok(Map.of("message", "TAI " + (estado.equals("bloqueado") ? "bloqueado" : "desbloqueado") + " correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── HU-18: Medios de Transporte ────────────────────────────────────
    @GetMapping("/transportes")
    public ResponseEntity<List<Map<String, Object>>> listarTransportes() {
        String sql = "SELECT Placa, Ubicacion, encode(CarnetDeConducir, 'base64') AS CarnetDeConducir, Disponibilidad, TipoVehiculo, Capacidad FROM MedioTransporte ORDER BY Placa";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/transportes")
    public ResponseEntity<Map<String, Object>> crearTransporte(@RequestBody Map<String, Object> body) {
        try {
            String carnetB64 = String.valueOf(body.get("carnetDeConducir"));
            byte[] carnetBytes = Base64.getDecoder().decode(carnetB64);
            jdbcTemplate.update("INSERT INTO MedioTransporte (Placa, Ubicacion, CarnetDeConducir, Disponibilidad, TipoVehiculo, Capacidad) VALUES (?, ?, ?, ?, ?, ?)",
                String.valueOf(body.get("placa")), String.valueOf(body.get("ubicacion")), carnetBytes,
                Boolean.parseBoolean(String.valueOf(body.get("disponibilidad"))), String.valueOf(body.get("tipoVehiculo")),
                Integer.parseInt(String.valueOf(body.get("capacidad"))));
            return ResponseEntity.ok(Map.of("message", "Medio de transporte creado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/transportes")
    public ResponseEntity<Map<String, Object>> actualizarTransporte(@RequestBody Map<String, Object> body) {
        try {
            byte[] carnetBytes = Base64.getDecoder().decode(String.valueOf(body.get("carnetDeConducir")));
            String placa = String.valueOf(body.get("placa"));
            String ubicacion = String.valueOf(body.get("ubicacion"));
            boolean disponibilidad = Boolean.parseBoolean(String.valueOf(body.get("disponibilidad")));
            String tipoVehiculo = String.valueOf(body.get("tipoVehiculo"));
            int capacidad = Integer.parseInt(String.valueOf(body.get("capacidad")));
            int updated = jdbcTemplate.update(
                "UPDATE MedioTransporte SET Ubicacion = ?, Disponibilidad = ?, TipoVehiculo = ?, Capacidad = ? WHERE Placa = ? AND CarnetDeConducir = ?",
                ubicacion, disponibilidad, tipoVehiculo, capacidad, placa, carnetBytes);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Transporte no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Transporte actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/transportes")
    public ResponseEntity<Map<String, Object>> eliminarTransporte(@RequestBody Map<String, Object> body) {
        try {
            String placa = String.valueOf(body.get("placa"));
            byte[] carnetBytes = Base64.getDecoder().decode(String.valueOf(body.get("carnetDeConducir")));
            int updated = jdbcTemplate.update("DELETE FROM MedioTransporte WHERE Placa = ? AND CarnetDeConducir = ?", placa, carnetBytes);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Transporte no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Medio de transporte eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se puede eliminar: " + e.getMessage()));
        }
    }

    // ─── Pago presencial (Personal Administrativo paga por otros) ─────
    @PostMapping("/pagos/presencial")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> pagoPresencial(@RequestBody Map<String, Object> body) {
        try {
            Object adminCiObj = body.get("adminCi");
            if (adminCiObj == null || !esPersonalAdministrativo(Integer.parseInt(adminCiObj.toString())))
                return ResponseEntity.status(403).body(Map.of("error", "Solo personal administrativo puede procesar pagos presenciales"));

            int ci = Integer.parseInt(String.valueOf(body.get("ci")));
            int idPrestadora = Integer.parseInt(String.valueOf(body.get("idPrestadora")));
            String nombreCategoria = String.valueOf(body.get("nombreCategoria"));
            String descripcion = String.valueOf(body.get("descripcion"));
            Object fc = parseFechaCreacionObj(body.get("fechaCreacion"));

            Map<String, Object> tramite = buscarTramiteExacto(ci, idPrestadora, nombreCategoria, descripcion, fc);
            if (tramite == null) return ResponseEntity.status(404).body(Map.of("error", "Trámite no encontrado"));
            fc = tramite.get("fechacreacion");

            // Obtener PrecioBase del servicio
            Double monto;
            try {
                monto = jdbcTemplate.queryForObject(
                    "SELECT PrecioBase FROM Servicio WHERE NombreCategoria = ? AND IDPrestadora = ? AND Descripcion = ?",
                    Double.class, nombreCategoria, idPrestadora, descripcion);
            } catch (Exception e) {
                monto = 0.0;
            }

            // Asegurar IDComprador del miembro
            Integer idComprador = jdbcTemplate.queryForObject(
                "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
            if (idComprador == null) {
                int newId = (int) (System.currentTimeMillis() % 1000000);
                jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
                jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
                idComprador = newId;
            }

            // Crear MetodoPago
            String metodoStr = String.valueOf(body.getOrDefault("metodo", "efectivo"));
            int idMetodo = (int) (System.currentTimeMillis() % 1000000);
            jdbcTemplate.update("INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?)", idMetodo, monto);

            // Crear PagoPresencial
            int idPresencial = (int) ((System.currentTimeMillis() + 1) % 1000000);
            jdbcTemplate.update("INSERT INTO PagoPresencial (IDPresencial, IDMetodo) VALUES (?, ?)", idPresencial, idMetodo);

            // Crear el método de pago específico
            switch (metodoStr) {
                case "tarjeta" -> {
                    long nroTarjeta = Long.parseLong(String.valueOf(body.get("nroTarjeta")));
                    String compania = String.valueOf(body.get("companiaEmisora"));
                    String moneda = String.valueOf(body.get("monedaLiquidacion"));
                    String tipoRed = String.valueOf(body.get("tipoRed"));
                    String fechaVenc = String.valueOf(body.get("fechaVencimiento"));
                    jdbcTemplate.update(
                        "INSERT INTO Tarjeta (NroTarjeta, \"compa\u00f1iaemisora\", MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial) VALUES (?, ?, ?, ?, CAST(? AS DATE), ?)",
                        nroTarjeta, compania, moneda, tipoRed, fechaVenc, idPresencial);
                }
                case "pagomovil" -> {
                    long nroRef = Long.parseLong(String.valueOf(body.get("nroReferencia")));
                    String telefono = String.valueOf(body.get("telefonoEmisor"));
                    String banco = String.valueOf(body.get("banco"));
                    jdbcTemplate.update(
                        "INSERT INTO PagoMovil (NroReferencia, TelefonoEmisor, Banco, IDPresencial) VALUES (?, ?, ?, ?)",
                        nroRef, telefono, banco, idPresencial);
                }
                case "tai" -> {
                    long pos = Long.parseLong(String.valueOf(body.get("pos")));
                    long uid = Long.parseLong(String.valueOf(body.get("uid")));
                    jdbcTemplate.update(
                        "INSERT INTO TAI (POS, UID, IDPresencial) VALUES (?, ?, ?)",
                        pos, uid, idPresencial);
                }
                default -> { // efectivo
                    String monedaCurso = String.valueOf(body.getOrDefault("monedaDeCurso", "bolivares"));
                    var fcEfectivo = jdbcTemplate.queryForMap(
                        "INSERT INTO Efectivo (MonedaDeCurso, IDPresencial) VALUES (?, ?) RETURNING FechaHoraPago",
                        monedaCurso, idPresencial);
                    Object cantidadObj = body.get("cantidad");
                    Object valorObj = body.get("valor");
                    if (cantidadObj != null && valorObj != null) {
                        int cantidad = Integer.parseInt(cantidadObj.toString());
                        double valor = Double.parseDouble(valorObj.toString());
                        jdbcTemplate.update(
                            "INSERT INTO DesgloseDenominaciones (FechaHoraPago, Cantidad, Valor) VALUES (CAST(? AS TIMESTAMP), ?, ?)",
                            fcEfectivo.get("fechahorapago"), cantidad, valor);
                    }
                }
            }

            // Obtener MesAñoApertura del EstadoCuenta (debe coincidir con el que creó el trigger)
            String mesAñoAperturaStr;
            try {
                mesAñoAperturaStr = jdbcTemplate.queryForObject(
                    "SELECT MesAñoApertura::text FROM EstadoCuenta WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                    String.class, ci, idPrestadora, nombreCategoria, descripcion, fc);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "EstadoCuenta no encontrado para el trámite"));
            }

            // Cerrar EstadoCuenta
            jdbcTemplate.update(
                "UPDATE EstadoCuenta SET EstadoFiscal = 'cerrado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                ci, idPrestadora, nombreCategoria, descripcion, fc);

            // Crear Factura (usa MesAñoApertura exacto del EstadoCuenta para FK)
            int numeroFactura = jdbcTemplate.queryForObject(
                "INSERT INTO Factura (IDComprador, CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS DATE)) RETURNING Numero",
                Integer.class, idComprador, ci, idPrestadora, nombreCategoria, descripcion, fc, mesAñoAperturaStr);

            // Vincular pago con factura
            jdbcTemplate.update("INSERT INTO PagaFactura (IDMetodo, NumeroFactura) VALUES (?, ?)", idMetodo, numeroFactura);

            // Finalizar trámite
            jdbcTemplate.update(
                "UPDATE Tramite SET Estado = 'finalizado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                ci, idPrestadora, nombreCategoria, descripcion, fc);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Pago presencial registrado correctamente. Factura #" + numeroFactura + " generada.",
                "nroFactura", numeroFactura, "monto", monto));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al procesar pago presencial: " + e.getMessage()));
        }
    }

    // ─── HU-18: Viajes ──────────────────────────────────────────────────
    @GetMapping("/viajes")
    public ResponseEntity<List<Map<String, Object>>> listarViajes() {
        String sql = "SELECT FechaHoraInicio, FechaHoraFin, Placa, encode(CarnetDeConducir, 'base64') AS CarnetDeConducir, Destino FROM Viaje ORDER BY FechaHoraInicio DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/viajes")
    public ResponseEntity<Map<String, Object>> crearViaje(@RequestBody Map<String, Object> body) {
        try {
            byte[] carnetBytes = Base64.getDecoder().decode(String.valueOf(body.get("carnetDeConducir")));
            jdbcTemplate.update("INSERT INTO Viaje (Placa, CarnetDeConducir, Destino) VALUES (?, ?, ?)",
                String.valueOf(body.get("placa")), carnetBytes, String.valueOf(body.get("destino")));
            return ResponseEntity.ok(Map.of("message", "Viaje registrado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/viajes")
    public ResponseEntity<Map<String, Object>> eliminarViaje(@RequestBody Map<String, Object> body) {
        try {
            byte[] carnetBytes = Base64.getDecoder().decode(String.valueOf(body.get("carnetDeConducir")));
            int updated = jdbcTemplate.update("DELETE FROM Viaje WHERE FechaHoraInicio = ? AND Placa = ? AND CarnetDeConducir = ?",
                LocalDateTime.parse(String.valueOf(body.get("fechaHoraInicio"))), String.valueOf(body.get("placa")), carnetBytes);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Viaje no encontrado"));
            return ResponseEntity.ok(Map.of("message", "Viaje eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Categorías (helper para servicios) ─────────────────────────────
    @GetMapping("/categorias")
    public ResponseEntity<List<Map<String, Object>>> listarCategorias() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT * FROM Categoria ORDER BY Nombre"));
    }

    // ─── Entidades Prestadoras (helper para servicios) ──────────────────
    @GetMapping("/prestadoras")
    public ResponseEntity<List<Map<String, Object>>> listarPrestadoras() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("SELECT * FROM EntidadPrestadora ORDER BY IDPrestadora"));
    }

    // ─── HU: Agregar Organización Externa ──────────────────────────────
    @PostMapping("/organizaciones")
    public ResponseEntity<Map<String, Object>> crearOrganizacion(@RequestBody Map<String, Object> body) {
        try {
            String rif = String.valueOf(body.get("rif"));
            if (rif == null || rif.isBlank() || rif.equals("null")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'rif' es requerido"));
            }
            String razonSocial = String.valueOf(body.get("razonSocial"));
            String fechaVencimientoStr = String.valueOf(body.get("fechaVencimientoContrato"));
            if (razonSocial == null || razonSocial.isBlank() || razonSocial.equals("null") ||
                fechaVencimientoStr == null || fechaVencimientoStr.isBlank() || fechaVencimientoStr.equals("null")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'razonSocial' y 'fechaVencimientoContrato' son requeridos"));
            }

            int idPrestadora = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(IDPrestadora), 0) + 1 FROM EntidadPrestadora", Integer.class);

            // Obtener datos del servicio asociado
            String servicioCat = String.valueOf(body.getOrDefault("servicioCategoria", "General"));
            String servicioDesc = String.valueOf(body.getOrDefault("servicioDescripcion", "Servicio de " + razonSocial));
            double servicioAjuste = Double.parseDouble(String.valueOf(body.getOrDefault("servicioAjuste", "0")));
            String servicioUbi = String.valueOf(body.getOrDefault("servicioUbicacion", "Nacional"));
            // PrecioBase debe ser único; generar uno que no exista
            double servicioPrecio;
            Object precioObj = body.get("servicioPrecioBase");
            if (precioObj != null) {
                servicioPrecio = Double.parseDouble(precioObj.toString());
            } else {
                servicioPrecio = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(PrecioBase) + 0.01, 1) FROM Servicio", Double.class);
            }
            // Buscar próximo valor único si el precio ya existe
            while (true) {
                Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Servicio WHERE PrecioBase = ?", Integer.class, servicioPrecio);
                if (cnt == null || cnt == 0) break;
                servicioPrecio += 0.01;
            }

            jdbcTemplate.update(
                "WITH "
                + "ep AS (INSERT INTO EntidadPrestadora (IDPrestadora) VALUES (?) RETURNING IDPrestadora), "
                + "org AS (INSERT INTO OrganizacionExterna (RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora) "
                + "         SELECT ?, CAST(? AS DATE), ?, IDPrestadora FROM ep), "
                + "sv AS (INSERT INTO Servicio (NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion) "
                + "       SELECT ?, IDPrestadora, ?, ?, ?, ? FROM ep RETURNING NombreCategoria, IDPrestadora, Descripcion) "
                + "INSERT INTO Tarifa (NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal) "
                + "SELECT NombreCategoria, IDPrestadora, Descripcion, CURRENT_DATE, 'miembro activo', ? FROM sv",
                idPrestadora, rif, fechaVencimientoStr, razonSocial,
                servicioCat, servicioDesc, servicioPrecio, servicioAjuste, servicioUbi, servicioPrecio);

            // Contactos legales opcionales
            Object contactosObj = body.get("contactosLegales");
            if (contactosObj instanceof List<?>) {
                List<?> rawContactos = (List<?>) contactosObj;
                for (Object raw : rawContactos) {
                    if (raw instanceof Map) {
                        Map<?, ?> c = (Map<?, ?>) raw;
                        jdbcTemplate.update(
                            "INSERT INTO ContactosLegales (RIF, Telefono, Nombre) VALUES (?, ?, ?)",
                            rif, String.valueOf(c.get("telefono")), String.valueOf(c.get("nombre")));
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "Organización externa creada correctamente",
                "rif", rif, "idPrestadora", idPrestadora));
        } catch (Exception e) {
            log.error("Error al crear organización: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error al crear organización: " + e.getMessage()));
        }
    }

    // ─── HU: Publicar Postulación / Oportunidad Laboral ────────────────
    @PostMapping("/postulaciones")
    public ResponseEntity<Map<String, Object>> publicarPostulacion(@RequestBody Map<String, Object> body) {
        try {
            String rif = String.valueOf(body.get("rif"));
            if (rif == null || rif.isBlank() || rif.equals("null")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'rif' es requerido"));
            }

            String perfilBuscado = String.valueOf(body.get("perfilBuscado"));
            String cargo = String.valueOf(body.get("cargo"));
            String beneficios = String.valueOf(body.get("beneficios"));
            String responsabilidades = String.valueOf(body.get("responsabilidades"));

            if (perfilBuscado == null || perfilBuscado.isBlank() || perfilBuscado.equals("null") ||
                cargo == null || cargo.isBlank() || cargo.equals("null") ||
                beneficios == null || beneficios.isBlank() || beneficios.equals("null") ||
                responsabilidades == null || responsabilidades.isBlank() || responsabilidades.equals("null")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'perfilBuscado', 'cargo', 'beneficios' y 'responsabilidades' son requeridos"));
            }

            // Verificar que la organización existe
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM OrganizacionExterna WHERE RIF = ?", Integer.class, rif);
            if (exists == null || exists == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No existe una organización externa con RIF: " + rif));
            }

            jdbcTemplate.update(
                "INSERT INTO OportunidadLaboral (RIF, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante) VALUES (?, ?, ?, ?, ?, 'disponible')",
                rif, perfilBuscado, cargo, beneficios, responsabilidades);

            return ResponseEntity.ok(Map.of("mensaje", "Postulación publicada correctamente", "rif", rif, "cargo", cargo));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al publicar postulación: " + e.getMessage()));
        }
    }

    // ─── Listar Organizaciones Externas ─────────────────────────────────
    @GetMapping("/organizaciones")
    public ResponseEntity<List<Map<String, Object>>> listarOrganizaciones() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT o.*, " +
            "(SELECT json_agg(json_build_object('nombre', cl.nombre, 'telefono', cl.telefono)) FROM ContactosLegales cl WHERE cl.RIF = o.RIF) AS contactos " +
            "FROM OrganizacionExterna o ORDER BY o.RazonSocial"));
    }

    // ─── Listar Postulaciones / Oportunidades Laborales ─────────────────
    @GetMapping("/postulaciones")
    public ResponseEntity<List<Map<String, Object>>> listarPostulaciones() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
            "SELECT o.RIF, o.FechaHoraOferta, o.PerfilBuscado, o.Cargo, o.Beneficios, o.Responsabilidades, o.EstatusVacante, " +
            "org.RazonSocial " +
            "FROM OportunidadLaboral o " +
            "JOIN OrganizacionExterna org ON o.RIF = org.RIF " +
            "ORDER BY o.FechaHoraOferta DESC"));
    }

    // ─── Eliminar Postulación ───────────────────────────────────────────
    @DeleteMapping("/postulaciones/{rif}/{fechaHoraOferta}")
    public ResponseEntity<Map<String, Object>> eliminarPostulacion(
            @PathVariable String rif, @PathVariable String fechaHoraOferta) {
        try {
            int updated = jdbcTemplate.update(
                "DELETE FROM OportunidadLaboral WHERE RIF = ? AND FechaHoraOferta = CAST(? AS TIMESTAMP)",
                rif, fechaHoraOferta);
            if (updated == 0) return ResponseEntity.status(404).body(Map.of("error", "Postulación no encontrada"));
            return ResponseEntity.ok(Map.of("mensaje", "Postulación eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
