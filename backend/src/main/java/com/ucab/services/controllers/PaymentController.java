package com.ucab.services.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/procesar")
    public ResponseEntity<?> procesarPago(@RequestBody Map<String, Object> body) {
        try {
            String categoria = body.get("categoria").toString();
            String metodo = body.get("metodo").toString();
            double monto = Double.parseDouble(body.get("monto").toString());
            LocalDateTime ahora = LocalDateTime.now();

            // 1. Crear Tasa
            jdbcTemplate.update(
                "INSERT INTO Tasa(FechaHoraVigencia, CambioReal) VALUES (?, ?)",
                ahora, 1.0
            );

            // 2. Crear MetodoPago
            int idMetodo = (int) (System.currentTimeMillis() % 1000000);
            jdbcTemplate.update(
                "INSERT INTO MetodoPago(IDMetodo, FechaHoraVigencia, MontoRecibido) VALUES (?, ?, ?)",
                idMetodo, ahora, (int) monto
            );

            if ("online".equals(categoria)) {
                // 3a. Crear TransaccionEnLinea
                int idLinea = (int) ((System.currentTimeMillis() + 1) % 1000000);
                jdbcTemplate.update(
                    "INSERT INTO TransaccionEnLinea(IDLinea, IDMetodo) VALUES (?, ?)",
                    idLinea, idMetodo
                );

                if ("zelle".equals(metodo)) {
                    int codigo = Integer.parseInt(body.get("codigoConfirmacion").toString());
                    String correo = body.get("correoElectronico").toString();
                    String pNombre = body.get("primerNombre").toString();
                    String pApellido = body.get("primerApellido").toString();

                    jdbcTemplate.update(
                        "INSERT INTO Zelle(CodigoConfirmacion, IDLinea, CorreoElectronico, PrimerNombre, PrimerApellido) VALUES (?, ?, ?, ?, ?)",
                        codigo, idLinea, correo, pNombre, pApellido
                    );
                } else if ("crypto".equals(metodo)) {
                    String txid = body.get("txid").toString();
                    String red = body.get("red").toString();
                    String direccion = body.get("direccionBilletera").toString();
                    double tasaConv = Double.parseDouble(body.get("tasaConversion").toString());

                    jdbcTemplate.update(
                        "INSERT INTO Criptomoneda(TXID, IDLinea, Red, DireccionBilletera, TasaConversion) VALUES (?, ?, ?, ?, ?)",
                        txid, idLinea, red, direccion, tasaConv
                    );
                }
            } else {
                // 3b. Crear PagoPresencial
                int idPresencial = (int) ((System.currentTimeMillis() + 2) % 1000000);
                jdbcTemplate.update(
                    "INSERT INTO PagoPresencial(IDPresencial, IDMetodo) VALUES (?, ?)",
                    idPresencial, idMetodo
                );

                if ("movil".equals(metodo)) {
                    int nroRef = Integer.parseInt(body.get("nroReferencia").toString());
                    String telefono = body.get("telefonoEmisor").toString();
                    String banco = body.get("banco").toString();

                    jdbcTemplate.update(
                        "INSERT INTO PagoMovil(NroReferencia, TelefonoEmisor, Banco, IDPresencial) VALUES (?, ?, ?, ?)",
                        nroRef, telefono, banco, idPresencial
                    );
                } else if ("tarjeta".equals(metodo)) {
                    int nroTarjeta = Integer.parseInt(body.get("nroTarjeta").toString());
                    String compania = body.get("companiaEmisora").toString();
                    String moneda = body.get("monedaLiquidacion").toString();
                    String tipoRed = body.get("tipoRed").toString();
                    String fechaVen = body.get("fechaVencimiento").toString() + "-01";

                    jdbcTemplate.update(
                        "INSERT INTO Tarjeta(FechaHoraPago, NroTarjeta, CompañiaEmisora, MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial) VALUES (?, ?, ?, ?, ?, CAST(? AS DATE), ?)",
                        ahora, nroTarjeta, compania, moneda, tipoRed, fechaVen, idPresencial
                    );
                } else if ("efectivo".equals(metodo)) {
                    String monedaCurso = body.get("monedaDeCurso").toString();

                    jdbcTemplate.update(
                        "INSERT INTO Efectivo(FechaHoraPago, MonedaDeCurso, IDPresencial) VALUES (?, ?, ?)",
                        ahora, monedaCurso, idPresencial
                    );
                } else if ("tai".equals(metodo)) {
                    int pos = Integer.parseInt(body.get("pos").toString());
                    int uid = Integer.parseInt(body.get("uid").toString());

                    jdbcTemplate.update(
                        "INSERT INTO TAI(POS, FechaHoraPago, UID, IDPresencial) VALUES (?, ?, ?, ?)",
                        pos, ahora, uid, idPresencial
                    );
                }
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "Pago registrado correctamente.",
                "metodo", metodo,
                "monto", monto,
                "fecha", ahora.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al procesar el pago: " + e.getMessage()));
        }
    }
}
