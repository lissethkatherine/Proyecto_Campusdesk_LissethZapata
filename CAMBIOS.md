# Cambios de esta versión

## Nuevo
- **Crear técnicos (ADMIN):** `POST /api/users/technicians` (nombre y correo). Rol TECHNICIAN y contraseña inicial **común** tomada de `TECHNICIAN_DEFAULT_PASSWORD`. Pantalla: Administración → ➕ Crear Técnico.
- **Cambiar contraseña (todos los roles):** `PATCH /api/users/me/password` (contraseña actual + nueva con política de seguridad). Revoca todas las sesiones del usuario. Pantalla: 🔑 Contraseña (barra superior).

- **Cancelar incidencia (estado `CANCELADA`):** el solicitante puede cancelar su ticket (ABIERTA, ASIGNADA o EN_PROCESO) si lo resolvió por su cuenta. Estado final e inmutable; el técnico ya no tiene trabajo pendiente. Botón en el detalle del ticket, filtro y estilo nuevos.

## Errores corregidos
1. Un ticket ya no puede crearse en otro estado que no sea ABIERTA (se quitó `status` de `CreateTicketDTO` y el selector «Estado» del formulario).
2. `updateStatus` ya no permite retrocesos ni destinos ABIERTA/ASIGNADA (se quitaron esos `case`; devuelve 409). Se eliminó el selector de estado libre del detalle del ticket.
3. Editar ticket: solo el solicitante, ticket ABIERTO y sin técnico. El admin ya no edita y la edición ya no cambia estados (se quitó `status` de `UpdateTicketDTO`).
4. Sin token ahora responde **401** en JSON (antes probablemente 403): `authenticationEntryPoint` y `accessDeniedHandler` en `SecurityConfig`.
5. `docker-compose.yml` ya no trae contraseñas ni secreto JWT por defecto (`POSTGRES_PASSWORD`, `REDIS_PASSWORD` y `JWT_SECRET` son obligatorios en `.env`).
6. Se quitó MapStruct (no se usaba) de `build.gradle`.
7. README y `docs/evidencias/README.md` actualizados (sin cifras de pruebas sin verificar). La captura suelta pasó a `docs/evidencias/`.

## Pruebas
- Ajustadas: `TicketServiceTest`, `CampusdeskE2EWorkflowTest`.
- Nuevas: crear técnico, cambiar contraseña (servicio, controlador y validación de DTO), retrocesos de estado rechazados, admin no edita tickets.

## Antes de usar
- Agregue `TECHNICIAN_DEFAULT_PASSWORD` a su `.env` (ver `.env.example`).
- Su `.env` debe tener `POSTGRES_PASSWORD`, `REDIS_PASSWORD` y `JWT_SECRET`.
- Ejecute `./gradlew clean test` y `docker compose up --build`.
