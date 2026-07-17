package com.ucab.services.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final JdbcTemplate jdbcTemplate;

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("usuario");
        String password = payload.get("contrasena");

        // Verificar bloqueo por intentos fallidos con timeout de 10 minutos
        try {
            List<Integer> ciList = jdbcTemplate.queryForList(
                "SELECT CI FROM Miembro WHERE CorreoInstitucional = ?", Integer.class, email);
            if (!ciList.isEmpty()) {
                int miembroCi = ciList.get(0);
                List<Map<String, Object>> sesion = jdbcTemplate.queryForList(
                    "SELECT ConteoIntentosFallidos, FechaInicio FROM Sesion WHERE CI = ? AND FechaFin IS NULL ORDER BY FechaInicio DESC LIMIT 1",
                    miembroCi);
                if (!sesion.isEmpty()) {
                    int intentos = sesion.get(0).get("conteointentosfallidos") != null
                        ? ((Number) sesion.get(0).get("conteointentosfallidos")).intValue() : 0;
                    if (intentos >= 3) {
                        Object fechaInicioObj = sesion.get(0).get("fechainicio");
                        if (fechaInicioObj != null) {
                            LocalDateTime fechaInicio;
                            if (fechaInicioObj instanceof java.sql.Timestamp) {
                                fechaInicio = ((java.sql.Timestamp) fechaInicioObj).toLocalDateTime();
                            } else if (fechaInicioObj instanceof LocalDateTime) {
                                fechaInicio = (LocalDateTime) fechaInicioObj;
                            } else {
                                fechaInicio = LocalDateTime.parse(fechaInicioObj.toString());
                            }
                            if (fechaInicio.isAfter(LocalDateTime.now().minusMinutes(10))) {
                                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                    .body(Map.of("error", "Demasiados intentos. Intente de nuevo en 10 minutos."));
                            } else {
                                jdbcTemplate.update("UPDATE Sesion SET ConteoIntentosFallidos = 0 WHERE CI = ? AND FechaFin IS NULL", miembroCi);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error al verificar intentos fallidos: {}", e.getMessage());
        }

        String sql = "SELECT CI, PrimerNombre, PrimerApellido, CorreoInstitucional, Categoria " +
                     "FROM Miembro WHERE CorreoInstitucional = ? AND Contraseña = ?";
        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                ps -> { ps.setString(1, email); ps.setString(2, password); },
                (rs, rowNum) -> {
                    Map<String, Object> user = new HashMap<>();
                    user.put("ci", rs.getInt("CI"));
                    user.put("firstName", rs.getString("PrimerNombre"));
                    user.put("lastName", rs.getString("PrimerApellido"));
                    user.put("email", rs.getString("CorreoInstitucional"));
                    user.put("category", rs.getString("Categoria"));
                    return user;
                });

        if (results.isEmpty()) {
            // Registrar intento fallido
            try {
                List<Integer> ciList = jdbcTemplate.queryForList(
                    "SELECT CI FROM Miembro WHERE CorreoInstitucional = ?", Integer.class, email);
                if (!ciList.isEmpty()) {
                    int miembroCi = ciList.get(0);
                    jdbcTemplate.update(
                        "UPDATE Sesion SET ConteoIntentosFallidos = COALESCE(ConteoIntentosFallidos, 0) + 1 WHERE CI = ? AND FechaFin IS NULL",
                        miembroCi);
                }
            } catch (Exception e) {
                logger.warn("No se pudo registrar intento fallido: {}", e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales inválidas"));
        }

        Map<String, Object> authenticatedUser = results.get(0);
        Integer ci = (Integer) authenticatedUser.get("ci");

        // Resetear intentos y crear nueva sesión
        if (ci != null) {
            try {
                jdbcTemplate.update("UPDATE Sesion SET ConteoIntentosFallidos = 0 WHERE CI = ? AND FechaFin IS NULL", ci);
                closeLatestOpenSession(ci);
                crearSesion(ci);
            } catch (Exception e) {
                logger.error("No se pudo crear la sesión para CI {}", ci, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo registrar la sesión de usuario.",
                                 "detail", e.getMessage()));
            }
        }

        // Identificar administrador
        String adminEmail = "admin@ucab.edu.ve";
        Object emailObj = authenticatedUser.get("email");
        boolean isAdmin = emailObj != null && adminEmail.equalsIgnoreCase(emailObj.toString());
        authenticatedUser.put("admin", isAdmin);

        return ResponseEntity.ok(authenticatedUser);
    }

    @GetMapping("/check-ci/{ci}")
    public ResponseEntity<Map<String, Object>> checkCi(@PathVariable Integer ci) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Miembro WHERE CI = ?", Integer.class, ci);
            return ResponseEntity.ok(Map.of("exists", count != null && count > 0));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> payload) {
        try {
            int ci = Integer.parseInt(String.valueOf(payload.get("CI")));

            Integer existente = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Miembro WHERE CI = ?", Integer.class, ci);
            if (existente != null && existente > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "La cédula ya está registrada."));
            }

            String primerNombre = String.valueOf(payload.get("PrimerNombre"));
            String primerApellido = String.valueOf(payload.get("PrimerApellido"));
            String sexo = String.valueOf(payload.get("Sexo"));
            String correo = String.valueOf(payload.get("CorreoInstitucional"));
            if (!correo.toLowerCase().endsWith("@ucab.edu.ve") && !correo.toLowerCase().endsWith("@est.ucab.edu.ve")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El correo debe tener dominio institucional (@ucab.edu.ve o @est.ucab.edu.ve)"));
            }
            String direccion = String.valueOf(payload.get("DireccionHabitacion"));
            String fechaNacimientoStr = String.valueOf(payload.get("FechaNacimiento"));
            String telefono = String.valueOf(payload.get("Telefono"));
            String categoria = String.valueOf(payload.getOrDefault("Categoria", "frecuente"));
            String contrasenia = String.valueOf(payload.get("contrasena"));

            try {
                Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                if (one == null) throw new SQLException("No se pudo validar conexión a la base de datos");
            } catch (Exception ex) {
                throw new SQLException("Error de conexión con la base de datos: " + ex.getMessage(), ex);
            }

            Date fechaNacimiento = null;
            try {
                if (fechaNacimientoStr != null && !fechaNacimientoStr.isBlank()) {
                    fechaNacimiento = Date.valueOf(LocalDate.parse(fechaNacimientoStr));
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Formato inválido para FechaNacimiento. Use YYYY-MM-DD.");
            }

            int uuidReg = (int) (System.currentTimeMillis() % 1000000);
            String sql = "WITH ins_miembro AS (INSERT INTO Miembro (CI, PrimerNombre, PrimerApellido, Sexo, CorreoInstitucional, DireccionHabitacion, FechaNacimiento, Telefono, Categoria, UltimaConexion, Contraseña) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING CI) INSERT INTO Sesion (CI, FechaInicio, Geolocalizacion, UUID, ConteoIntentosFallidos) SELECT CI, CURRENT_TIMESTAMP, ?, ?, 0 FROM ins_miembro";
            jdbcTemplate.update(sql, ci, primerNombre, primerApellido, sexo, correo, direccion, fechaNacimiento, telefono, categoria, "Nunca", contrasenia, "Registro inicial", uuidReg);

            Map<String, Object> user = new HashMap<>();
            user.put("ci", ci);
            user.put("firstName", primerNombre);
            user.put("lastName", primerApellido);
            user.put("email", correo);
            user.put("category", categoria);

            return ResponseEntity.ok(user);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "CI inválida"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No se pudo registrar el miembro: " + e.getMessage()));
        }
    }

    @PostMapping("/session/close-latest")
    public ResponseEntity<Map<String, Object>> closeLatestSession(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            if (ciValue == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debes enviar la CI del miembro."));
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));
            int updatedRows = closeLatestOpenSession(ci);

            return ResponseEntity.ok(Map.of(
                "message", updatedRows > 0
                    ? "Sesión cerrada correctamente."
                    : "No había sesiones abiertas para cerrar.",
                "updatedRows", updatedRows
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "CI inválida."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "No se pudo cerrar la sesión: " + e.getMessage()));
        }
    }

    private void crearSesion(int ci) {
        int uuidSes = (int) (System.currentTimeMillis() % 1000000);
        jdbcTemplate.update(
            "INSERT INTO Sesion (CI, FechaInicio, Geolocalizacion, UUID, ConteoIntentosFallidos) VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?)",
            ci, "Entorno local", uuidSes, 0
        );
    }

    private int closeLatestOpenSession(int ci) {
        String sql = "UPDATE Sesion SET FechaFin = CURRENT_TIMESTAMP WHERE CI = ? AND FechaFin IS NULL " +
                     "AND FechaInicio = (SELECT MAX(FechaInicio) FROM Sesion WHERE CI = ? AND FechaFin IS NULL)";
        return jdbcTemplate.update(sql, ci, ci);
    }

}
