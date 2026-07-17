CREATE TABLE Miembro(
    CI INT PRIMARY KEY,
    PrimerNombre VARCHAR(50) NOT NULL,
    PrimerApellido VARCHAR(50) NOT NULL,
    SegundoNombre VARCHAR(50), --Puede ser NULL
    SegundoApellido VARCHAR(50), --Puede ser NULL
    Sexo CHAR(1) NOT NULL CHECK(Sexo IN ('F', 'M')),
    CorreoInstitucional VARCHAR(50) UNIQUE NOT NULL,
    DireccionHabitacion TEXT NOT NULL,
    FechaNacimiento DATE NOT NULL,
    Telefono VARCHAR(20) NOT NULL,
    Categoria VARCHAR(50) NOT NULL CHECK(Categoria IN ('frecuente', 'preferencial')) DEFAULT 'frecuente',
    Contraseña VARCHAR(50) NOT NULL,
    Foto BYTEA,
    EstadoCuenta VARCHAR(50) CHECK(EstadoCuenta IN ('activa', 'suspendida', 'bloqueada')) DEFAULT 'activa',
    UltimaConexion VARCHAR(50) NOT NULL,
    FechaCambioContraseña TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Sesion(
    CI INT,
    FechaInicio TIMESTAMP,
    FechaFin TIMESTAMP CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    IP INET DEFAULT inet_client_addr(),
    Geolocalizacion VARCHAR(50) NOT NULL,
    UUID INT NOT NULL, -- Se obtiene a partir de Fingerprinting en JavaScript
    ProtocoloProteccion BOOLEAN DEFAULT FALSE, --Cuando se active restringe privilegios en tiempo real.
    MFA VARCHAR(50) CHECK(MFA IN ('activo', 'desactivado', 'pendiente')) DEFAULT 'pendiente',
    ConteoIntentosFallidos INT NOT NULL,
    CONSTRAINT fk_sesion_miembro FOREIGN KEY(CI) REFERENCES Miembro(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_sesion PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Rol(
    CI INT,
    FechaInicio DATE,
    FechaFin DATE CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    CONSTRAINT fk_rol_miembro FOREIGN KEY(CI) REFERENCES Miembro(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_rol PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Estudiante(
    CI INT,
    FechaInicio DATE,
    Semestre INT NOT NULL CHECK(Semestre > 0),
    Escuela VARCHAR(50) NOT NULL,
    UnidadesCreditoAprobadas INT NOT NULL CHECK(UnidadesCreditoAprobadas >= 0),
    PromedioPonderado REAL NOT NULL CHECK(PromedioPonderado >= 0 AND PromedioPonderado <= 20),
    FacultadAdscripcion VARCHAR(50) NOT NULL,
    CONSTRAINT fk_estudiante_rol FOREIGN KEY(CI, FechaInicio) REFERENCES Rol(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_estudiante PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Becario(
    CI INT,
    FechaInicio DATE,
    CumplimientoAcademico BOOLEAN DEFAULT TRUE,
    EstatusBeca VARCHAR(50) CHECK(EstatusBeca IN ('activo', 'suspendido', 'finalizado')) DEFAULT 'activo',
    EstatusBeneficio BOOLEAN DEFAULT TRUE,
    TipoBeca VARCHAR(50) NOT NULL CHECK(TipoBeca IN ('ayuda económica', 'excelencia', 'comedor')),
    CONSTRAINT fk_becario_estudiante FOREIGN KEY(CI, FechaInicio) REFERENCES Estudiante(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_becario PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Preparador(
    CI INT,
    FechaInicio DATE,
    Asignatura VARCHAR(50) NOT NULL,
    HorasAyudantia INT NOT NULL,
    CONSTRAINT fk_preparador_estudiante FOREIGN KEY(CI, FechaInicio) REFERENCES Estudiante(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_preparador PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Empleado(
    CI INT,
    FechaInicio DATE,
    CargaHorariaSemanal INT NOT NULL,
    CONSTRAINT fk_empleado_rol FOREIGN KEY(CI, FechaInicio) REFERENCES Rol(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_empleado PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Profesor(
    CI INT,
    FechaInicio DATE,
    EscalafonDocente VARCHAR(50) NOT NULL,
    CodigoInvestigador VARCHAR(50) NOT NULL,
    CONSTRAINT fk_profesor_empleado FOREIGN KEY(CI, FechaInicio) REFERENCES Empleado(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_profesor PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE PersonalAdministrativo(
    CI INT,
    FechaInicio DATE,
    UnidadAdscripcionPresupuestaria VARCHAR(50) NOT NULL,
    CargoAdministrativo VARCHAR(50) NOT NULL,
    CONSTRAINT fk_personal_administrativo_empleado FOREIGN KEY(CI, FechaInicio) REFERENCES Empleado(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_personal_administrativo PRIMARY KEY(CI, FechaInicio)
);

CREATE TABLE Curso(
    Materia VARCHAR(50),
    FechaInicio DATE,
    FechaFin DATE CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    CONSTRAINT pk_curso PRIMARY KEY(Materia, FechaInicio)
);

CREATE TABLE Asiste(
    CIEstudiante INT,
    CIProfesor INT,
    Materia VARCHAR(50),
    FechaInicio DATE,
    Nota REAL NOT NULL CHECK(Nota >= 0 AND Nota <= 20),
    CONSTRAINT fk_asiste_estudiante FOREIGN KEY(CIEstudiante, FechaInicio) REFERENCES Estudiante(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_asiste_profesor FOREIGN KEY(CIProfesor, FechaInicio) REFERENCES Profesor(CI, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_asiste_curso FOREIGN KEY(Materia, FechaInicio) REFERENCES Curso(Materia, FechaInicio) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_asiste PRIMARY KEY(CIEstudiante, CIProfesor, Materia, FechaInicio)
);

CREATE TABLE Beneficiario(
    DocumentoIdentidad BYTEA PRIMARY KEY,
    Parentesco VARCHAR(50) NOT NULL,
    -- Edad INT NOT NULL CHECK(Edad >= 0), --Podemos calcular la edad a partir de la fecha de nacimiento, pero la dejamos por si acaso
    PrimerNombre VARCHAR(50) NOT NULL,
    PrimerApellido VARCHAR(50) NOT NULL,
    SegundoNombre VARCHAR(50), --Puede ser NULL
    SegundoApellido VARCHAR(50), --Puede ser NULL
    FechaNacimiento DATE NOT NULL,
    FechaInicio DATE NOT NULL,
    FechaFin DATE CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    CentroEducativoInicial VARCHAR(50), --Puede ser NULL, solo para beneficiarios menores de edad
    EsquemaVacunacion BYTEA, --Puede ser NULL, solo para beneficiarios menores de edad
    ConstanciaEstudiosUniversitarios BYTEA, --Puede ser NULL, solo para beneficiarios mayores de edad
    CertificadoSolteria BYTEA --Puede ser NULL, solo para beneficiarios mayores de edad
);

CREATE TABLE Registra(
    DocumentoIdentidad BYTEA,
    CI INT,
    CONSTRAINT fk_registra_beneficiario FOREIGN KEY(DocumentoIdentidad) REFERENCES Beneficiario(DocumentoIdentidad) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_registra_miembro FOREIGN KEY(CI) REFERENCES Miembro(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_registra PRIMARY KEY(DocumentoIdentidad, CI)
);

CREATE TABLE Egresado(
    CI INT,
    Titulo VARCHAR(50) NOT NULL,
    AñoGraduacion DATE NOT NULL,
    IndiceAcademicoFinal REAL NOT NULL CHECK(IndiceAcademicoFinal >= 0 AND IndiceAcademicoFinal <= 20),
    CONSTRAINT fk_egresado_estudiante FOREIGN KEY(CI) REFERENCES Miembro(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_egresado PRIMARY KEY(CI)
);

CREATE TABLE Sede(
    Ubicacion VARCHAR(50) CHECK(Ubicacion IN ('Montalbán', 'Guayana')) PRIMARY KEY
);

CREATE TABLE Edificacion(
    Direccion VARCHAR(50),
    Nombre VARCHAR(50),
    Ubicacion VARCHAR(50) NOT NULL,
    CONSTRAINT fk_edificacion_sede FOREIGN KEY(Ubicacion) REFERENCES Sede(Ubicacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_edificacion PRIMARY KEY(Direccion, Nombre)
);

CREATE TABLE EspacioFisico(
    NroIdentificador INT,
    Direccion VARCHAR(50),
    Nombre VARCHAR(50),
    CapacidadMaxima INT NOT NULL CHECK(CapacidadMaxima > 0),
    TipoDeMobiliario VARCHAR(50) NOT NULL,
    CONSTRAINT fk_espacio_edificacion FOREIGN KEY(Direccion, Nombre) REFERENCES Edificacion(Direccion, Nombre) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_espacio_fisico PRIMARY KEY(NroIdentificador, Direccion, Nombre)
);

CREATE TABLE BloqueHorario(
    NroIdentificador INT,
    Direccion VARCHAR(50),
    Nombre VARCHAR(50),
    FechaHoraInicio TIMESTAMP,
    FechaHoraFin TIMESTAMP CHECK(FechaHoraFin >= FechaHoraInicio),
    Disponibilidad BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_bloque_espacio FOREIGN KEY(NroIdentificador, Direccion, Nombre) REFERENCES EspacioFisico(NroIdentificador, Direccion, Nombre),
    CONSTRAINT pk_bloque PRIMARY KEY(NroIdentificador, Direccion, Nombre, FechaHoraInicio, FechaHoraFin)
);

CREATE TABLE RecursosTecnologicos(
    NroIdentificador INT,
    Direccion VARCHAR(50),
    Nombre VARCHAR(50),
    Recurso VARCHAR(50) NOT NULL,
    Estado BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_recurso_espacio FOREIGN KEY(NroIdentificador, Direccion, Nombre) REFERENCES EspacioFisico(NroIdentificador, Direccion, Nombre) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_recursos_tecnologicos PRIMARY KEY(NroIdentificador, Direccion, Nombre, Recurso, Estado)
);

CREATE TABLE MedioTransporte(
    Placa VARCHAR(15), --El tamaño máximo en el mundo es de 14 caracteres alfanuméricos.
    Ubicacion VARCHAR(50) NOT NULL,
    CarnetDeConducir BYTEA,
    Disponibilidad BOOLEAN DEFAULT TRUE,
    TipoVehiculo VARCHAR(50) NOT NULL CHECK(TipoVehiculo IN ('carro', 'autobús')),
    Capacidad INT NOT NULL CHECK(Capacidad > 0),
    CONSTRAINT fk_medio_sede FOREIGN KEY(Ubicacion) REFERENCES Sede(Ubicacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_medio_transporte PRIMARY KEY(Placa, CarnetDeConducir)
);

CREATE TABLE Viaje(
    FechaHoraInicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FechaHoraFin TIMESTAMP CHECK(FechaHoraFin >= FechaHoraInicio), --Puede ser NULL
    Placa VARCHAR(15),
    CarnetDeConducir BYTEA,
    Destino VARCHAR(50) NOT NULL,
    CONSTRAINT fk_viaje_medio FOREIGN KEY(Placa, CarnetDeConducir) REFERENCES MedioTransporte(Placa, CarnetDeConducir) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_viaje PRIMARY KEY(FechaHoraInicio, Placa, CarnetDeConducir)
);

CREATE TABLE Categoria(
    Nombre VARCHAR(50) PRIMARY KEY,
    CostoMaximo INT NOT NULL
);

CREATE TABLE EntidadPrestadora(
    IDPrestadora INT PRIMARY KEY
);

CREATE TABLE EntidadInterna (
    CodigoPresupuestario INT,
    DirectorOficina VARCHAR(50),
    IDPrestadora INT NOT NULL,
    CONSTRAINT pk_entidad_interna PRIMARY KEY(CodigoPresupuestario, DirectorOficina),
    CONSTRAINT fk_prestadora_interna FOREIGN KEY(IDPrestadora) REFERENCES EntidadPrestadora(IDPrestadora) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE OrganizacionExterna (
    RIF VARCHAR(15) PRIMARY KEY,
    FechaVencimientoContrato DATE NOT NULL,
    RazonSocial VARCHAR(50) NOT NULL,
    IDPrestadora INT NOT NULL,
    CONSTRAINT fk_organizacion_prestadora FOREIGN KEY(IDPrestadora) REFERENCES EntidadPrestadora(IDPrestadora) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE ContactosLegales(
    RIF VARCHAR(15) NOT NULL,
    Telefono VARCHAR(20) NOT NULL,
    Nombre VARCHAR(50) NOT NULL,
    CONSTRAINT fk_contacto_organizacion FOREIGN KEY(RIF) REFERENCES OrganizacionExterna(RIF) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_contactos_legales PRIMARY KEY(RIF, Telefono, Nombre)
);

CREATE TABLE OportunidadLaboral(
    RIF VARCHAR(15) NOT NULL,
    FechaHoraOferta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PerfilBuscado TEXT NOT NULL,
    Cargo VARCHAR(50) NOT NULL,
    Beneficios TEXT NOT NULL,
    Responsabilidades TEXT NOT NULL,
    EstatusVacante VARCHAR(50) CHECK(EstatusVacante IN ('disponible', 'ocupada')) DEFAULT 'disponible',
    CONSTRAINT fk_oportunidad_organizacion FOREIGN KEY(RIF) REFERENCES OrganizacionExterna(RIF) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_oportunidad_laboral PRIMARY KEY(RIF, FechaHoraOferta)
);

CREATE TABLE SePostula(
    RIF VARCHAR(15) NOT NULL,
    FechaHoraOferta TIMESTAMP,
    CI INT NOT NULL,
    Curriculum BYTEA NOT NULL,
    Resultado VARCHAR(50) CHECK(Resultado IN ('en revisión', 'aceptado', 'rechazado')) DEFAULT 'en revisión',
    CONSTRAINT fk_postula_oportunidad FOREIGN KEY(RIF, FechaHoraOferta) REFERENCES OportunidadLaboral(RIF, FechaHoraOferta) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_postula_miembro FOREIGN KEY(CI) REFERENCES Egresado(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_se_postula PRIMARY KEY(RIF, FechaHoraOferta, CI)
);

CREATE TABLE Servicio(
    NombreCategoria VARCHAR(50) NOT NULL,
    IDPrestadora INT NOT NULL,
    Descripcion TEXT NOT NULL,
    PrecioBase REAL NOT NULL UNIQUE,
    Ajuste REAL NOT NULL,
    Ubicacion VARCHAR(50) NOT NULL,
    CONSTRAINT fk_servicio_categoria FOREIGN KEY(NombreCategoria) REFERENCES Categoria(Nombre) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_servicio_prestadora FOREIGN KEY(IDPrestadora) REFERENCES EntidadPrestadora(IDPrestadora) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_servicio_sede FOREIGN KEY(Ubicacion) REFERENCES Sede(Ubicacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_servicio PRIMARY KEY(NombreCategoria, IDPrestadora, Descripcion)
);

CREATE TABLE Requisito(
    Acreditacion VARCHAR(50) PRIMARY KEY
);

CREATE TABLE Exige(
    Acreditacion VARCHAR(50) NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    IDPrestadora INT NOT NULL,
    Descripcion TEXT NOT NULL,
    CONSTRAINT fk_exige_requisito FOREIGN KEY(Acreditacion) REFERENCES Requisito(Acreditacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_exige_servicio FOREIGN KEY(NombreCategoria, IDPrestadora, Descripcion) REFERENCES Servicio(NombreCategoria, IDPrestadora, Descripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_exige PRIMARY KEY(Acreditacion, NombreCategoria, IDPrestadora, Descripcion)
);

CREATE TABLE Tarifa(
    NombreCategoria VARCHAR(50) NOT NULL,
    IDPrestadora INT NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaInicio DATE NOT NULL,
    Perfil VARCHAR(50) NOT NULL CHECK(Perfil IN ('miembro activo', 'egresado', 'público externo')),
    CostoFinal REAL NOT NULL,
    FechaFin DATE CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    CONSTRAINT fk_tarifa_servicio FOREIGN KEY(NombreCategoria, IDPrestadora, Descripcion) REFERENCES Servicio(NombreCategoria, IDPrestadora, Descripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_tarifa PRIMARY KEY(NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil)
);

CREATE TABLE Tramite(
    CI INT NOT NULL,
    IDPrestadora INT NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaCreacion TIMESTAMP NOT NULL,
    FechaCierre DATE, --Puede ser NULL
    Estado VARCHAR(50) CHECK(Estado IN ('activo', 'finalizado')) DEFAULT 'activo',
    TiempoResolucion INTERVAL, --Duración calculada a partir de las fechas de creación y cierre.
    CONSTRAINT fk_tramite_miembro FOREIGN KEY(CI) REFERENCES Miembro(CI) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tramite_servicio FOREIGN KEY(NombreCategoria, IDPrestadora, Descripcion) REFERENCES Servicio(NombreCategoria, IDPrestadora, Descripcion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_tramite PRIMARY KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion)
);

CREATE TABLE Feriado(
    Fecha DATE PRIMARY KEY,
    Nombre VARCHAR(50) NOT NULL
);

CREATE TABLE Acompañante(
    CI INT PRIMARY KEY,
    CIMiembro INT NOT NULL,
    IDPrestadora INT NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaCreacion TIMESTAMP NOT NULL,
    PrimerNombre VARCHAR(50) NOT NULL,
    PrimerApellido VARCHAR(50) NOT NULL,
    SegundoNombre VARCHAR(50), --Puede ser NULL
    SegundoApellido VARCHAR(50), --Puede ser NULL
    Estado VARCHAR(50) CHECK(Estado IN ('activo', 'archivado')) DEFAULT 'activo',
    CONSTRAINT fk_acompanante_tramite FOREIGN KEY(CIMiembro, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) REFERENCES Tramite(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE PasoActividad(
    CI INT NOT NULL,
    IDPrestadora INT NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaCreacion TIMESTAMP NOT NULL,
    OrdenSecuencial INT NOT NULL,
    DescripcionInteraccion TEXT NOT NULL,
    ResponsableAsignado VARCHAR(60) NOT NULL,
    Estado VARCHAR(50) CHECK(Estado IN ('completado', 'en curso')) DEFAULT 'en curso',
    FechaInicio DATE NOT NULL,
    FechaFin DATE CHECK(FechaFin >= FechaInicio), --Puede ser NULL
    DuracionEstimada INT NOT NULL, --Duración estimada en minutos
    DuracionReal INTERVAL, --Duración real calculada a partir de las fechas de inicio y fin
    CONSTRAINT fk_paso_tramite FOREIGN KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) REFERENCES Tramite(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_paso_actividad PRIMARY KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial)
);

CREATE TABLE EstadoCuenta(
    CI INT NOT NULL,
    IDPrestadora INT NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaCreacion TIMESTAMP NOT NULL,
    MesAñoApertura DATE NOT NULL,
    EstadoFiscal VARCHAR(50) CHECK(EstadoFiscal IN ('abierto', 'cerrado')) DEFAULT 'abierto',
    FechaCierre DATE, --Puede ser NULL
    CONSTRAINT fk_estado_cuenta_tramite FOREIGN KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) REFERENCES Tramite(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_estado_cuenta PRIMARY KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura)
);

CREATE TABLE LineaCargo(
    CI INT NOT NULL,
    IDPrestadora INT NOT NULL,
    NombreCategoria VARCHAR(50) NOT NULL,
    Descripcion TEXT NOT NULL,
    FechaCreacion TIMESTAMP NOT NULL,
    MesAñoApertura DATE NOT NULL,
    NroLinea INT NOT NULL,
    -- Consolida
    FechaCargo TIMESTAMP NOT NULL,
    Cantidad INT NOT NULL,
    Concepto TEXT NOT NULL,
    PrecioUnitario REAL NOT NULL,
    ImpuestosLey REAL NOT NULL,
    CONSTRAINT fk_linea_estado FOREIGN KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) REFERENCES EstadoCuenta(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_linea_cargo PRIMARY KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea)
);

CREATE TABLE Comprador(
    IDComprador INT PRIMARY KEY
);

CREATE TABLE Factura(
    Numero INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    Estatus VARCHAR(50) CHECK(Estatus IN ('pagada', 'pendiente')) DEFAULT 'pendiente',
    FechaHoraEmision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    Deuda REAL NOT NULL, --Se calcula automáticamente
    FechaHoraPago TIMESTAMP, --Puede ser NULL
    MontoAcumulado REAL NOT NULL DEFAULT 0,
    IDComprador INT NOT NULL,
    CI INT,
    IDPrestadora INT,
    NombreCategoria VARCHAR(50),
    Descripcion TEXT,
    FechaCreacion TIMESTAMP,
    MesAñoApertura DATE,
    CONSTRAINT fK_factura_estado FOREIGN KEY(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) REFERENCES EstadoCuenta(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_factura_comprador FOREIGN KEY(IDComprador) REFERENCES Comprador(IDComprador) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Tasa(
    FechaHoraVigencia TIMESTAMP DEFAULT CURRENT_TIMESTAMP PRIMARY KEY,
    CambioReal REAL NOT NULL,
    FechaHoraFin TIMESTAMP CHECK(FechaHoraFin >= FechaHoraVigencia)
);

CREATE TABLE MetodoPago(
    IDMetodo INT PRIMARY KEY,
    FechaHoraVigencia TIMESTAMP NOT NULL, -- Lo toma automáticamente el sistema
    MontoRecibido REAL NOT NULL,
    CONSTRAINT fk_tasa_metodo FOREIGN KEY(FechaHoraVigencia) REFERENCES Tasa(FechaHoraVigencia) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Paga(
    IDMetodo INT,
    IDComprador INT NOT NULL, -- Lo toma automáticamente el sistema
    NumeroFactura INT NOT NULL,
    CONSTRAINT fk_paga_metodo FOREIGN KEY(IDMetodo) REFERENCES MetodoPago(IDMetodo) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_paga_comprador FOREIGN KEY(IDComprador) REFERENCES Comprador(IDComprador) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_paga_factura FOREIGN KEY(NumeroFactura) REFERENCES Factura(Numero) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_paga PRIMARY KEY(IDMetodo)
);

CREATE TABLE TransaccionEnLinea(
    IDLinea INT PRIMARY KEY,
    IDMetodo INT NOT NULL,
    CONSTRAINT fk_linea_metodo FOREIGN KEY(IDMetodo) REFERENCES MetodoPago(IDMetodo) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE PagoPresencial(
    IDPresencial INT PRIMARY KEY,
    IDMetodo INT NOT NULL,
    CONSTRAINT fk_presencial_metodo FOREIGN KEY(IDMetodo) REFERENCES MetodoPago(IDMetodo) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE Criptomoneda(
    TXID VARCHAR(64) PRIMARY KEY,
    IDLinea INT,
    Red VARCHAR(50) NOT NULL,
    DireccionBilletera TEXT NOT NULL,
    TasaConversion REAL NOT NULL,
    CONSTRAINT fk_cripto_linea FOREIGN KEY(IDLinea) REFERENCES TransaccionEnLinea(IDLinea) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE Zelle(
    CodigoConfirmacion INT PRIMARY KEY,
    IDLinea INT,
    CorreoElectronico VARCHAR(50) NOT NULL,
    PrimerNombre VARCHAR(50) NOT NULL,
    PrimerApellido VARCHAR(50) NOT NULL,
    SegundoNombre VARCHAR(50), --Puede ser NULL
    SegundoApellido VARCHAR(50), --Puede ser NULL
    CONSTRAINT fk_zelle_linea FOREIGN KEY(IDLinea) REFERENCES TransaccionEnLinea(IDLinea) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE Tarjeta(
    FechaHoraPago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    NroTarjeta BIGINT,
    CompañiaEmisora VARCHAR(50) NOT NULL,
    MonedaLiquidacion VARCHAR(50) NOT NULL,
    TipoRed VARCHAR(50) NOT NULL CHECK(TipoRed IN ('nacional', 'internacional')),
    FechaVencimiento DATE NOT NULL,
    IDPresencial INT,
    CONSTRAINT fk_tarjeta_presencial FOREIGN KEY(IDPresencial) REFERENCES PagoPresencial(IDPresencial) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT pk_tarjeta PRIMARY KEY(FechaHoraPago, NroTarjeta)
);

CREATE TABLE PagoMovil(
    NroReferencia BIGINT PRIMARY KEY,
    FechaMovimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    TelefonoEmisor VARCHAR(20) NOT NULL,
    Banco VARCHAR(50) NOT NULL,
    IDPresencial INT,
    CONSTRAINT fk_movil_presencial FOREIGN KEY(IDPresencial) REFERENCES PagoPresencial(IDPresencial) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE TAI(
    POS BIGINT,
    FechaHoraPago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UID BIGINT,
    IDPresencial INT,
    CONSTRAINT fk_tai_presencial FOREIGN KEY(IDPresencial) REFERENCES PagoPresencial(IDPresencial) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT pk_tai PRIMARY KEY(POS, FechaHoraPago)
);

CREATE TABLE Efectivo(
    FechaHoraPago TIMESTAMP DEFAULT CURRENT_TIMESTAMP PRIMARY KEY,
    MonedaDeCurso VARCHAR(50) NOT NULL CHECK(MonedaDeCurso IN ('bolivares', 'divisas')),
    IDPresencial INT,
    CONSTRAINT fk_efectivo_presencial FOREIGN KEY(IDPresencial) REFERENCES PagoPresencial(IDPresencial) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE DesgloseDenominaciones(
    FechaHoraPago TIMESTAMP,
    Cantidad INT,
    Valor REAL,
    CONSTRAINT fk_desglose_efectivo FOREIGN KEY(FechaHoraPago) REFERENCES Efectivo(FechaHoraPago) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT pk_desglose PRIMARY KEY(FechaHoraPago, Cantidad, Valor)
);

ALTER TABLE Miembro
ADD IDComprador INT;

ALTER TABLE Miembro
ADD CONSTRAINT fk_miembro_comprador FOREIGN KEY(IDComprador) REFERENCES Comprador(IDComprador) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE OrganizacionExterna
ADD IDComprador INT;

ALTER TABLE OrganizacionExterna
ADD CONSTRAINT fk_organizacion_comprador FOREIGN KEY(IDComprador) REFERENCES Comprador(IDComprador) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE OR REPLACE VIEW PagaFactura AS
SELECT IDMetodo, NumeroFactura FROM Paga;

-- ! Restricciones mínimo 1.
-- TODO: Asiste (Curso - Estudiante - Profesor)
CREATE OR REPLACE FUNCTION fn_verificar_asistencia_minima()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    -- Evaluamos dependiendo de qué tabla disparó el trigger
    IF LOWER(TG_TABLE_NAME) = 'estudiante' THEN
        v_existe := EXISTS (SELECT 1 FROM Asiste WHERE CIEstudiante = NEW.CI AND FechaInicio = NEW.FechaInicio);
        IF NOT v_existe THEN
            RAISE EXCEPTION 'Restricción (1,n) violada: El Estudiante con CI % debe estar asociado con al menos un curso en Asiste.', NEW.CI;
        END IF;
        
    ELSIF LOWER(TG_TABLE_NAME) = 'profesor' THEN
        v_existe := EXISTS (SELECT 1 FROM Asiste WHERE CIProfesor = NEW.CI AND FechaInicio = NEW.FechaInicio);
        IF NOT v_existe THEN
            RAISE EXCEPTION 'Restricción (1,n) violada: El Profesor con CI % debe estar asociado con al menos un curso en Asiste.', NEW.CI;
        END IF;
        
    ELSIF LOWER(TG_TABLE_NAME) = 'curso' THEN
        v_existe := EXISTS (SELECT 1 FROM Asiste WHERE Materia = NEW.Materia AND FechaInicio = NEW.FechaInicio);
        IF NOT v_existe THEN
            RAISE EXCEPTION 'Restricción (1,n) violada: El Curso con materia % debe tener asignado al menos un Estudiante y un Profesor en Asiste.', NEW.Materia;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- * Trigger para Estudiante
CREATE CONSTRAINT TRIGGER trg_estudiante_asiste_minimo
AFTER INSERT OR UPDATE ON Estudiante
DEFERRABLE INITIALLY DEFERRED --Indica que es una condición que se ejecutará al final de la transacción, permitiendo crear las tablas primero
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_asistencia_minima();

-- * Trigger para Profesor
CREATE CONSTRAINT TRIGGER trg_profesor_asiste_minimo
AFTER INSERT OR UPDATE ON Profesor
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_asistencia_minima();

-- * Trigger para Curso
CREATE CONSTRAINT TRIGGER trg_curso_asiste_minimo
AFTER INSERT OR UPDATE ON Curso
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_asistencia_minima();

-- TODO: Registra (Beneficiario - Miembro)
CREATE OR REPLACE FUNCTION fn_verificar_registro_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    v_existe := EXISTS (SELECT 1 FROM Registra WHERE DocumentoIdentidad = NEW.DocumentoIdentidad);
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: El beneficiario con Documento Identidad % debe estar asociado con al menos un miembro en Registra.', NEW.DocumentoIdentidad;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_beneficiario_registro_minimo
AFTER INSERT OR UPDATE ON Beneficiario
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_registro_minimo();

-- TODO: Es registrado (Sesion - Miembro)
CREATE OR REPLACE FUNCTION fn_verificar_es_registrado_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    v_existe := EXISTS (SELECT 1 FROM Sesion WHERE CI = NEW.CI);
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: El Miembro con CI % debe estar asociado con al menos una sesión.', NEW.CI;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_miembro_es_registrado_minimo
AFTER INSERT OR UPDATE ON Miembro
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_es_registrado_minimo();

-- TODO: Se compone (Tramite - PasoActividad)
CREATE OR REPLACE FUNCTION fn_verificar_se_compone_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    v_existe := EXISTS (
    SELECT 1 FROM PasoActividad 
    WHERE CI = NEW.CI
        AND IDPrestadora = NEW.IDPrestadora
        AND NombreCategoria = NEW.NombreCategoria
        AND Descripcion = NEW.Descripcion
        AND FechaCreacion = NEW.FechaCreacion
    );
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: El Trámite con CI %, IDPrestadora %, NombreCategoria %, Descripcion % y FechaCreacion % debe estar asociado con al menos un Paso Actividad.', NEW.CI, NEW.IDPrestadora, NEW.NombreCategoria, NEW.Descripcion, NEW.FechaCreacion;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_tramite_se_compone_minimo
AFTER INSERT OR UPDATE ON Tramite
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_se_compone_minimo();

-- TODO: Alberga (Edificacion - Sede)
CREATE OR REPLACE FUNCTION fn_verificar_alberga_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    v_existe := EXISTS (SELECT 1 FROM EspacioFisico WHERE Direccion = NEW.Direccion AND Nombre = NEW.Nombre);
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: La edificación con Direccion % y Nombre % debe estar asociado con al menos un espacio físico.', NEW.Direccion, NEW.Nombre;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_edificacion_alberga_minimo
AFTER INSERT OR UPDATE ON Edificacion
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_alberga_minimo();

-- TODO: Gestiona (Edificacion - Sede)
CREATE OR REPLACE FUNCTION fn_verificar_gestion_minima()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    v_existe := EXISTS (SELECT 1 FROM Edificacion WHERE Ubicacion = NEW.Ubicacion);
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: La sede con Ubicación % debe estar asociado con al menos una Edificación.', NEW.Ubicacion;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_sede_gestion_minima
AFTER INSERT ON Sede
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_gestion_minima();

-- TODO: Parametriza (Servicio - Tarifa)
CREATE OR REPLACE FUNCTION fn_verificar_parametriza_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
--NombreCategoria, IDPrestadora, Descripcion
    v_existe := EXISTS (
        SELECT 1 FROM Tarifa 
        WHERE NombreCategoria = NEW.NombreCategoria 
        AND IDPrestadora = NEW.IDPrestadora 
        AND Descripcion = NEW.Descripcion
        );
    IF NOT v_existe THEN
        RAISE EXCEPTION 'Restricción (1,n) violada: El Servicio de Categoria %, ID de Entidad Prestadora % y Descripcion % debe estar asociado con al menos una Tarifa.', NEW.NombreCategoria, NEW.IDPrestadora, NEW.Descripcion;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_servicio_parametriza_minimo
AFTER INSERT ON Servicio
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_parametriza_minimo();

-- TODO: Pertenece (Servicio - EntidadPrestadora - Categoria)
CREATE OR REPLACE FUNCTION fn_verificar_pertenece_minimo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    IF LOWER(TG_TABLE_NAME) = 'entidadprestadora' THEN
        v_existe := EXISTS (SELECT 1 FROM Servicio WHERE IDPrestadora = NEW.IDPrestadora);
        IF NOT v_existe THEN
            RAISE EXCEPTION 'Restricción (1,n) violada: La Entidad Prestadora con ID % debe estar asociado con al menos un Servicio.', NEW.IDPrestadora;
        END IF;
    ELSIF LOWER(TG_TABLE_NAME) = 'categoria' THEN
        v_existe := EXISTS (SELECT 1 FROM Servicio WHERE NombreCategoria = NEW.Nombre);
        IF NOT v_existe THEN
            RAISE EXCEPTION 'Restricción (1,n) violada: La Categoría de nombre % debe estar asociado con al menos un Servicio.', NEW.Nombre;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_prestadora_pertenece_minimo
AFTER INSERT ON EntidadPrestadora
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_pertenece_minimo();

CREATE CONSTRAINT TRIGGER trg_categoria_pertenece_minimo
AFTER INSERT ON Categoria
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_pertenece_minimo();

-- ! Función para crear automáticamente EstadoCuenta a trámite:
CREATE OR REPLACE FUNCTION fn_crear_estado_cuenta()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO EstadoCuenta(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura)
    VALUES(NEW.CI, NEW.IDPrestadora, NEW.NombreCategoria, NEW.Descripcion, NEW.FechaCreacion, CURRENT_DATE);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tramite_crea_cuenta
AFTER INSERT ON Tramite
FOR EACH ROW
EXECUTE FUNCTION fn_crear_estado_cuenta();

-- ! Restricciones de herencia:
-- TODO: Verificar que todo Rol deba tener herencia disjunta
CREATE OR REPLACE FUNCTION fn_verificar_herencia_rol()
RETURNS TRIGGER AS $$
DECLARE
    estudiante_existe BOOLEAN;
    empleado_existe BOOLEAN;
BEGIN
    estudiante_existe := EXISTS (SELECT 1 FROM Estudiante WHERE CI = NEW.CI AND FechaInicio = NEW.FechaInicio);
    empleado_existe := EXISTS (SELECT 1 FROM Empleado WHERE CI = NEW.CI AND FechaInicio = NEW.FechaInicio);
    IF empleado_existe AND estudiante_existe THEN
        RAISE EXCEPTION 'Restricción herencia disjunta violada: No puede haber dos entidades distintas heredando del mismo Rol (%, %)', NEW.CI, NEW.FechaInicio;
    ELSIF NOT empleado_existe AND NOT estudiante_existe THEN
        RAISE EXCEPTION 'Restricción herencia obligatoria violada: No puede existir un Rol que no posea herencia ninguna (%, %)', NEW.CI, NEW.FechaInicio;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_herencia_rol
AFTER INSERT OR UPDATE ON Rol
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_herencia_rol();

-- ! Restricciones de categoría:
-- TODO: Verificar que toda Entidad Prestadora este en una entidad padre de manera disjunta
CREATE OR REPLACE FUNCTION fn_verificar_categoria_prestadora()
RETURNS TRIGGER AS $$
DECLARE
    externa_existe BOOLEAN;
    interna_existe BOOLEAN;
BEGIN
    externa_existe := EXISTS (SELECT 1 FROM OrganizacionExterna WHERE IDPrestadora = NEW.IDPrestadora);
    interna_existe := EXISTS (SELECT 1 FROM EntidadInterna WHERE IDPrestadora = NEW.IDPrestadora);
    IF externa_existe AND interna_existe THEN
        RAISE EXCEPTION 'Restricción herencia disjunta violada: No puede haber dos entidades distintas que compartan la misma Entidad Prestadora (%)', NEW.IDPrestadora;
    ELSIF NOT externa_existe AND NOT interna_existe THEN
        RAISE EXCEPTION 'Restricción herencia obligatoria violada: No puede existir una Entidad Prestadora que no herede de ninguna entidad (%)', NEW.IDPrestadora;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_entidad_prestadora_herencia
AFTER INSERT OR UPDATE ON EntidadPrestadora
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_categoria_prestadora();

-- TODO: Verificar que todo Método Pago este en una entidad padre de manera disjunta
CREATE OR REPLACE FUNCTION fn_verificar_categoria_metodo_pago()
RETURNS TRIGGER AS $$
DECLARE
    linea_existe BOOLEAN;
    presencial_existe BOOLEAN;
BEGIN
    linea_existe := EXISTS (SELECT 1 FROM TransaccionEnLinea WHERE IDMetodo = NEW.IDMetodo);
    presencial_existe := EXISTS (SELECT 1 FROM PagoPresencial WHERE IDMetodo = NEW.IDMetodo);
    IF linea_existe AND presencial_existe THEN
        RAISE EXCEPTION 'Restricción herencia disjunta violada: No puede haber dos entidades distintas que compartan el mismo ID de Método de Pago (%)', NEW.IDMetodo;
    ELSIF NOT linea_existe AND NOT presencial_existe THEN
        RAISE EXCEPTION 'Restricción herencia obligatoria violada: No puede existir un Método de pago que no herede de ninguna entidad (%)', NEW.IDMetodo;
    END IF;

    

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_metodo_pago_herencia
AFTER INSERT OR UPDATE ON MetodoPago
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_categoria_metodo_pago();

-- TODO: Verificar que todo Método Pago este en una entidad padre de manera disjunta
CREATE OR REPLACE FUNCTION fn_verificar_categoria_linea()
RETURNS TRIGGER AS $$
DECLARE
    cripto_existe BOOLEAN;
    zelle_existe BOOLEAN;
BEGIN
    cripto_existe := EXISTS (SELECT 1 FROM Criptomoneda WHERE IDLinea = NEW.IDLinea);
    zelle_existe := EXISTS (SELECT 1 FROM Zelle WHERE IDLinea = NEW.IDLinea);
    IF cripto_existe AND zelle_existe THEN
        RAISE EXCEPTION 'Restricción herencia disjunta violada: No puede haber dos entidades distintas que compartan el mismo ID de Transacción en línea (%)', NEW.IDLinea;
    ELSIF NOT cripto_existe AND NOT zelle_existe THEN
        RAISE EXCEPTION 'Restricción herencia obligatoria violada: No puede existir una Transacción en línea que no herede de ninguna entidad (%)', NEW.IDLinea;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_transaccion_linea_herencia
AFTER INSERT OR UPDATE ON TransaccionEnLinea
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_categoria_linea();

-- TODO: Verificar que todo Pago presencial este en una entidad padre de manera disjunta
CREATE OR REPLACE FUNCTION fn_verificar_categoria_presencial()
RETURNS TRIGGER AS $$
DECLARE
    v_coincidencias INT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM Tarjeta WHERE IDPresencial = NEW.IDPresencial) THEN
        v_coincidencias := v_coincidencias + 1;
    END IF;
    IF EXISTS (SELECT 1 FROM PagoMovil WHERE IDPresencial = NEW.IDPresencial) THEN
        v_coincidencias := v_coincidencias + 1;
    END IF;
    IF EXISTS (SELECT 1 FROM TAI WHERE IDPresencial = NEW.IDPresencial) THEN
        v_coincidencias := v_coincidencias + 1;
    END IF;
    IF EXISTS (SELECT 1 FROM Efectivo WHERE IDPresencial = NEW.IDPresencial) THEN
        v_coincidencias := v_coincidencias + 1;
    END IF;
    -- Violación de Obligatoriedad: No apareció en ninguna tabla (Suma = 0)
    IF v_coincidencias = 0 THEN
        RAISE EXCEPTION 'Restricción obligatoria violada: El Pago Presencial (%) debe heredar de al menos un padre.', NEW.IDPresencial;
    -- Violación de Disyunción: Apareció en más de una tabla a la vez (Suma > 1)
    ELSIF v_coincidencias > 1 THEN
        RAISE EXCEPTION 'Restricción disjunta violada: El Pago Presencial (%) no puede pertenecer a múltiples categorías padre simultáneamente.', NEW.IDPresencial;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_pago_presencial_herencia
AFTER INSERT OR UPDATE ON PagoPresencial
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_categoria_presencial();

-- TODO: Restricción 5
CREATE OR REPLACE FUNCTION fn_miembro_es_empleado()
RETURNS TRIGGER AS $$
DECLARE
    FechaInicio_registrado DATE;
    es_empleado BOOLEAN;
BEGIN
    -- Se busca el rol actual
    SELECT FechaInicio INTO FechaInicio_registrado
    FROM Rol
    WHERE FechaInicio <= CURRENT_DATE
    AND CI = NEW.CI
    ORDER BY FechaInicio DESC
    LIMIT 1;
    -- Se verifica si el rol actual es empleado
    es_empleado := EXISTS (SELECT 1 FROM Empleado WHERE CI = NEW.CI AND FechaInicio = FechaInicio_registrado);
    IF NOT es_empleado THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Los Beneficiarios solo pueden relacionarse con miembros que sean empleados de la institución (Error de %)', NEW.DocumentoIdentidad;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_registra
BEFORE UPDATE OR INSERT ON Registra
FOR EACH ROW
EXECUTE FUNCTION fn_miembro_es_empleado();

-- TODO: Restricción 7, 24, 30 y 31
--* Restricción extra: Para todo Tramite, cuando estado pase a ser finalizado entonces FechaCierre ≠ NULL.
CREATE OR REPLACE FUNCTION fn_calcular_tiempo_resolucion()
RETURNS TRIGGER AS $$
DECLARE
    v_rol_activo BOOLEAN;
BEGIN
    -- Verifica si el miembro con el que se vincula posee un rol actualmente
    v_rol_activo := EXISTS(SELECT 1 FROM Rol WHERE CI = NEW.CI AND FechaFin IS NULL);
    IF NOT v_rol_activo THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Miembro % no puede crear trámites si no posee rol en el momento de creación', NEW.CI;
    END IF;

    IF NEW.Estado = 'finalizado' THEN
        NEW.FechaCierre := NOW();

        UPDATE Acompañante
        SET Estado = 'archivado'
        WHERE CIMiembro = NEW.CI
        AND IDPrestadora = NEW.IDPrestadora
        AND NombreCategoria = NEW.NombreCategoria
        AND Descripcion = NEW.Descripcion
        AND FechaCreacion = NEW.FechaCreacion;
    ELSE
        UPDATE Acompañante
        SET Estado = 'activo'
        WHERE CIMiembro = NEW.CI
        AND IDPrestadora = NEW.IDPrestadora
        AND NombreCategoria = NEW.NombreCategoria
        AND Descripcion = NEW.Descripcion
        AND FechaCreacion = NEW.FechaCreacion;
    END IF;

    IF NEW.FechaCierre IS NULL THEN
        NEW.TiempoResolucion := NULL;
    ELSE
        -- Calculamos los días laborables transcurridos
        SELECT COUNT(*) * INTERVAL '1 day'
        INTO NEW.TiempoResolucion
        FROM generate_series(NEW.FechaCreacion + INTERVAL '1 day', NEW.FechaCierre, '1 day'::interval) AS d
        WHERE EXTRACT(ISODOW FROM d) NOT IN (6, 7) -- Excluye Sábado (6) y Domingo (7)
        AND d::date NOT IN (SELECT Fecha FROM feriado); -- Excluye los feriados
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_tramite
BEFORE INSERT OR UPDATE ON Tramite
FOR EACH ROW
EXECUTE FUNCTION fn_calcular_tiempo_resolucion();

-- TODO: Restricción 11 y 12
CREATE OR REPLACE FUNCTION fn_calcular_duracion_real()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.Estado = 'completado' THEN
        NEW.FechaFin := NOW();
    END IF;
    IF NEW.FechaFin IS NULL THEN
        NEW.DuracionReal := NULL;
    ELSE
        -- Calculamos los días laborables transcurridos
        SELECT COUNT(*) * INTERVAL '1 day'
        INTO NEW.DuracionReal
        FROM generate_series(NEW.FechaInicio + 1, NEW.FechaFin, '1 day'::interval) AS d
        WHERE EXTRACT(ISODOW FROM d) NOT IN (6, 7) -- Excluye Sábado (6) y Domingo (7)
        AND d::date NOT IN (SELECT Fecha FROM Feriado); -- Excluye los feriados
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_paso_actividad
BEFORE INSERT OR UPDATE ON PasoActividad
FOR EACH ROW
EXECUTE FUNCTION fn_calcular_duracion_real();

-- TODO: Restricción 14
CREATE OR REPLACE FUNCTION fn_mismo_comprador_factura()
RETURNS TRIGGER AS $$
DECLARE
    v_id_comprador INT;
BEGIN
    -- Buscamos el comprador real directamente desde la Factura
    SELECT IDComprador INTO v_id_comprador
    FROM Factura
    WHERE Numero = NEW.NumeroFactura;

    IF v_id_comprador IS NULL THEN
        RAISE EXCEPTION 'La factura número % no existe.', NEW.NumeroFactura;
    END IF;

    -- El trigger se encarga de hacer el INSERT real en la tabla física con los 3 datos
    INSERT INTO Paga(IDMetodo, NumeroFactura, IDComprador)
    VALUES (NEW.IDMetodo, NEW.NumeroFactura, v_id_comprador);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_paga
INSTEAD OF UPDATE OR INSERT ON PagaFactura
FOR EACH ROW
EXECUTE FUNCTION fn_mismo_comprador_factura();

-- TODO: Restricción 16, 17, 18, 19 & 40
CREATE OR REPLACE FUNCTION fn_revisar_factura()
RETURNS TRIGGER AS $$
DECLARE
    cuenta_estado_fiscal VARCHAR(50);
    deuda_total INT;
    v_comprador_miembro BOOLEAN;
    v_comprador_mismo_factura BOOLEAN;
BEGIN
    --LÓGICA 1: Validación de Relación Factura con Estado de cuenta cerrada.
    SELECT EstadoFiscal 
    INTO cuenta_estado_fiscal 
    FROM EstadoCuenta
    WHERE CI = NEW.CI
    AND IDPrestadora = NEW.IDPrestadora
    AND NombreCategoria = NEW.NombreCategoria
    AND Descripcion = NEW.Descripcion
    AND FechaCreacion = NEW.FechaCreacion
    AND MesAñoApertura = NEW.MesAñoApertura;

    IF cuenta_estado_fiscal <> 'cerrado' THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Factura % debe relacionarse con Estado de Cuenta cerrado', NEW.Numero;
    END IF;
    -- Se obtiene la deuda total de las línea cargo que se relacionan con Factura.
    SELECT SUM((PrecioUnitario + ImpuestosLey) * Cantidad)
    INTO deuda_total
    FROM LineaCargo
    WHERE CI = NEW.CI
    AND IDPrestadora = NEW.IDPrestadora
    AND NombreCategoria = NEW.NombreCategoria
    AND Descripcion = NEW.Descripcion
    AND FechaCreacion = NEW.FechaCreacion
    AND MesAñoApertura = NEW.MesAñoApertura;

    NEW.Deuda := COALESCE(deuda_total, 0); --Si no hay deuda, entonces la deuda será 0.


    -- LÓGICA 2: Validación del Comprador Miembro
    v_comprador_miembro := EXISTS(SELECT 1 FROM Miembro WHERE IDComprador = NEW.IDComprador);
    v_comprador_mismo_factura := EXISTS(SELECT 1 FROM Miembro WHERE CI = NEW.CI AND IDComprador = NEW.IDComprador);
    IF v_comprador_miembro AND NOT v_comprador_mismo_factura THEN
        RAISE EXCEPTION 'Restricción de negocio violada: La factura % posee un Comprador Miembro que no es el mismo que la generó', NEW.Numero;
    END IF;

    -- LÓGICA 3: Evaluación de Montos, Estatus y Fecha de Pago
    IF NEW.MontoAcumulado < NEW.Deuda THEN
        NEW.Estatus := 'pendiente';
    ELSE
        NEW.Estatus := 'pagada';
    END IF;
    IF NEW.Estatus = 'pagada' THEN
        NEW.FechaHoraPago := NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_monto_mayor_a_deuda_factura
BEFORE UPDATE OR INSERT ON Factura
FOR EACH ROW
EXECUTE FUNCTION fn_revisar_factura();

-- TODO: Restricción 21 y 22
CREATE OR REPLACE FUNCTION fn_verificar_edad()
RETURNS TRIGGER AS $$
DECLARE
    edad INT;
BEGIN
    edad := DATE_PART('year', AGE(NEW.FechaNacimiento));
    IF edad < 18 THEN
        NEW.CertificadoSolteria := NULL;
        NEW.ConstanciaEstudiosUniversitarios := NULL;
        RAISE NOTICE 'Beneficiario % es menor de edad, por lo que certificado de soltería y constancia de estudios serán NULL', NEW.DocumentoIdentidad;
    ELSE
        NEW.EsquemaVacunacion := NULL;
        NEW.CentroEducativoInicial := NULL;
        RAISE NOTICE 'Beneficiario % es mayor de edad, por lo que esquema de vacunación y centro educativo inicial serán NULL', NEW.DocumentoIdentidad;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_beneficiario
BEFORE UPDATE OR INSERT ON Beneficiario
FOR EACH ROW
EXECUTE FUNCTION fn_verificar_edad();

-- TODO: Restricción 23
-- ? Eso de Restricción de edad límite sin constancia de estudios universitarios no entendí cómo colocarlos.
CREATE OR REPLACE FUNCTION fn_fin_beneficiario()
RETURNS TRIGGER AS $$
DECLARE
    vinculo_existe BOOLEAN;
BEGIN
    vinculo_existe := EXISTS(SELECT 1 FROM Registra WHERE DocumentoIdentidad = NEW.DocumentoIdentidad);
    IF NOT vinculo_existe THEN
        NEW.FechaFin := NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_fin_beneficiario
BEFORE UPDATE ON Beneficiario
FOR EACH ROW
EXECUTE FUNCTION fn_fin_beneficiario();

-- TODO: Restricción 26
CREATE OR REPLACE FUNCTION fn_choque_horarios()
RETURNS TRIGGER AS $$
DECLARE
    interseccion_existe BOOLEAN;
BEGIN
    interseccion_existe := EXISTS(
        SELECT 1 FROM BloqueHorario 
        WHERE NEW.FechaHoraInicio <= FechaHoraFin 
        AND NEW.FechaHoraFin >= FechaHoraInicio
        AND NEW.NroIdentificador = NroIdentificador
        AND NEW.Direccion = Direccion
        AND NEW.Nombre = Nombre
        AND (TG_OP = 'INSERT' OR (FechaHoraInicio <> OLD.FechaHoraInicio OR FechaHoraFin <> OLD.FechaHoraFin))
        );
    IF interseccion_existe THEN
        RAISE EXCEPTION 'Restricción de negocio violada: El rango de fechas % - % intersecta con otra fecha previa', NEW.FechaHoraInicio, NEW.FechaHoraFin;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_bloque_horario
BEFORE UPDATE OR INSERT ON BloqueHorario
FOR EACH ROW
EXECUTE FUNCTION fn_choque_horarios();

-- TODO: Restricción 27
CREATE OR REPLACE FUNCTION fn_info_corresponde_titular()
RETURNS TRIGGER AS $$
DECLARE
    -- Solo declaramos un tipo récord (o fila) para guardar los datos específicos que necesitamos
    r_miembro RECORD;
BEGIN
    -- Unificamos los 3 SELECTs en un solo JOIN directo y limpio
    SELECT m.PrimerNombre, m.PrimerApellido, m.SegundoNombre, m.SegundoApellido, m.CorreoInstitucional
    INTO r_miembro
    FROM TransaccionEnLinea TL
    JOIN Paga p ON p.IDMetodo = TL.IDMetodo
    JOIN Miembro m ON m.IDComprador = p.IDComprador
    WHERE TL.IDLinea = NEW.IDLinea;

    -- FOUND es una variable interna de PostgreSQL que verifica si el SELECT anterior trajo datos
    IF FOUND THEN
        NEW.PrimerNombre        := r_miembro.PrimerNombre;
        NEW.PrimerApellido      := r_miembro.PrimerApellido;
        NEW.SegundoNombre       := r_miembro.SegundoNombre;
        NEW.SegundoApellido     := r_miembro.SegundoApellido;
        NEW.CorreoElectronico   := r_miembro.CorreoInstitucional;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_zelle
BEFORE UPDATE OR INSERT ON Zelle
FOR EACH ROW
EXECUTE FUNCTION fn_info_corresponde_titular();

-- TODO: Restricción 32
--* Restricción extra: Para toda Tasa, al insertarse una nueva tasa, la anterior registra la fecha fin ese mismo momento.
CREATE OR REPLACE FUNCTION fn_reemplazo_tasa()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE Tasa SET FechaHoraFin = NEW.FechaHoraVigencia
    WHERE FechaHoraFin IS NULL AND FechaHoraVigencia < NEW.FechaHoraVigencia;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_tasa
AFTER INSERT ON Tasa
FOR EACH ROW
EXECUTE FUNCTION fn_reemplazo_tasa();

CREATE OR REPLACE FUNCTION fn_tasa_mas_reciente()
RETURNS TRIGGER AS $$
BEGIN
    SELECT FechaHoraVigencia INTO NEW.FechaHoraVigencia FROM Tasa WHERE FechaHoraFin IS NULL;
    -- Validación de seguridad: Si no encontramos ninguna tasa activa
    IF NEW.FechaHoraVigencia IS NULL THEN
        RAISE EXCEPTION 'No se encontró ninguna tasa activa (vigente) en el sistema.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_metodo_pago
BEFORE UPDATE OR INSERT ON MetodoPago
FOR EACH ROW
EXECUTE FUNCTION fn_tasa_mas_reciente();

-- TODO: Restricción 35
CREATE OR REPLACE FUNCTION fn_ajuste_costo_final()
RETURNS TRIGGER AS $$
DECLARE
    valor_maximo INT;
    valor_minimo INT;
BEGIN
    SELECT Ajuste, CostoMaximo 
    INTO valor_minimo, valor_maximo
    FROM Servicio s JOIN Categoria c ON s.NombreCategoria = c.Nombre
    WHERE s.NombreCategoria = NEW.NombreCategoria 
    AND s.IDPrestadora = NEW.IDPrestadora
    AND s.Descripcion = NEW.Descripcion;

    IF NEW.CostoFinal > valor_maximo OR NEW.CostoFinal < valor_minimo THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Tarifa (%, %, %, %) debe poseer un costo final menor a % y mayor
         a %', NEW.NombreCategoria, NEW.IDPrestadora, NEW.Descripcion, NEW.FechaInicio, valor_maximo, valor_minimo;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_tarifa
BEFORE UPDATE OR INSERT ON Tarifa
FOR EACH ROW
EXECUTE FUNCTION fn_ajuste_costo_final();

-- TODO: Restricción 37, 38
CREATE OR REPLACE FUNCTION fn_cerrar_estado()
RETURNS TRIGGER AS $$
BEGIN
    IF NOW() >= (NEW.MesAñoApertura + INTERVAL '1 month') THEN
        NEW.EstadoFiscal := 'cerrado';
    END IF;
    IF NEW.EstadoFiscal = 'cerrado' THEN
        NEW.FechaCierre := NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_estado_cuenta
BEFORE UPDATE OR INSERT ON EstadoCuenta
FOR EACH ROW
EXECUTE FUNCTION fn_cerrar_estado();

-- TODO: Restricción 39 y automatizar toma de Mes
CREATE OR REPLACE FUNCTION fn_relacion_cuenta_cerrada()
RETURNS TRIGGER AS $$
DECLARE
    es_egresado BOOLEAN;
    perfil_usuario VARCHAR(50);
    cuenta_estado_fiscal VARCHAR(50);
    precio_tarifa REAL;
BEGIN

    SELECT EstadoFiscal 
    INTO cuenta_estado_fiscal 
    FROM EstadoCuenta
    WHERE CI = NEW.CI
    AND IDPrestadora = NEW.IDPrestadora
    AND NombreCategoria = NEW.NombreCategoria
    AND Descripcion = NEW.Descripcion
    AND FechaCreacion = NEW.FechaCreacion
    AND MesAñoApertura = NEW.MesAñoApertura;
    IF cuenta_estado_fiscal = 'cerrado' THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Linea cargo (%, %, %, %, %, %, %) no puede relacionarse con Estado de cuenta ya cerrado', 
        NEW.CI,
        NEW.IDPrestadora,
        NEW.NombreCategoria,
        NEW.Descripcion,
        NEW.FechaCreacion,
        NEW.MesAñoApertura,
        NEW.NroLinea;
    END IF;

    es_egresado := EXISTS (SELECT 1 FROM Egresado WHERE CI = NEW.CI);
    IF es_egresado THEN
        perfil_usuario := 'egresado';
    ELSE 
        perfil_usuario := 'miembro activo';
    END IF;

    -- Automatizar precio unitario:
    SELECT CostoFinal INTO precio_tarifa
    FROM Tarifa
    WHERE Descripcion = NEW.Concepto
    AND NombreCategoria = NEW.NombreCategoria
    AND IDPrestadora = NEW.IDPrestadora
    AND Perfil = perfil_usuario
    AND FechaFin IS NULL;
    IF precio_tarifa IS NULL THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Concepto de Linea cargo (%, %, %, %, %, %, %) no se relaciona con ningún servicio con Tarifa activa', 
        NEW.CI,
        NEW.IDPrestadora,
        NEW.NombreCategoria,
        NEW.Descripcion,
        NEW.FechaCreacion,
        NEW.MesAñoApertura,
        NEW.NroLinea;
    ELSE 
        NEW.PrecioUnitario := precio_tarifa;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_linea_cargo
BEFORE UPDATE OR INSERT ON LineaCargo
FOR EACH ROW
EXECUTE FUNCTION fn_relacion_cuenta_cerrada();

-- TODO: Restricción 43
CREATE OR REPLACE FUNCTION fn_disponibilidad_carro()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.TipoVehiculo = 'carro' AND NEW.Capacidad > 4 THEN
        RAISE EXCEPTION 'Restricción de negocio violada: Transporte de placa % y carnet % no puede tener una Capacidad mayor a 4 por ser de tipo carro', NEW.Placa, NEW.CarnetDeConducir;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_medio_transporte
BEFORE UPDATE OR INSERT ON MedioTransporte
FOR EACH ROW
EXECUTE FUNCTION fn_disponibilidad_carro();

-- TODO: Restricción 44
CREATE OR REPLACE FUNCTION fn_disponibilidad_transporte()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.FechaHoraFin IS NULL THEN
        UPDATE MedioTransporte SET Disponibilidad = FALSE
        WHERE Placa = NEW.Placa
        AND CarnetDeConducir = NEW.CarnetDeConducir;
    ELSE
        UPDATE MedioTransporte SET Disponibilidad = TRUE
        WHERE Placa = NEW.Placa
        AND CarnetDeConducir = NEW.CarnetDeConducir;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_viaje
AFTER UPDATE OR INSERT ON Viaje
FOR EACH ROW
EXECUTE FUNCTION fn_disponibilidad_transporte();

-- TODO: Automatizar última conexión miembro y automatizar cambio fecha de cambio de contraseña.
CREATE OR REPLACE FUNCTION fn_ultima_conexion_miembro()
RETURNS TRIGGER AS $$
DECLARE
    ultima_sesion DATE;
BEGIN
    -- =========================================================================
    -- LÓGICA 1: Automatizar fecha de cambio de contraseña
    -- =========================================================================
    IF (TG_OP = 'INSERT' AND NEW.Contraseña IS NOT NULL) OR 
       (TG_OP = 'UPDATE' AND OLD.Contraseña IS DISTINCT FROM NEW.Contraseña) THEN
        NEW.FechaCambioContraseña := CURRENT_TIMESTAMP;
    END IF;

    -- =========================================================================
    -- LÓGICA 2: Obtener última conexión del miembro
    -- =========================================================================
    SELECT FechaFin INTO ultima_sesion FROM Sesion
    WHERE NEW.CI = CI
    AND FechaInicio <= CURRENT_DATE
    ORDER BY FechaInicio DESC
    LIMIT 1;

    IF ultima_sesion IS NULL THEN
        NEW.UltimaConexion := CURRENT_DATE;
    ELSE
        NEW.UltimaConexion := ultima_sesion;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_miembro
BEFORE UPDATE OR INSERT ON Miembro
FOR EACH ROW
EXECUTE FUNCTION fn_ultima_conexion_miembro();


-- TODO: Actualizar MontoAcumulado de Factura al crear Paga:
CREATE OR REPLACE FUNCTION fn_actualizar_monto_acumulado_factura()
RETURNS TRIGGER AS $$
DECLARE
    v_total_pagado NUMERIC;
    v_numero_factura INT;
BEGIN
    -- Identificamos qué factura recibió el movimiento (funciona en INSERT, UPDATE o DELETE)
    IF TG_OP = 'DELETE' THEN
        v_numero_factura := OLD.NumeroFactura;
    ELSE
        v_numero_factura := NEW.NumeroFactura;
    END IF;

    -- 1. Sumamos todos los pagos actuales que tiene esa factura en la tabla Paga
    SELECT COALESCE(SUM(MontoRecibido), 0)
    INTO v_total_pagado
    FROM Paga NATURAL JOIN MetodoPago
    WHERE NumeroFactura = v_numero_factura;

    -- 2. Impactamos directamente el campo de la tabla Factura
    UPDATE Factura
    SET MontoAcumulado = v_total_pagado
    WHERE Numero = v_numero_factura;
    RETURN NULL; -- Al ser un trigger AFTER, el retorno puede ser NULL
END;
$$ LANGUAGE plpgsql;

-- Trigger asociado a la tabla PAGA
CREATE TRIGGER trg_after_paga_balance
AFTER INSERT OR UPDATE OR DELETE ON Paga
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_monto_acumulado_factura();

BEGIN;
    INSERT INTO Miembro(
        CI,
        PrimerNombre,
        PrimerApellido,
        SegundoNombre,
        SegundoApellido,
        Sexo,
        CorreoInstitucional,
        DireccionHabitacion,
        FechaNacimiento,
        Telefono,
        Contraseña
    ) VALUES (
        30849378,
        'Javier',
        'Díaz',
        'Eduardo',
        'Ramos',
        'M',
        'jediaz.24@est.ucab.edu.ve',
        'Caracas, Santa Fe Sur',
        TO_DATE('29/10/2005', 'DD/MM/YYYY'),
        '0416-6078122',
        '123'
    ), (
        32000475,
        'Eduardo',
        'Alejandro',
        'Perez',
        'López',
        'M',
        'alej@mail.com',
        'Colombia, Bógota',
        TO_DATE('12/05/2007', 'DD/MM/YYYY'),
        '0416-5107782',
        '32Lp'
    ), (
        31012721,
        'Annabela',
        'Alejandra',
        'Morena',
        'Rivas',
        'F',
        'amgarcia.24@est.ucab.edu.ve',
        'Caracas, La Castellana',
        TO_DATE('20/02/2003', 'DD/MM/YYYY'),
        '0426-40102367',
        'Er453'
    ), (
        29071218,
        'Susana',
        'Luisa',
        'Rojas',
        'Jimenez',
        'F',
        'slrojas.24@est.ucab.edu.ve',
        'Caracas, Palos Grandes',
        TO_DATE('02/10/2000', 'DD/MM/YYYY'),
        '0416-12435043',
        'q@if2m_pl23'
    );

    INSERT INTO Sesion (
        CI,
        FechaInicio,
        Geolocalizacion,
        UUID,
        ConteoIntentosFallidos
    )
    VALUES (
        30849378,
        CURRENT_DATE,
        'Caracas, Santa Fe Sur',
        1659728723,
        0
    ), (32000475,
        TO_DATE('06/06/2026', 'DD/MM/YYYY'),
        'Colombia, Bógota',
        564246789,
        1
    ), (31012721,
        TO_DATE('04/07/2026', 'DD/MM/YYYY'),
        'Caracas, Chacaito',
        395437193,
        2
    ), (29071218,
        TO_DATE('12/03/2026', 'DD/MM/YYYY'),
        'Caracas, Palos Grandes',
        184475162,
        0
    );
    
    INSERT INTO Sede(Ubicacion)
    VALUES('Montalbán'), ('Guayana');

    INSERT INTO Edificacion(Direccion, Nombre, Ubicacion)
    VALUES('Feria', 'Cincuentenario', 'Montalbán'), ('Feria', 'Laboratorios', 'Guayana');

    INSERT INTO EspacioFisico(NroIdentificador, Direccion, Nombre, CapacidadMaxima, TipoDeMobiliario)
    VALUES(101, 'Feria', 'Cincuentenario', 20, 'Sillas, pupitres y pizarra'), 
    (101, 'Feria', 'Laboratorios', 25, 'Sillas, pupitres y pizarra');
COMMIT;

BEGIN;
    INSERT INTO BloqueHorario(NroIdentificador, Direccion, Nombre, FechaHoraInicio, FechaHoraFin)
    VALUES(101, 'Feria', 'Cincuentenario', TIMESTAMP '2026-10-07 7:00:00', TIMESTAMP '2026-10-07 10:00:00');

    INSERT INTO BloqueHorario(NroIdentificador, Direccion, Nombre, FechaHoraInicio, FechaHoraFin)
    VALUES(101, 'Feria', 'Laboratorios', TIMESTAMP '2026-10-07 8:00:00', TIMESTAMP '2026-10-07 10:00:00');

    INSERT INTO RecursosTecnologicos(NroIdentificador, Direccion, Nombre, Recurso)
    VALUES(101, 'Feria', 'Cincuentenario', 'Proyector');

    INSERT INTO RecursosTecnologicos(NroIdentificador, Direccion, Nombre, Recurso, Estado)
    VALUES(101, 'Feria', 'Cincuentenario', 'Ventilación', FALSE);
COMMIT;

BEGIN;
    INSERT INTO Categoria(Nombre, CostoMaximo)
    VALUES('Deporte', 60), ('Cultura', 80);

    INSERT INTO EntidadPrestadora(IDPrestadora)
    VALUES(1), (2);

    INSERT INTO EntidadInterna(CodigoPresupuestario, DirectorOficina, IDPrestadora)
    VALUES(100, 'José Manuel', 1);

    INSERT INTO OrganizacionExterna(RIF, FechaVencimientoContrato, RazonSocial, IDPrestadora)
    VALUES('J-12345678-0', TO_DATE('01/08/2030', 'DD/MM/YYYY'), 'Distribuidora de Peliculas', 2);

    INSERT INTO Servicio(NombreCategoria, IDPrestadora, Descripcion, PrecioBase, Ajuste, Ubicacion)
    VALUES
        ('Cultura', 2, 'Proyección película "Mi novia Otaku" el martes de 1:00 a 3:00', 15, 5, 'Montalbán'),
        ('Deporte', 1, 'Suscripción a Gimnasio', 10, 8, 'Guayana'),
        ('Deporte', 1, 'Bebida energética del Gimnasio', 8, 5, 'Guayana');
    
    INSERT INTO Tarifa(NombreCategoria, IDPrestadora, Descripcion, FechaInicio, Perfil, CostoFinal)
    VALUES
        ('Cultura', 2, 'Proyección película "Mi novia Otaku" el martes de 1:00 a 3:00', TO_DATE('04/07/2026', 'DD/MM/YYYY'), 'miembro activo', 10),
        ('Deporte', 1, 'Suscripción a Gimnasio', TO_DATE('07/04/2026', 'DD/MM/YYYY'), 'público externo', 20),
        ('Deporte', 1, 'Suscripción a Gimnasio', TO_DATE('07/04/2026', 'DD/MM/YYYY'), 'miembro activo', 15),
        ('Deporte', 1, 'Suscripción a Gimnasio', TO_DATE('07/04/2026', 'DD/MM/YYYY'), 'egresado', 10),
        ('Deporte', 1, 'Bebida energética del Gimnasio', TO_DATE('10/04/2026', 'DD/MM/YYYY'), 'miembro activo', 10);

    INSERT INTO Requisito(Acreditacion)
    VALUES('Documento Digital de usuario');

    INSERT INTO Exige(Acreditacion, NombreCategoria, IDPrestadora, Descripcion)
    VALUES('Documento Digital de usuario', 'Deporte', 1, 'Suscripción a Gimnasio');
COMMIT;

BEGIN;
    INSERT INTO Rol(CI, FechaInicio)
    VALUES(31012721, TO_DATE('10/09/2022', 'DD/MM/YYYY')),
    (29071218, TO_DATE('10/09/2022', 'DD/MM/YYYY')),
    (30849378, TO_DATE('10/09/2022', 'DD/MM/YYYY')),
    (32000475, TO_DATE('10/09/2022', 'DD/MM/YYYY'));

    INSERT INTO Empleado(CI, FechaInicio, CargaHorariaSemanal)
    VALUES(31012721, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 12),
    (29071218, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 15);

    INSERT INTO Profesor(CI, FechaInicio, EscalafonDocente, CodigoInvestigador)
    VALUES(31012721, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 'Instructor', 'bfbe767a16');

    INSERT INTO PersonalAdministrativo(CI, FechaInicio, UnidadAdscripcionPresupuestaria, CargoAdministrativo)
    VALUES(29071218, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 'Facultad de Ingeniería', 'Coordinador');

    INSERT INTO Estudiante(CI, FechaInicio, Semestre, Escuela, UnidadesCreditoAprobadas, PromedioPonderado, FacultadAdscripcion)
    VALUES(30849378, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 6, 'Ingeniería', 30, 15, 'División de Control de Estudios'),
    (32000475, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 3, 'Industrial', 28, 12, 'División de Control de Estudios');

    INSERT INTO Becario(CI, FechaInicio, TipoBeca)
    VALUES(30849378, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 'ayuda económica');

    INSERT INTO Preparador(CI, FechaInicio, HorasAyudantia, Asignatura)
    VALUES(30849378, TO_DATE('10/09/2022', 'DD/MM/YYYY'), 25, 'Matemáticas');

    INSERT INTO Curso(Materia, FechaInicio)
    VALUES('Cálculo Integral', TO_DATE('10/09/2022', 'DD/MM/YYYY'));

    INSERT INTO Asiste(CIEstudiante, CIProfesor, Materia, FechaInicio, Nota)
    VALUES(30849378, 31012721, 'Cálculo Integral', TO_DATE('10/09/2022', 'DD/MM/YYYY'), 14),
    (32000475, 31012721, 'Cálculo Integral', TO_DATE('10/09/2022', 'DD/MM/YYYY'), 15);
COMMIT;

BEGIN;
    INSERT INTO Tramite(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion)
    VALUES(30849378, 1, 'Deporte', 'Suscripción a Gimnasio', CURRENT_DATE);
    --TODO: Probar que Duración Real funcione
    INSERT INTO PasoActividad(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, OrdenSecuencial, DescripcionInteraccion, ResponsableAsignado, FechaInicio, DuracionEstimada)
    VALUES(30849378, 1, 'Deporte', 'Suscripción a Gimnasio', CURRENT_DATE, 1, 'Activar suscripción', 'Roberto Vargas', CURRENT_DATE, 30);

    INSERT INTO LineaCargo(CI, IDPrestadora, NombreCategoria, Descripcion, FechaCreacion, MesAñoApertura, NroLinea, FechaCargo, Cantidad, Concepto, ImpuestosLey)
    VALUES(30849378, 1, 'Deporte', 'Suscripción a Gimnasio', CURRENT_DATE, CURRENT_DATE, 1, CURRENT_TIMESTAMP, 1, 'Bebida energética del Gimnasio', 3);

    INSERT INTO Acompañante(
        CI, 
        CIMiembro,
        IDPrestadora, 
        NombreCategoria, 
        Descripcion,
        FechaCreacion, 
        PrimerNombre, 
        PrimerApellido)
    VALUES(
        30287122,
        30849378,
        1,
        'Deporte',
        'Suscripción a Gimnasio',
        CURRENT_DATE,
        'Manuel',
        'Rodrigo'
    );
COMMIT;

BEGIN;
    INSERT INTO Egresado(CI, Titulo, AñoGraduacion, IndiceAcademicoFinal)
    VALUES(31012721, 'Informática', TO_DATE('2021/01/01', 'YYYY/MM/DD'), 16);

    INSERT INTO OportunidadLaboral(RIF, FechaHoraOferta, PerfilBuscado, Cargo, Beneficios, Responsabilidades)
    VALUES('J-12345678-0', TO_DATE('12/12/2025', 'DD/MM/YYYY'), 'Informático especializado en bases de datos', 'Database Administrator', '200$ la hora, horario flexible', 'Diseñar, instalar y asegurar optimización de información');

    INSERT INTO SePostula(RIF, CI, FechaHoraOferta, Curriculum)
    VALUES('J-12345678-0', 31012721, TO_DATE('12/12/2025', 'DD/MM/YYYY'), E'\x7f\x7f');
COMMIT;

BEGIN;
    INSERT INTO MedioTransporte(Placa, Ubicacion, CarnetDeConducir, TipoVehiculo, Capacidad)
    VALUES('GF322D', 'Montalbán', E'\x7f\x7f', 'carro', 4);

    INSERT INTO Viaje(Placa, CarnetDeConducir, Destino)
    VALUES('GF322D', E'\x7f\x7f', 'Altamira');
COMMIT;

BEGIN;
    INSERT INTO Comprador(IDComprador)
    VALUES(1);

    UPDATE Miembro
    SET IDComprador = 1
    WHERE CI = 30849378;

    UPDATE EstadoCuenta
    SET EstadoFiscal = 'cerrado'
    WHERE CI=30849378 AND IDPrestadora = 1 AND NombreCategoria = 'Deporte' AND Descripcion = 'Suscripción a Gimnasio' AND FechaCreacion = CURRENT_DATE;
COMMIT;

BEGIN;
    CREATE TEMP TABLE var_factura ON COMMIT DROP AS
    WITH insercion_factura AS (
        INSERT INTO Factura (
            IDComprador, CI, IDPrestadora, NombreCategoria, 
            Descripcion, FechaCreacion, MesAñoApertura
        )
        VALUES (
            1, 30849378, 1, 'Deporte', 
            'Suscripción a Gimnasio', CURRENT_DATE, CURRENT_DATE
        )
        RETURNING Numero
    )
    SELECT Numero FROM insercion_factura;
    
    INSERT INTO Tasa(CambioReal)
    VALUES(699.35);

    INSERT INTO MetodoPago(IDMetodo, MontoRecibido)
    VALUES(1, 2), (2, 3.25), (3, 1.25), (4, 1.5), (5, 2.5), (6, 1);

    INSERT INTO PagaFactura(IDMetodo, NumeroFactura)
    SELECT v.id_metodo, (SELECT Numero FROM var_factura)
    FROM (VALUES (1), (2), (3), (4), (5), (6)) AS v(id_metodo);
        
    INSERT INTO TransaccionEnLinea(IDLinea, IDMetodo)
    VALUES(1, 1), (2, 2);

    INSERT INTO Criptomoneda(TXID, Red, DireccionBilletera, TasaConversion, IDLinea)
    VALUES('a1075db55d416d3ca199f55b6084e2115b9345e16c5cf302fc80e9d5fbf5d48d', 'TRC20', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa', 223, 1);

    INSERT INTO Zelle(CodigoConfirmacion, IDLinea, CorreoElectronico)
    VALUES(243, 2, 'false@mail');

    INSERT INTO PagoPresencial(IDPresencial, IDMetodo)
    VALUES(1, 3), (2, 4), (3, 5), (4, 6);

    INSERT INTO Tarjeta(NroTarjeta, CompañiaEmisora, MonedaLiquidacion, TipoRed, FechaVencimiento, IDPresencial)
    VALUES(456720340022, 'Mercantil', 'Dolar', 'nacional', TO_DATE('20/10/2027', 'DD/MM/YYYY'), 1);

    INSERT INTO PagoMovil(NroReferencia, TelefonoEmisor, Banco, IDPresencial)
    VALUES(111525520691, '0426-14099912', 'Banesco', 2);

    INSERT INTO TAI(POS, UID, IDPresencial)
    VALUES(716148647550, 242958645845, 3);

    INSERT INTO Efectivo(MonedaDeCurso, IDPresencial)
    VALUES('bolivares', 4);

    INSERT INTO DesgloseDenominaciones(FechaHoraPago, Cantidad, Valor)
    VALUES(CURRENT_TIMESTAMP, 2, 0.5);
COMMIT;

INSERT INTO Feriado (Fecha, Nombre) VALUES
('2026-01-01', 'Año Nuevo'),
('2026-05-01', 'Día del Trabajador'),
('2026-12-25', 'Navidad');

-- Deshabilitados temporalmente durante desarrollo para facilitar inserciones
ALTER TABLE Estudiante DISABLE TRIGGER trg_estudiante_asiste_minimo;
ALTER TABLE Profesor DISABLE TRIGGER trg_profesor_asiste_minimo;
ALTER TABLE Curso DISABLE TRIGGER trg_curso_asiste_minimo;
ALTER TABLE Edificacion DISABLE TRIGGER trg_edificacion_alberga_minimo;