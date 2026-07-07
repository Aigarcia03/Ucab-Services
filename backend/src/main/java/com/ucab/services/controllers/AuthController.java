package com.ucab.services.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final JdbcTemplate jdbcTemplate;

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload,
                                                     HttpServletRequest request) {
        String email = payload.get("usuario");
        String password = payload.get("contrasena");

        String sql = "SELECT CI, PrimerNombre, PrimerApellido, CorreoInstitucional, Categoria " +
                     "FROM Miembro WHERE CorreoInstitucional = ? AND contrasena = ?";
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales inválidas"));
        }

        Map<String, Object> authenticatedUser = results.get(0);
        Integer ci = (Integer) authenticatedUser.get("ci");
        if (ci != null) {
            try {
                closeLatestOpenSession(ci);
                createNewSession(ci, resolveClientIp(request));
            } catch (Exception e) {
                logger.error("No se pudo registrar la sesión para CI {}", ci, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo registrar la sesión de usuario. Intenta de nuevo.",
                                 "detail", e.getMessage()));
            }
        }

        return ResponseEntity.ok(authenticatedUser);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> payload) {
        try {
            int ci = Integer.parseInt(String.valueOf(payload.get("CI")));
            String primerNombre = String.valueOf(payload.get("PrimerNombre"));
            String primerApellido = String.valueOf(payload.get("PrimerApellido"));
            String sexo = String.valueOf(payload.get("Sexo"));
            String correo = String.valueOf(payload.get("CorreoInstitucional"));
            String direccion = String.valueOf(payload.get("DireccionHabitacion"));
            String fechaNacimientoStr = String.valueOf(payload.get("FechaNacimiento"));
            String telefono = String.valueOf(payload.get("Telefono"));
            String categoria = String.valueOf(payload.get("Categoria"));
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

            String sql = "INSERT INTO Miembro (CI, PrimerNombre, PrimerApellido, Sexo, CorreoInstitucional, DireccionHabitacion, FechaNacimiento, Telefono, Categoria, UltimaConexion, contrasena) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, ci, primerNombre, primerApellido, sexo, correo, direccion, fechaNacimiento, telefono, categoria, "Nunca", contrasenia);

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

    private void createNewSession(int ci, String clientIp) {
        String uuid = UUID.randomUUID().toString();
        String geo = resolveGeolocalizacion(clientIp);
        logger.info("Creando sesión - CI: {}, IP: {}, Geo: {}, UUID: {}", ci, clientIp, geo, uuid);
        jdbcTemplate.update(
            "INSERT INTO sesion (ci, fechainicio, fechafin, ip, geolocalizacion, uuid, protocoloproteccion, mfa, conteointentosfallidos) VALUES (?, CURRENT_TIMESTAMP, NULL, ?, ?, ?, ?, ?, ?)",
            ci, clientIp, geo, uuid, Boolean.FALSE, "pendiente", 0
        );
    }

    private int closeLatestOpenSession(int ci) {
        String sql = "UPDATE sesion SET fechafin = CURRENT_TIMESTAMP WHERE ci = ? AND fechafin IS NULL " +
                     "AND fechainicio = (SELECT MAX(fechainicio) FROM sesion WHERE ci = ? AND fechafin IS NULL)";
        return jdbcTemplate.update(sql, ci, ci);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "0.0.0.0";

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "0.0.0.0" : remoteAddr;
    }

    private String resolveGeolocalizacion(String clientIp) {
        if (clientIp == null || clientIp.equals("0.0.0.0") || clientIp.equals("127.0.0.1") || clientIp.equals("::1")) {
            return "Entorno local";
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://ip-api.com/json/" + clientIp + "?fields=city,regionName,country"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                String city = extractJsonStr(body, "city");
                String region = extractJsonStr(body, "regionName");
                String country = extractJsonStr(body, "country");
                if (city != null && !city.isEmpty() && country != null && !country.isEmpty()) {
                    String geo = region != null && !region.isEmpty() && !region.equals(city)
                        ? city + ", " + region + ", " + country
                        : city + ", " + country;
                    return geo.length() > 50 ? geo.substring(0, 50) : geo;
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo obtener geolocalización para IP {}: {}", clientIp, e.getMessage());
        }
        return "No disponible";
    }

    private String extractJsonStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }
}
