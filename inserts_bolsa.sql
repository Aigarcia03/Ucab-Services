-- ============================================
-- INSERTS DE PRUEBA — BOLSA DE TRABAJO
-- ============================================

-- Organizaciones Externas (ajustar columnas si la tabla tiene más campos obligatorios)
INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
VALUES (1001, '2027-12-31 23:59:59', 'Mercantil Banco Universal', 1);

INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
VALUES (1002, '2027-12-31 23:59:59', 'Polar C.A.', 1);

INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
VALUES (1003, '2027-12-31 23:59:59', 'CANTV', 2);

INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
VALUES (1004, '2027-12-31 23:59:59', 'Digitel', 2);

INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
VALUES (1005, '2027-12-31 23:59:59', 'TechSolutions Venezuela', 1);

-- Oportunidades Laborales
INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1001, '2026-07-01 08:00:00', 'Ingeniero en Informática o afines. Conocimientos en Java, Spring Boot, PostgreSQL.', 'Desarrollador Backend Jr.', 'Seguro médico, fondo de ahorro, bonos trimestrales.', 'Desarrollar y mantener APIs REST. Apoyar en diseño de bases de datos. Documentar código.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1002, '2026-07-02 10:30:00', 'Ingeniero Químico o Industrial. Experiencia en control de calidad.', 'Supervisor de Calidad', 'Comidas subsidiadas, transporte, seguro HCM.', 'Supervisar procesos de producción. Realizar auditorías de calidad. Elaborar informes.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1003, '2026-07-03 14:00:00', 'Técnico en redes o Ing. en Telecomunicaciones.', 'Técnico de Redes', 'Seguro médico, bonos nocturnos, formación continua.', 'Instalar y mantener equipos de red. Atender averías. Monitorear conectividad.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1004, '2026-07-04 09:00:00', 'Ingeniero en Telecomunicaciones con 3+ años de experiencia en redes 4G/5G.', 'Ingeniero de Radiofrecuencia', 'Vehículo corporativo, seguro internacional, bonos por meta.', 'Planificar y optimizar cobertura 4G/5G. Realizar drive tests. Analizar KPIs de red.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1005, '2026-07-05 11:00:00', 'Estudiante o egresado de Ing. Informática. Conocimientos en React y Node.js.', 'Desarrollador Full Stack Trainee', 'Mentoría, certificaciones pagas, horario flexible.', 'Colaborar en el desarrollo de features. Corregir bugs. Participar en code reviews.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1001, '2026-07-06 15:00:00', 'Licenciado en Administración o Contaduría.', 'Analista de Riesgos', 'Seguro médico, plan de jubilación, bonos anuales.', 'Evaluar riesgos crediticios. Elaborar informes financieros. Monitorear carteras.', 'disponible');

INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades, EstatusVacante)
VALUES (1002, '2026-07-07 08:30:00', 'TSU en Informática. Conocimientos en soporte técnico.', 'Soporte Técnico Nivel 1', 'Comidas subsidiadas, seguro médico, crecimiento interno.', 'Atender tickets de soporte. Diagnosticar y resolver incidencias de hardware/software.', 'disponible');

-- ============================================
-- DATOS DE MIEMBROS Y EGRESADOS DE PRUEBA
-- (omitir si ya existen para evitar duplicados)
-- ============================================

-- Egresados de prueba
INSERT INTO Egresado(CI, Titulo, AñoGraduacion, IndiceAcademicoFinal)
VALUES (26123456, 'Ingeniero en Informática', '2024-07-15', 16.5)
ON CONFLICT (CI) DO NOTHING;

INSERT INTO Egresado(CI, Titulo, AñoGraduacion, IndiceAcademicoFinal)
VALUES (25111222, 'Ingeniero Químico', '2023-07-15', 15.2)
ON CONFLICT (CI) DO NOTHING;

INSERT INTO Egresado(CI, Titulo, AñoGraduacion, IndiceAcademicoFinal)
VALUES (31123456, 'Ingeniero en Informática', '2025-07-15', 17.8)
ON CONFLICT (CI) DO NOTHING;

-- ============================================
-- POSTULACIONES DE PRUEBA
-- ============================================
INSERT INTO SePostula(RIF, FechaHoraOferta, CI, Curriculum)
VALUES (1001, '2026-07-01 08:00:00', 26123456, 'Egresado de Ing. Informática UCAB. Experiencia en Java y Spring Boot. Proyectos académicos con PostgreSQL.');

INSERT INTO SePostula(RIF, FechaHoraOferta, CI, Curriculum)
VALUES (1002, '2026-07-02 10:30:00', 25111222, 'Ing. Químico UCAB. 2 años de experiencia en control de calidad en industria alimenticia.');

INSERT INTO SePostula(RIF, FechaHoraOferta, CI, Curriculum)
VALUES (1005, '2026-07-05 11:00:00', 31123456, 'Conocimientos en React, Node.js y MongoDB. Proyecto final UCAB con stack MERN.');
