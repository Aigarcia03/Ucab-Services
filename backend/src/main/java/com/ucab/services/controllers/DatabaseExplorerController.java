package com.ucab.services.controllers;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class DatabaseExplorerController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseExplorerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
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

        return ResponseEntity.ok(results.get(0));
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

        String sql = "SELECT CI, PrimerNombre, SegundoNombre, PrimerApellido, SegundoApellido, Sexo, CorreoInstitucional, DireccionHabitacion, FechaNacimiento, Telefono, Categoria, EstadoCuenta, UltimaConexion, FechaCambioContraseña FROM Miembro WHERE CorreoInstitucional = ? OR CI = ?";
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
                    return profile;
                });

        if (results.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No se encontró el perfil del miembro.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(results.get(0));
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
