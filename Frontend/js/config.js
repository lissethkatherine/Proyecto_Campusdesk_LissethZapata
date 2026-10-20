/**
 * CAMPUSDESK - Configuración única de la URL del backend.
 * Se carga ANTES que auth-guard.js / app.js y el resto de scripts de las páginas.
 *  - Local con Docker (nginx en :5506): usa el mismo origen (nginx hace proxy a /api y /auth).
 *  - Cualquier otro caso (Live Server, servidor local, hosting publicado): backend en Railway.
 * Si guardas 'api_url' en localStorage (campo de index.html) tiene prioridad.
 */
(function () {
  'use strict';
  var RAILWAY_API = 'https://proyectocampusdesklissethzapata-production.up.railway.app';
  var host = window.location.hostname;
  var isLocal = host === 'localhost' || host === '127.0.0.1';
  var url = localStorage.getItem('api_url');
  if (!url) {
    // Solo con Docker local (nginx en :5506) se usa el mismo origen; en cualquier otro caso, el backend de Railway
    url = (isLocal && window.location.port === '5506') ? window.location.origin : RAILWAY_API;
  }
  // Sin "/" final: evita URLs con "//api/..." que Spring Security rechaza
  window.CAMPUSDESK_API_URL = url.replace(/\/+$/, '');
})();
