package com.ucab.services.controllers;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class DatabaseExplorerController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseExplorerController.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseExplorerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload,
                                                     HttpServletRequest request) {
        String email = payload.get("usuario");

        String password = payload.get("contrasena");

        String sql = "SELECT CI, PrimerNombre, PrimerApellido, CorreoInstitucional, Categoria FROM Miembro WHERE CorreoInstitucional = ? AND contrasena = ?";
        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                ps -> {
                    ps.setString(1, email);
                    ps.setString(2, password);
                },
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
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Map<String, Object> authenticatedUser = results.get(0);
        Integer ci = (Integer) authenticatedUser.get("ci");
        if (ci != null) {
            try {
                closeLatestOpenSession(ci);
                createNewSession(ci, resolveClientIp(request));
            } catch (Exception e) {
                logger.error("No se pudo registrar la sesión para CI {}", ci, e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No se pudo registrar la sesión de usuario. Intenta de nuevo.");
                error.put("detail", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        }

        return ResponseEntity.ok(authenticatedUser);
    }

    @PostMapping("/auth/session/close-latest")
    public ResponseEntity<Map<String, Object>> closeLatestSession(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            if (ciValue == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Debes enviar la CI del miembro.");
                return ResponseEntity.badRequest().body(error);
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));
            int updatedRows = closeLatestOpenSession(ci);

            Map<String, Object> response = new HashMap<>();
            response.put("message", updatedRows > 0
                    ? "Sesión cerrada correctamente."
                    : "No había sesiones abiertas para cerrar.");
            response.put("updatedRows", updatedRows);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "CI inválida.");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se pudo cerrar la sesión: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/auth/register")
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

            // Verificar conexión rápida a la BD
            try {
                Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                if (one == null) throw new SQLException("No se pudo validar conexión a la base de datos");
            } catch (Exception ex) {
                throw new SQLException("Error de conexión con la base de datos: " + ex.getMessage(), ex);
            }

            // Parsear FechaNacimiento a java.sql.Date (espera 'YYYY-MM-DD')
            Date fechaNacimiento = null;
            try {
                if (fechaNacimientoStr != null && !fechaNacimientoStr.isBlank()) {
                    LocalDate ld = LocalDate.parse(fechaNacimientoStr);
                    fechaNacimiento = Date.valueOf(ld);
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Formato inválido para FechaNacimiento. Use YYYY-MM-DD.");
            }

            // Usar la columna renombrada `contrasena`
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
            Map<String, Object> error = new HashMap<>();
            error.put("error", "CI inválida");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se pudo registrar el miembro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/auth/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam(required = false) String email,
                                                          @RequestParam(required = false) Integer ci) {
        if ((email == null || email.isBlank()) && ci == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Debes enviar el correo institucional o la CI del miembro.");
            return ResponseEntity.badRequest().body(error);
        }

        String sql = "SELECT CI, PrimerNombre, SegundoNombre, PrimerApellido, SegundoApellido, Sexo, CorreoInstitucional, DireccionHabitacion, FechaNacimiento, Telefono, Categoria, EstadoCuenta, UltimaConexion, FechaCambioContraseña, avatar FROM Miembro WHERE CorreoInstitucional = ? OR CI = ?";
        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                ps -> {
                    ps.setString(1, email != null ? email : "");
                    if (ci != null) {
                        ps.setInt(2, ci);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("ci", rs.getInt("CI"));
                    profile.put("firstName", rs.getString("PrimerNombre"));
                    profile.put("secondName", rs.getString("SegundoNombre"));
                    profile.put("lastName", rs.getString("PrimerApellido"));
                    profile.put("secondLastName", rs.getString("SegundoApellido"));
                    profile.put("sex", rs.getString("Sexo"));
                    profile.put("email", rs.getString("CorreoInstitucional"));
                    profile.put("address", rs.getString("DireccionHabitacion"));
                    Date birthDate = rs.getDate("FechaNacimiento");
                    profile.put("birthDate", birthDate != null ? birthDate.toLocalDate().toString() : null);
                    profile.put("phone", rs.getString("Telefono"));
                    profile.put("category", rs.getString("Categoria"));
                    profile.put("accountStatus", rs.getString("EstadoCuenta"));
                    profile.put("lastConnection", rs.getString("UltimaConexion"));
                    profile.put("passwordChangeDate", rs.getTimestamp("FechaCambioContraseña") != null
                            ? rs.getTimestamp("FechaCambioContraseña").toString()
                            : null);
                        byte[] avatarBytes = rs.getBytes("avatar");
                        profile.put("avatar", avatarBytes != null ? toImageDataUrl(avatarBytes) : null);
                    return profile;
                });

        if (results.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se encontró el perfil del miembro.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(results.get(0));
    }

    @PostMapping("/auth/profile/avatar")
    public ResponseEntity<Map<String, Object>> updateProfileAvatar(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            String avatarData = payload.get("avatar") == null ? null : String.valueOf(payload.get("avatar")).trim();

            if (ciValue == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Debes enviar la CI del miembro.");
                return ResponseEntity.badRequest().body(error);
            }

            if (avatarData == null || avatarData.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Debes seleccionar una imagen válida.");
                return ResponseEntity.badRequest().body(error);
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));
            byte[] avatarBytes = decodeAvatarPayload(avatarData);
            String sql = "UPDATE Miembro SET avatar = ? WHERE CI = ?";
            int updatedRows = jdbcTemplate.update(sql, avatarBytes, ci);

            if (updatedRows == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No se encontró el miembro para actualizar el avatar.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Avatar actualizado correctamente.");
            response.put("ci", ci);
            response.put("avatar", toImageDataUrl(avatarBytes));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "La imagen enviada no tiene un formato válido.");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se pudo actualizar el avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/auth/profile/update")
    public ResponseEntity<Map<String, Object>> updateProfileField(@RequestBody Map<String, Object> payload) {
        try {
            Object ciValue = payload.get("ci");
            String field = String.valueOf(payload.get("field"));
            String value = payload.get("value") == null ? null : String.valueOf(payload.get("value")).trim();

            if (ciValue == null || field == null || field.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Debes enviar la CI y el campo a modificar.");
                return ResponseEntity.badRequest().body(error);
            }

            int ci = Integer.parseInt(String.valueOf(ciValue));

            if (value == null || value.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "El nuevo valor no puede estar vacío.");
                return ResponseEntity.badRequest().body(error);
            }

            String column;
            switch (field) {
                case "secondName" -> column = "SegundoNombre";
                case "secondLastName" -> column = "SegundoApellido";
                case "address" -> column = "DireccionHabitacion";
                case "phone" -> column = "Telefono";
                default -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Campo no permitido para modificación.");
                    return ResponseEntity.badRequest().body(error);
                }
            }

            if ("SegundoNombre".equals(column) || "SegundoApellido".equals(column)) {
                String currentValue = jdbcTemplate.queryForObject(
                        "SELECT " + column + " FROM Miembro WHERE CI = ?",
                        new Object[]{ci},
                        String.class
                );

                if (currentValue != null && !currentValue.isBlank()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", column + " ya tiene valor y no se puede modificar.");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
                }
            }

            String sql = "UPDATE Miembro SET " + column + " = ? WHERE CI = ?";
            int updatedRows = jdbcTemplate.update(sql, value, ci);

            if (updatedRows == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No se encontró el miembro para actualizar.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Campo actualizado correctamente.");
            response.put("field", field);
            response.put("value", value);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "CI inválida.");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se pudo actualizar el perfil: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> listTables() throws SQLException {
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schema = connection.getSchema();
            List<TableInfo> tables = new ArrayList<>();

            try (ResultSet rs = metadata.getTables(connection.getCatalog(), schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(new TableInfo(
                            rs.getString("TABLE_CAT"),
                            rs.getString("TABLE_SCHEM"),
                            rs.getString("TABLE_NAME"),
                            rs.getString("REMARKS")
                    ));
                }
            }
            return ResponseEntity.ok(tables);
        }
    }

    @GetMapping("/tables/{tableName}/columns")
    public ResponseEntity<List<ColumnInfo>> getTableColumns(@PathVariable String tableName) throws SQLException {
        validateTableName(tableName);
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schema = connection.getSchema();
            if (!tableExists(metadata, schema, tableName)) {
                return ResponseEntity.notFound().build();
            }

            List<ColumnInfo> columns = new ArrayList<>();
            try (ResultSet rs = metadata.getColumns(connection.getCatalog(), schema, tableName, "%")) {
                while (rs.next()) {
                    columns.add(new ColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getInt("DATA_TYPE"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getString("IS_NULLABLE"),
                            rs.getString("COLUMN_DEF")
                    ));
                }
            }
            return ResponseEntity.ok(columns);
        }
    }

    @GetMapping("/tables/{tableName}/rows")
    public ResponseEntity<List<Map<String, Object>>> getTableRows(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "100") int limit
    ) throws SQLException {
        validateTableName(tableName);
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schema = connection.getSchema();
            if (!tableExists(metadata, schema, tableName)) {
                return ResponseEntity.notFound().build();
            }
        }

        if (limit <= 0) {
            limit = 100;
        }

        String sql = "SELECT * FROM \"" + tableName + "\" LIMIT ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit);
        return ResponseEntity.ok(rows);
    }

    private void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Nombre de tabla inválido");
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String schema, String tableName) throws SQLException {
        try (ResultSet rs = metadata.getTables(null, schema, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void createNewSession(int ci, String clientIp) {
        Exception lastError = null;
        List<String> mfaCandidates = resolveMfaCandidates();

        String sqlWithoutUuid = "INSERT INTO sesion " +
                "(ci, fechainicio, fechafin, ip, geolocalizacion, protocoloproteccion, mfa, conteointentosfallidos) " +
                "VALUES (?, CURRENT_TIMESTAMP, NULL, ?, ?, ?, ?, ?)";

        for (String mfaValue : mfaCandidates) {
            try {
                jdbcTemplate.update(
                        sqlWithoutUuid,
                        ci,
                        clientIp,
                        "No disponible",
                        Boolean.FALSE,
                        mfaValue,
                        0
                );
                return;
            } catch (Exception e) {
                lastError = e;
                logger.warn("Insert en sesion sin uuid falló con mfa='{}': {}", mfaValue, e.getMessage());
            }
        }

        Integer nextUuid = null;
        try {
            nextUuid = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(uuid), 0) + 1 FROM sesion",
                    Integer.class
            );
        } catch (Exception e) {
            lastError = e;
            logger.warn("No se pudo calcular next uuid en sesion: {}", e.getMessage());
        }

        if (nextUuid != null) {
            String sqlWithUuid = "INSERT INTO sesion " +
                    "(ci, fechainicio, fechafin, ip, geolocalizacion, uuid, protocoloproteccion, mfa, conteointentosfallidos) " +
                    "VALUES (?, CURRENT_TIMESTAMP, NULL, ?, ?, ?, ?, ?, ?)";

            for (String mfaValue : mfaCandidates) {
                try {
                    jdbcTemplate.update(
                            sqlWithUuid,
                            ci,
                            clientIp,
                            "No disponible",
                            nextUuid,
                            Boolean.FALSE,
                            mfaValue,
                            0
                    );
                    return;
                } catch (Exception e) {
                    lastError = e;
                    logger.warn("Insert en sesion con uuid falló con mfa='{}': {}", mfaValue, e.getMessage());
                }
            }
        }

        throw new IllegalStateException(
                "No se pudo insertar en sesion. Ultimo error: " +
                        (lastError != null ? lastError.getMessage() : "desconocido"),
                lastError
        );
    }

    private List<String> resolveMfaCandidates() {
        Set<String> candidates = new LinkedHashSet<>();

        String checkDefinitionSql = "SELECT pg_get_constraintdef(c.oid) " +
                "FROM pg_constraint c " +
                "JOIN pg_class t ON t.oid = c.conrelid " +
                "WHERE t.relname = 'sesion' AND c.contype = 'c' AND c.conname ILIKE '%mfa%' " +
                "ORDER BY c.oid DESC LIMIT 1";

        try {
            String constraintDef = jdbcTemplate.queryForObject(checkDefinitionSql, String.class);
            if (constraintDef != null && !constraintDef.isBlank()) {
                Matcher matcher = Pattern.compile("'([^']*)'").matcher(constraintDef);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    if (value != null && !value.isBlank()) {
                        candidates.add(value);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo leer sesion_mfa_check: {}", e.getMessage());
        }

        if (candidates.isEmpty()) {
            candidates.add("No");
            candidates.add("false");
            candidates.add("N");
            candidates.add("Desactivado");
        }

        return new ArrayList<>(candidates);
    }

    private int closeLatestOpenSession(int ci) {
        String sql = "UPDATE sesion SET fechafin = CURRENT_TIMESTAMP WHERE ci = ? AND fechafin IS NULL " +
                "AND fechainicio = (SELECT MAX(fechainicio) FROM sesion WHERE ci = ? AND fechafin IS NULL)";
        return jdbcTemplate.update(sql, ci, ci);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

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

    private String toImageDataUrl(byte[] avatarBytes) {
        String mimeType = detectImageMimeType(avatarBytes);
        String base64 = Base64.getEncoder().encodeToString(avatarBytes);
        return "data:" + mimeType + ";base64," + base64;
    }

    private byte[] decodeAvatarPayload(String avatarData) {
        String base64 = avatarData;
        int commaIndex = avatarData.indexOf(',');
        if (commaIndex >= 0) {
            base64 = avatarData.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(base64);
    }

    private String detectImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "image/png";
        }

        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E
                && (bytes[3] & 0xFF) == 0x47) {
            return "image/png";
        }

        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }

        if ((bytes[0] & 0xFF) == 'G' && (bytes[1] & 0xFF) == 'I' && (bytes[2] & 0xFF) == 'F') {
            return "image/gif";
        }

        if (bytes.length >= 12
                && (bytes[0] & 0xFF) == 'R'
                && (bytes[1] & 0xFF) == 'I'
                && (bytes[2] & 0xFF) == 'F'
                && (bytes[8] & 0xFF) == 'W'
                && (bytes[9] & 0xFF) == 'E'
                && (bytes[10] & 0xFF) == 'B'
                && (bytes[11] & 0xFF) == 'P') {
            return "image/webp";
        }

        return "image/png";
    }

    public static record TableInfo(String catalog, String schema, String name, String remarks) {
    }

    public static record ColumnInfo(String name, int dataType, String typeName, int size, String nullable, String defaultValue) {
    }

    @PostMapping("/admin/rename-password-column")
    public ResponseEntity<Map<String, Object>> renamePasswordColumn() {
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String schema = connection.getSchema();

            String found = null;
            try (ResultSet rs = metadata.getColumns(connection.getCatalog(), schema, "miembro", "%")) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null && col.toLowerCase().contains("contra")) {
                        found = col;
                        break;
                    }
                }
            }

            if (found == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "No se encontró ninguna columna similar a 'contraseña' en la tabla Miembro");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
            }

            String sql = "ALTER TABLE Miembro RENAME COLUMN \"" + found + "\" TO contrasena";
            jdbcTemplate.execute(sql);

            Map<String, Object> resp = new HashMap<>();
            resp.put("renamedFrom", found);
            resp.put("renamedTo", "contrasena");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    
}
