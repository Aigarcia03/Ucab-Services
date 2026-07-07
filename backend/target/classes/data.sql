-- Insert Sedes
INSERT INTO Sede(Ubicacion) VALUES ('Montalbán') ON CONFLICT DO NOTHING;
INSERT INTO Sede(Ubicacion) VALUES ('Guayana') ON CONFLICT DO NOTHING;

-- Insert Categorias
INSERT INTO Categoria(Nombre, CostoMaximo) VALUES ('Estudiantil', 100) ON CONFLICT DO NOTHING;
INSERT INTO Categoria(Nombre, CostoMaximo) VALUES ('Laboratorios', 200) ON CONFLICT DO NOTHING;

-- Insert Entidad Prestadora
INSERT INTO EntidadPrestadora(IDPrestadora) VALUES (1) ON CONFLICT DO NOTHING;
INSERT INTO EntidadPrestadora(IDPrestadora) VALUES (2) ON CONFLICT DO NOTHING;

-- Insert Servicios (Estudiantil)
INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
VALUES ('Estudiantil', 1, 'Constancia de estudio. Certifica la inscripción activa del estudiante en el período académico en curso.', 5.0, 0.0, 'Montalbán') ON CONFLICT DO NOTHING;

INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
VALUES ('Estudiantil', 1, 'Notas certificadas. Récord académico oficial con firma y sello de Secretaría.', 25.0, 0.0, 'Montalbán') ON CONFLICT DO NOTHING;

INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
VALUES ('Estudiantil', 1, 'Certificación de programas. Copia sellada de los contenidos programáticos (pensum) de las materias aprobadas.', 30.0, 0.0, 'Montalbán') ON CONFLICT DO NOTHING;

-- Insert Servicios (Laboratorios)
INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
VALUES ('Laboratorios', 2, 'Laboratorio de Fabricación Digital. Ubicación: Edificio Cinter, Planta Baja. Capacidad: 15 personas. Equipamiento: Impresoras 3D (FDM y Resina), Cortadora Láser, Fresadoras CNC.', 100.0, 0.0, 'Montalbán') ON CONFLICT DO NOTHING;

INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
VALUES ('Laboratorios', 2, 'Laboratorio de Psicología Experimental. Ubicación: Edificio Laboratorios, Piso 3. Capacidad: 8 personas. Equipamiento: Equipos de Eye-tracking, sensores de respuesta galvánica de la piel y cámaras de Gesell.', 80.0, 0.0, 'Montalbán') ON CONFLICT DO NOTHING;
