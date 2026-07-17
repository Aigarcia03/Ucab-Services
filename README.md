
# UCAB-Services Grupo 5

Este proyecto es un ecosistema integrado de gestión de servicios académicos y administrativos diseñado para centralizar y unificar las operaciones de la Universidad Católica Andrés Bello (UCAB) en sus campus de Montalbán y Guayana.

A diferencia de un portal de pagos convencional, UCAB-Services gestiona de punta a punta identidades dinámicas, recursos físicos finitos, flujos financieros multimoneda complejos y auditorías en tiempo real.




## Instalación
Este servicio requiere:

- Tener instalado Node.Js.
- Poseer un entorno donde correr SpringBoot.
- Tener usuario en Postgresql.

Pasos para instalar y correr el Servicio:
- Abrir archivo backend/target/classes/application.properties
- Colocar en spring.datasource.username el nombre de tu usuario de postgresql
- Colocar en spring.datasource.password la contraseña de tu usuario de postgresql

## Correr Programa
Primero instalar vue
```bash
    npm create vue@latest
```

Después se puede correr el servicio.
```bash
    npm run
```
    