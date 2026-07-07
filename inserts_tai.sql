-- =====================================================
-- INSERTS DE PRUEBA — TAI, PAGOS Y FACTURAS ASOCIADAS
-- Reemplaza 123456789 por tu CI real
-- =====================================================

-- 1. Asegurar que Miembro tenga IDComprador (si no existe la columna, descomentar)
-- ALTER TABLE Miembro ADD COLUMN IF NOT EXISTS IDComprador INT;
-- ALTER TABLE Miembro ADD CONSTRAINT fk_miembro_comprador FOREIGN KEY (IDComprador) REFERENCES Comprador(IDComprador);

-- 2. Crear Comprador
INSERT INTO Comprador(IDComprador) VALUES (100)
ON CONFLICT DO NOTHING;

-- 3. Vincular el Miembro al Comprador (CAMBIAR 123456789 por tu CI)
UPDATE Miembro SET IDComprador = 100 WHERE CI = 123456789;

-- 4. Tasas de cambio
INSERT INTO Tasa(FechaHoraVigencia, CambioReal) VALUES ('2026-07-01 08:00:00', 36.50)
ON CONFLICT DO NOTHING;
INSERT INTO Tasa(FechaHoraVigencia, CambioReal) VALUES ('2026-07-05 10:00:00', 37.00)
ON CONFLICT DO NOTHING;

-- 5. Métodos de pago
INSERT INTO MetodoPago(IDMetodo, FechaHoraVigencia, MontoRecibido) VALUES (501, '2026-07-01 08:00:00', 25)
ON CONFLICT DO NOTHING;
INSERT INTO MetodoPago(IDMetodo, FechaHoraVigencia, MontoRecibido) VALUES (502, '2026-07-05 10:00:00', 50)
ON CONFLICT DO NOTHING;

-- 6. Pago presencial
INSERT INTO PagoPresencial(IDPresencial, IDMetodo) VALUES (701, 501)
ON CONFLICT DO NOTHING;
INSERT INTO PagoPresencial(IDPresencial, IDMetodo) VALUES (702, 502)
ON CONFLICT DO NOTHING;

-- 7. TAI — registros de pago (UID = tu CI, CAMBIAR 123456789)
INSERT INTO TAI(POS, FechaHoraPago, UID, IDPresencial) VALUES (1, '2026-07-01 08:05:00', 123456789, 701)
ON CONFLICT DO NOTHING;
INSERT INTO TAI(POS, FechaHoraPago, UID, IDPresencial) VALUES (1, '2026-07-05 10:15:00', 123456789, 702)
ON CONFLICT DO NOTHING;

-- 8. Facturas (requiere EstadoCuenta, que requiere Tramite, que requiere Servicio)
-- NOTA: Si no tienes EstadoCuenta creado, primero debes crear un Tramite.
-- Estos INSERT asumen que ya existe un EstadoCuenta con estos valores.
-- Si no, puedes omitir las facturas (la sección TAI se verá sin facturas asociadas).
-- EJEMPLO (cambia los FK según tu data real):
-- INSERT INTO Factura(Numero, Estatus, Deuda, MontoAcumulado, IDComprador,
--                     CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura)
-- VALUES (9001, 'pagada', 0, 25, 100, 123456789, 1, 'Estudiantil', 'Constancia de estudio', '2026-07-01', '2026-07-01');

-- 9. Paga — vincula Método de Pago con Factura y Comprador
-- (necesitas las facturas del paso 8. Ejemplo:)
-- INSERT INTO Paga(IDMetodo, IDComprador, NumeroFactura) VALUES (501, 100, 9001);
-- INSERT INTO Paga(IDMetodo, IDComprador, NumeroFactura) VALUES (502, 100, 9002);
