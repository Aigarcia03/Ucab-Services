package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/procesar")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> procesarPagoOnline(@RequestBody Map<String, Object> body) {
        try {
            String metodo = String.valueOf(body.get("metodo"));

            Object montoVal = body.get("monto");
            if (montoVal == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'monto' es requerido"));
            }
            int monto = (int) Double.parseDouble(montoVal.toString());

            int idMetodo = (int) (System.currentTimeMillis() % 1000000);
            int idLinea = (int) ((System.currentTimeMillis() + 1) % 1000000);
            int idPresencial = (int) ((System.currentTimeMillis() + 2) % 1000000);

            boolean esPresencial = false;
            switch (metodo) {
                case "zelle" -> {
                    Object codObj = body.get("codigoConfirmacion");
                    if (codObj == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El campo 'codigoConfirmacion' es requerido para Zelle"));
                    }
                    int codigo;
                    try {
                        codigo = Integer.parseInt(codObj.toString().replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El código de confirmación debe ser un número válido"));
                    }
                    String correo = String.valueOf(body.getOrDefault("correoElectronico", ""));
                    String pNombre = String.valueOf(body.getOrDefault("primerNombre", ""));
                    String pApellido = String.valueOf(body.getOrDefault("primerApellido", ""));
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "tl AS (INSERT INTO TransaccionEnLinea (IDLinea, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDLinea) "
                        + "INSERT INTO Zelle (CodigoConfirmacion, IDLinea, CorreoElectronico, PrimerNombre, PrimerApellido) "
                        + "SELECT ?, ?, ?, ?, ? FROM tl";
                    jdbcTemplate.update(sql, idMetodo, monto, idLinea, codigo, idLinea, correo, pNombre, pApellido);
                }
                case "crypto" -> {
                    String txid = String.valueOf(body.getOrDefault("txid", ""));
                    String red = String.valueOf(body.getOrDefault("red", "BTC"));
                    String direccion = String.valueOf(body.getOrDefault("direccionBilletera", ""));
                    Object tasaObj = body.get("tasaConversion");
                    if (tasaObj == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El campo 'tasaConversion' es requerido para Crypto"));
                    }
                    double tasaConv = Double.parseDouble(tasaObj.toString());
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "tl AS (INSERT INTO TransaccionEnLinea (IDLinea, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDLinea) "
                        + "INSERT INTO Criptomoneda (TXID, IDLinea, Red, DireccionBilletera, TasaConversion) "
                        + "SELECT ?, ?, ?, ?, ? FROM tl";
                    jdbcTemplate.update(sql, idMetodo, monto, idLinea, txid, idLinea, red, direccion, tasaConv);
                }
                case "tarjeta" -> {
                    esPresencial = true;
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Tarjeta (NroTarjeta, \"compa\u00f1iaemisora\", MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial) "
                        + "SELECT CAST(? AS BIGINT), ?, ?, ?, CAST(? AS DATE), IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, monto, idPresencial,
                        String.valueOf(body.get("nroTarjeta")), String.valueOf(body.get("companiaEmisora")),
                        String.valueOf(body.get("monedaLiquidacion")), String.valueOf(body.get("tipoRed")),
                        String.valueOf(body.get("fechaVencimiento")));
                }
                case "pagomovil" -> {
                    esPresencial = true;
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO PagoMovil (NroReferencia, TelefonoEmisor, Banco, IDPresencial) "
                        + "SELECT ?, ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, monto, idPresencial,
                        Long.parseLong(String.valueOf(body.get("nroReferencia"))),
                        String.valueOf(body.get("telefonoEmisor")),
                        String.valueOf(body.get("banco")));
                }
                case "tai" -> {
                    esPresencial = true;
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO TAI (POS, UID, IDPresencial) "
                        + "SELECT ?, ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, monto, idPresencial,
                        Long.parseLong(String.valueOf(body.get("pos"))),
                        Long.parseLong(String.valueOf(body.get("uid"))));
                }
                case "efectivo" -> {
                    esPresencial = true;
                    String sql = "WITH mp AS (INSERT INTO MetodoPago (IDMetodo, MontoRecibido) VALUES (?, ?) RETURNING IDMetodo), "
                        + "pp AS (INSERT INTO PagoPresencial (IDPresencial, IDMetodo) SELECT ?, IDMetodo FROM mp RETURNING IDPresencial) "
                        + "INSERT INTO Efectivo (MonedaDeCurso, IDPresencial) "
                        + "SELECT ?, IDPresencial FROM pp";
                    jdbcTemplate.update(sql, idMetodo, monto, idPresencial,
                        String.valueOf(body.getOrDefault("monedaDeCurso", "bolivares")));
                }
                default -> {
                    return ResponseEntity.badRequest().body(Map.of("error", "Método de pago no soportado: " + metodo));
                }
            }

            // Si llegaron identificadores del trámite, crear Factura y vincular
            Object ciObj = body.get("ci");
            if (ciObj != null) {
                int ci = Integer.parseInt(ciObj.toString());
                int idPrestadora = Integer.parseInt(body.getOrDefault("idPrestadora", "0").toString());
                String nombreCategoria = String.valueOf(body.getOrDefault("nombreCategoria", ""));
                String descripcion = String.valueOf(body.getOrDefault("descripcion", ""));

                // Obtener FechaCreacion exacta desde el body o desde Tramite por rango
                Timestamp fechaCreacion;
                Object fcBody = body.get("fechaCreacion");
                if (fcBody != null) {
                    String fcStr = fcBody.toString().strip();
                    try {
                        fechaCreacion = Timestamp.valueOf(fcStr);
                    } catch (IllegalArgumentException e1) {
                        try {
                            fechaCreacion = Timestamp.from(java.time.Instant.parse(fcStr));
                        } catch (Exception e2) {
                            fechaCreacion = Timestamp.valueOf(fcStr.replace("T", " ").substring(0, 19));
                        }
                    }
                    List<Map<String, Object>> tramites = jdbcTemplate.queryForList(
                        "SELECT * FROM Tramite WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? " +
                        "AND CAST(? AS TIMESTAMP) BETWEEN FechaCreacion - INTERVAL '1 millisecond' AND FechaCreacion + INTERVAL '1 millisecond' " +
                        "ORDER BY FechaCreacion DESC LIMIT 1",
                        ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);
                    if (tramites.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No se encontró el trámite especificado"));
                    }
                    Object exactFc = tramites.get(0).get("fechacreacion");
                    if (exactFc instanceof Timestamp) {
                        fechaCreacion = (Timestamp) exactFc;
                    } else {
                        fechaCreacion = Timestamp.valueOf(exactFc.toString());
                    }
                } else {
                    try {
                        Object fcObj = jdbcTemplate.queryForObject(
                            "SELECT FechaCreacion FROM Tramite WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? ORDER BY FechaCreacion DESC LIMIT 1",
                            Object.class, ci, idPrestadora, nombreCategoria, descripcion);
                        if (fcObj instanceof Timestamp) {
                            fechaCreacion = (Timestamp) fcObj;
                        } else if (fcObj instanceof java.sql.Date) {
                            fechaCreacion = new Timestamp(((java.sql.Date) fcObj).getTime());
                        } else {
                            String s = fcObj.toString();
                            if (s.length() <= 10) s += " 00:00:00";
                            fechaCreacion = Timestamp.valueOf(s);
                        }
                    } catch (EmptyResultDataAccessException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No se encontró el trámite especificado"));
                    }
                }

                Integer idComprador = jdbcTemplate.queryForObject(
                    "SELECT IDComprador FROM Miembro WHERE CI = ?", Integer.class, ci);
                if (idComprador == null) {
                    int newId = (int) ((System.currentTimeMillis() + 2) % 1000000);
                    jdbcTemplate.update("INSERT INTO Comprador(IDComprador) VALUES (?)", newId);
                    jdbcTemplate.update("UPDATE Miembro SET IDComprador = ? WHERE CI = ?", newId, ci);
                    idComprador = newId;
                }

                // Obtener MesAñoApertura del EstadoCuenta (debe coincidir con el que creó el trigger)
                String mesAñoAperturaStr = jdbcTemplate.queryForObject(
                    "SELECT MesAñoApertura::text FROM EstadoCuenta WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                    String.class, ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

                // Cerrar EstadoCuenta
                jdbcTemplate.update(
                    "UPDATE EstadoCuenta SET EstadoFiscal = 'cerrado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                    ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

                int numeroFactura = jdbcTemplate.queryForObject(
                    "INSERT INTO Factura (IDComprador, CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS DATE)) RETURNING Numero",
                    Integer.class, idComprador, ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion, mesAñoAperturaStr);

                jdbcTemplate.update("INSERT INTO PagaFactura (IDMetodo, NumeroFactura) VALUES (?, ?)", idMetodo, numeroFactura);

                // Finalizar el trámite
                jdbcTemplate.update(
                    "UPDATE Tramite SET Estado = 'finalizado' WHERE CI = ? AND IDPrestadora = ? AND NombreCategoria = ? AND Descripcion = ? AND FechaCreacion = ?",
                    ci, idPrestadora, nombreCategoria, descripcion, fechaCreacion);

                return ResponseEntity.ok(Map.of(
                    "mensaje", "Pago registrado correctamente. Factura #" + numeroFactura + " generada.",
                    "metodo", metodo, "monto", monto, "nroFactura", numeroFactura));
            }

            return ResponseEntity.ok(Map.of("mensaje", "Pago registrado correctamente.", "metodo", metodo, "monto", monto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al procesar el pago: " + e.getMessage()));
        }
    }

}
