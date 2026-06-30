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
