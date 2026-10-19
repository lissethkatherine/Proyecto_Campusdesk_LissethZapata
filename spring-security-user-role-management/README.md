# CAMPUSDESK — Backend (Spring Boot)

API REST del sistema de gestión de incidencias. Java 21, Spring Boot, Spring Data JPA (PostgreSQL), Spring Security + JWT, Redis (sesiones), Swagger.

- **Puerto:** `8056` — Swagger: <http://localhost:8056/swagger-ui.html>
- **Variables de entorno:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (mín. 32 caracteres), `REDIS_HOST`, `REDIS_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `TECHNICIAN_DEFAULT_PASSWORD`. Se definen en el `.env` de la raíz (ver `.env.example`).
- **Pruebas:** `./gradlew clean test`
- **Ejecución recomendada:** desde la raíz del proyecto, `docker compose up --build`.

La documentación completa (arquitectura, roles, máquina de estados, cuentas de prueba) está en el [README principal](../README.md).
