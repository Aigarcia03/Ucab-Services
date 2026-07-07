-- ============================================
-- INSERTS DE PRUEBA — VIAJES POR SEDES
-- (Asumiendo que Sede ya tiene Montalbán y Guayana)
-- ============================================

-- Medios de Transporte
INSERT INTO MedioTransporte(Placa, CarnetDeConducir, Ubicacion, Disponibilidad, TipoVehiculo, Capacidad)
VALUES ('ABC-123', 'LIC-001', 'Montalbán', TRUE, 'autobús', 40);

INSERT INTO MedioTransporte(Placa, CarnetDeConducir, Ubicacion, Disponibilidad, TipoVehiculo, Capacidad)
VALUES ('DEF-456', 'LIC-002', 'Montalbán', TRUE, 'carro', 4);

INSERT INTO MedioTransporte(Placa, CarnetDeConducir, Ubicacion, Disponibilidad, TipoVehiculo, Capacidad)
VALUES ('GHI-789', 'LIC-003', 'Guayana', TRUE, 'autobús', 40);

INSERT INTO MedioTransporte(Placa, CarnetDeConducir, Ubicacion, Disponibilidad, TipoVehiculo, Capacidad)
VALUES ('JKL-012', 'LIC-004', 'Guayana', FALSE, 'carro', 4);

-- Viajes desde Montalbán hacia Guayana
INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-06 08:00:00', '2026-07-06 12:30:00', 'ABC-123', 'LIC-001', 'Guayana');

INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-06 14:00:00', '2026-07-06 18:30:00', 'ABC-123', 'LIC-001', 'Guayana');

INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-06 09:00:00', '2026-07-06 12:00:00', 'DEF-456', 'LIC-002', 'Guayana');

-- Viajes desde Guayana hacia Montalbán
INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-06 06:00:00', '2026-07-06 10:30:00', 'GHI-789', 'LIC-003', 'Montalbán');

INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-06 15:00:00', '2026-07-06 19:30:00', 'GHI-789', 'LIC-003', 'Montalbán');

-- Viaje con vehículo no disponible (Disponibilidad = FALSE)
INSERT INTO Viaje(FechaHoraInicio, FechaHoraFin, Placa, CarnetDeConducir, Destino)
VALUES ('2026-07-07 10:00:00', '2026-07-07 14:00:00', 'JKL-012', 'LIC-004', 'Montalbán');
