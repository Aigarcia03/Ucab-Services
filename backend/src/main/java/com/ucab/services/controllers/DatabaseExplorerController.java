package com.ucab.services.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

        if (limit <= 0) limit = 100;

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

    public static record TableInfo(String catalog, String schema, String name, String remarks) {}
    public static record ColumnInfo(String name, int dataType, String typeName, int size, String nullable, String defaultValue) {}
}
