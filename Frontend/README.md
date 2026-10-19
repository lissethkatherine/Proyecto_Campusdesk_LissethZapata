# CAMPUSDESK — Frontend

HTML5, CSS3 y JavaScript puro (sin frameworks). Se sirve con Nginx dentro de Docker (`http://localhost:5506`) y consume la API con `fetch` y `async/await`.

- `registro.html` / `login.html`: autenticación (el token se guarda en `sessionStorage`).
- `dashboard.html` + `js/app.js`: indicadores, tickets, filtros, detalle, comentarios, historial, administración, crear técnicos y cambio de contraseña.
- `js/auth-guard.js`: protege las páginas privadas y cierra sesión.

Estructura: páginas `.html` en la raíz, estilos en `css/`, scripts en `js/` e imágenes en `assets/img/`.

Las cuentas de prueba y la guía de ejecución están en el [README principal](../README.md).
