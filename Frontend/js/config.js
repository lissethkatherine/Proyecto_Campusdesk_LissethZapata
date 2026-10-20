/**
 * CAMPUSDESK - Configuración única de la URL del backend.
 * Se carga ANTES que auth-guard.js / app.js y el resto de scripts de las páginas.
 *  - Local con Docker (nginx en :5506): usa el mismo origen (nginx hace proxy a /api y /auth).
 *  - Local sin Docker: backend en http://localhost:8056.
 *  - Publicado (Railway u otro hosting): backend en Railway.
 * Si guardas 'api_url' en localStorage (campo de index.html) tiene prioridad.
 */
(function () {
  'use strict';
  var RAILWAY_API = 'https://proyectocampusdesklissethzapata-production.up.railway.app';
  var host = window.location.hostname;
  var isLocal = host === 'localhost' || host === '127.0.0.1';
  var url = localStorage.getItem('api_url');
  if (!url) {
    url = isLocal
      ? (window.location.port === '5506' ? window.location.origin : 'http://localhost:8056')
      : RAILWAY_API;
  }
  // Sin "/" final: evita URLs con "//api/..." que Spring Security rechaza
  window.CAMPUSDESK_API_URL = url.replace(/\/+$/, '');
})();
