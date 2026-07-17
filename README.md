# UCAB-Services Grupo 5

Este proyecto es un ecosistema integrado de gestión de servicios académicos y administrativos diseñado para centralizar y unificar las operaciones de la Universidad Católica Andrés Bello (UCAB) en sus campus de Montalbán y Guayana.

A diferencia de un portal de pagos convencional, UCAB-Services gestiona de punta a punta identidades dinámicas, recursos físicos finitos, flujos financieros multimoneda complejos y auditorías en tiempo real.

## Requisitos

- **Node.js** (v18+) y **npm**
- **Java 17** (JDK)
- **Maven** (incluido en el proyecto o instalado globalmente)
- **PostgreSQL 15+**
- **pgAdmin** (opcional, para administrar la base de datos)

## Instalación

### 1. Base de datos

1. Instalar PostgreSQL y pgAdmin.
2. Abrir pgAdmin y crear una base de datos llamada `Ucab-services`.
3. Abrir la herramienta **Query Tool** para la base de datos `Ucab-services`.
4. Copiar y pegar el contenido del archivo `Database.sql` (incluye creación de tablas, funciones, triggers y datos de prueba) y ejecutarlo.
5. Verificar que todas las tablas se crearon correctamente.

### 2. Configurar credenciales de la base de datos

Abrir `backend/src/main/resources/application.properties` y ajustar:

```properties
spring.datasource.username=postgres
spring.datasource.password=tu_contraseña
```

### 3. Backend (Spring Boot)

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

El backend se iniciará en `http://localhost:8080`.

### 4. Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

El frontend se iniciará en `http://127.0.0.1:5173`.

> El frontend tiene un proxy configurado en `vite.config.js` que redirige las peticiones `/api` a `http://localhost:8080`, por lo que no es necesario configurar CORS manualmente.

## Uso

1. Abrir `http://127.0.0.1:5173` en el navegador.
2. Registrarse como nuevo miembro o iniciar sesión con un usuario existente.
3. Usuarios de prueba incluidos en los inserts de `Database.sql`:
   - CI: `30849378`, Contraseña: `123` (Estudiante/Becario/Preparador)
   - CI: `32000475`, Contraseña: `32Lp` (Estudiante)
   - CI: `31012721`, Contraseña: `Er453` (Profesor)
   - CI: `29071218`, Contraseña: `q@if2m_pl23` (Personal Administrativo)
4. Administrador: `admin@ucab.edu.ve` (cualquier contraseña en Miembro)

## Estructura del proyecto

```
Ucab-Services/
├── backend/                 # API REST Spring Boot (Java 17, Maven)
│   └── src/main/java/com/ucab/services/controllers/
├── frontend/                # Cliente Vue 3 (Vite)
│   └── src/views/
├── Database.sql             # Script completo de la base de datos
└── Reporte*.jrxml           # Reportes JasperReports
```

## Notas

- El frontend usa `http://127.0.0.1:5173` para evitar conflictos con otros servicios en `localhost:3000`.
- El backend corre en el puerto `8080` por defecto (Spring Boot).
- Los reportes `.jrxml` se generan con JasperReports y se pueden compilar con JasperStudio.
