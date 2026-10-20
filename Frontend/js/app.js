/**
 * CAMPUSDESK - Lógica de Frontend Vanilla JS (TechNova Solutions)
 * Arquitectura: HTML5 + CSS3 + Fetch API nativa. Sin frameworks externos.
 * Ciclo de vida de incidencias, máquina de estados, métricas, comentarios, auditoría y administración.
 */
'use strict';

(function () {
  // 1. Configuración de API y URLs
  const BASE_URL = window.CAMPUSDESK_API_URL;

  // 2. Estado de la Aplicación
  const AppState = {
    currentUser: null,
    tickets: [],
    technicians: [],
    users: [],
    activeTicket: null,
    activeFilterStatus: '',
    activeFilterPriority: '',
    activeFilterCategory: '',
    searchQuery: '',
    userFilterRole: '',
    userSearchQuery: ''
  };

  // 3. Utilidades y Helpers
  function getHeaders() {
    const token = AuthGuard.getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    };
  }

  let isHandlingSessionExpiry = false;

  /**
   * Cliente HTTP centralizado para peticiones protegidas.
   * Intercepta respuestas 401 (sesión expirada o token revocado) redirigiendo ordenadamente a login.
   */
  async function apiFetch(url, options = {}) {
    const headers = Object.assign({}, getHeaders(), options.headers || {});
    try {
      const resp = await fetch(url, { ...options, headers });
      if (resp.status === 401) {
        console.warn('Sesión expirada o token inválido (401). Redirigiendo a login...');
        if (!isHandlingSessionExpiry) {
          isHandlingSessionExpiry = true;
          showToast('Tu sesión ha expirado. Redirigiendo al inicio de sesión...', 'error');
          setTimeout(async () => {
            await AuthGuard.logout();
          }, 1200);
        }
        return null;
      }
      return resp;
    } catch (err) {
      throw err;
    }
  }

  function formatDate(isoStr) {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('es-CO', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoStr;
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function showToast(message, type = 'success') {
    const toast = document.getElementById('toastBox');
    const msg = document.getElementById('toastMessage');
    if (!toast || !msg) return;

    msg.textContent = message;
    toast.className = `campus-toast show ${type}`;

    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => {
      toast.className = 'campus-toast';
    }, 3800);
  }

  // 4. Inicialización de Usuario y Perfil
  async function initUserProfile() {
    const role = AuthGuard.getRole();
    const storedEmail = AuthGuard.getUserEmail();

    const roleBadge = document.getElementById('navUserRole');
    const emailSpan = document.getElementById('navUserEmail');
    const btnNavAdmin = document.getElementById('btnNavAdmin');

    if (roleBadge) {
      roleBadge.textContent = role;
      roleBadge.className = `role-pill ${role.toLowerCase()}`;
    }
    if (emailSpan) {
      emailSpan.textContent = storedEmail || 'Usuario';
    }

    // F2: Habilitar botón de navegación a Administración solo para ADMIN
    if (btnNavAdmin) {
      if (role === 'ADMIN') {
        btnNavAdmin.style.display = 'inline-flex';
      } else {
        btnNavAdmin.style.display = 'none';
      }
    }

    try {
      const resp = await apiFetch(`${BASE_URL}/api/users/me`);
      if (!resp) return;
      if (resp.ok) {
        const user = await resp.json();
        AppState.currentUser = user;
        if (emailSpan) emailSpan.textContent = user.name || user.email;
        if (roleBadge) {
          roleBadge.textContent = user.role;
          roleBadge.className = `role-pill ${user.role.toLowerCase()}`;
        }
        if (btnNavAdmin) {
          btnNavAdmin.style.display = user.role === 'ADMIN' ? 'inline-flex' : 'none';
        }
      }
    } catch (e) {
      console.warn('No se pudo refrescar el perfil desde el backend:', e);
    }
  }

  // 5. Carga de Técnicos (E4: Exclusivo para ADMIN para asignación de incidencias)
  async function loadTechnicians() {
    if (AuthGuard.getRole() !== 'ADMIN') {
      return;
    }
    try {
      const resp = await apiFetch(`${BASE_URL}/api/users/technicians`);
      if (!resp) return;
      if (resp.ok) {
        AppState.technicians = await resp.json();
      }
    } catch (e) {
      console.warn('Error al cargar lista de técnicos:', e);
    }
  }

  // 5.1 Carga de Todos los Usuarios (F2: Exclusivo para ADMIN)
  async function loadAdminUsers() {
    if (AuthGuard.getRole() !== 'ADMIN') return;
    const tbody = document.getElementById('usersTableBody');
    if (tbody) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 30px; color: #64748B;">
            Consultando usuarios registrados...
          </td>
        </tr>
      `;
    }

    try {
      const resp = await apiFetch(`${BASE_URL}/api/users`);
      if (!resp || !resp.ok) {
        if (tbody) {
          tbody.innerHTML = `
            <tr>
              <td colspan="5" style="text-align: center; padding: 30px; color: #E11D48;">
                Error al cargar usuarios del servidor.
              </td>
            </tr>
          `;
        }
        return;
      }

      AppState.users = await resp.json();
      renderAdminUsersTable();
    } catch (e) {
      console.error('Error cargando usuarios:', e);
      if (tbody) {
        tbody.innerHTML = `
          <tr>
            <td colspan="5" style="text-align: center; padding: 30px; color: #E11D48;">
              Fallo de conexión al cargar usuarios.
            </td>
          </tr>
        `;
      }
    }
  }

  function renderAdminUsersTable() {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    const query = AppState.userSearchQuery.toLowerCase().trim();
    const roleFilter = AppState.userFilterRole;

    const filtered = AppState.users.filter(u => {
      const matchesRole = !roleFilter || u.role === roleFilter;
      const matchesQuery = !query ||
        (u.name || '').toLowerCase().includes(query) ||
        (u.email || '').toLowerCase().includes(query) ||
        String(u.id).includes(query);
      return matchesRole && matchesQuery;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 35px; color: #64748B;">
            No se encontraron usuarios coincidentes con el criterio de búsqueda.
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = filtered.map(u => {
      const roleClass = (u.role || 'USER').toLowerCase();
      return `
        <tr>
          <td style="font-weight: 700; color: #475569;">#${u.id}</td>
          <td style="font-weight: 600; color: #0F172A;">${escapeHtml(u.name)}</td>
          <td style="color: #475569;">${escapeHtml(u.email)}</td>
          <td><span class="role-pill ${roleClass}">${u.role}</span></td>
          <td style="text-align: right; white-space: nowrap;">
            <button class="btn-campus-secondary" style="padding: 5px 10px; font-size: 12px;" onclick="window.CampusDesk.filterTicketsByUser('${escapeHtml(u.email)}')">
              Ver Incidencias
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 6. Carga de Métricas del Dashboard (RF-07)
  async function loadDashboardMetrics() {
    try {
      const resp = await apiFetch(`${BASE_URL}/api/dashboard/metrics`);
      if (!resp || !resp.ok) return;

      const data = await resp.json();
      const mTotal = document.getElementById('metricTotal');
      const mAbiertas = document.getElementById('metricAbiertas');
      const mAsignadas = document.getElementById('metricAsignadas');
      const mProceso = document.getElementById('metricEnProceso');
      const mResueltas = document.getElementById('metricResueltas');
      const mCerradas = document.getElementById('metricCerradas');

      if (mTotal) mTotal.textContent = data.total ?? 0;
      if (mAbiertas) mAbiertas.textContent = data.abiertas ?? 0;
      if (mAsignadas) mAsignadas.textContent = data.asignadas ?? 0;
      if (mProceso) mProceso.textContent = data.enProceso ?? 0;
      if (mResueltas) mResueltas.textContent = data.resueltas ?? 0;
      if (mCerradas) mCerradas.textContent = data.cerradas ?? 0;
    } catch (e) {
      console.error('Error cargando métricas:', e);
    }
  }

  // 7. Carga y Filtrado de Incidencias (RF-03, RF-08)
  async function loadTickets() {
    const tbody = document.getElementById('ticketsTableBody');
    if (!tbody) return;

    tbody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; padding: 30px; color: #64748B;">
          Actualizando lista de incidencias...
        </td>
      </tr>
    `;

    try {
      const params = new URLSearchParams();
      if (AppState.activeFilterStatus) params.append('status', AppState.activeFilterStatus);
      if (AppState.activeFilterPriority) params.append('priority', AppState.activeFilterPriority);
      if (AppState.activeFilterCategory) params.append('category', AppState.activeFilterCategory);

      const url = `${BASE_URL}/api/tickets?${params.toString()}`;
      const resp = await apiFetch(url);
      if (!resp) return;

      if (!resp.ok) {
        tbody.innerHTML = `
          <tr>
            <td colspan="9" style="text-align: center; padding: 30px; color: #E11D48;">
              Error al obtener incidencias del servidor (${resp.status}).
            </td>
          </tr>
        `;
        return;
      }

      AppState.tickets = await resp.json();
      renderTicketsTable();
    } catch (e) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 30px; color: #E11D48;">
            No se pudo conectar con el servicio de tickets.
          </td>
        </tr>
      `;
      console.error(e);
    }
  }

  function renderTicketsTable() {
    const tbody = document.getElementById('ticketsTableBody');
    if (!tbody) return;

    const query = AppState.searchQuery.toLowerCase().trim();

    const filtered = AppState.tickets.filter(t => {
      if (!query) return true;
      const titleMatch = (t.title || '').toLowerCase().includes(query);
      const descMatch = (t.description || '').toLowerCase().includes(query);
      const idMatch = String(t.id).includes(query);
      const reqMatch = t.solicitante && (t.solicitante.name || t.solicitante.email || '').toLowerCase().includes(query);
      const techMatch = t.tecnico && (t.tecnico.name || t.tecnico.email || '').toLowerCase().includes(query);
      return titleMatch || descMatch || idMatch || reqMatch || techMatch;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 40px; color: #64748B;">
            <div style="font-size: 24px; margin-bottom: 8px;">📋</div>
            <div style="font-weight: 600; font-size: 15px; color: #334155;">No se encontraron incidencias</div>
            <div style="font-size: 13px; margin-top: 4px;">Modifica los filtros o crea una nueva incidencia con el botón superior.</div>
          </td>
        </tr>
      `;
      return;
    }

    const currentRole = AuthGuard.getRole();
    const currentEmail = AuthGuard.getUserEmail();

    tbody.innerHTML = filtered.map(t => {
      const id = t.id;
      const title = escapeHtml(t.title);
      const cat = t.category || 'OTROS';
      const prio = t.priority || 'MEDIA';
      const status = t.status || 'ABIERTA';
      const solicitante = t.solicitante ? escapeHtml(t.solicitante.name || t.solicitante.email) : '—';
      const tecnico = t.tecnico ? escapeHtml(t.tecnico.name || t.tecnico.email) : '<span style="color: #94A3B8; font-style: italic;">Sin asignar</span>';
      const fecha = formatDate(t.createdAt);

      // Acciones rápidas según rol y estado estricto
      let shortcutBtn = '';

      // F1: Solicitante puede editar si está ABIERTA y sin técnico asignado
      const isRequester = t.solicitante && t.solicitante.email === currentEmail;
      if (isRequester && status === 'ABIERTA' && !t.tecnico) {
        shortcutBtn = `<button class="btn-campus-secondary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px; border-color: #3B82F6; color: #1D4ED8;" onclick="window.CampusDesk.openEditModal(${id})">Editar</button>`;
      } else if (currentRole === 'ADMIN' && (status === 'ABIERTA' || status === 'ASIGNADA')) {
        shortcutBtn = `<button class="btn-campus-secondary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px;" onclick="window.CampusDesk.openAssignQuick(${id})">Asignar</button>`;
      } else if (currentRole === 'TECHNICIAN' && status === 'ASIGNADA' && t.tecnico && t.tecnico.email === currentEmail) {
        shortcutBtn = `<button class="btn-campus-primary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'EN_PROCESO', 'Iniciando atención')">Atender</button>`;
      } else if (currentRole === 'TECHNICIAN' && status === 'EN_PROCESO' && t.tecnico && t.tecnico.email === currentEmail) {
        shortcutBtn = `<button class="btn-campus-primary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px; background: #059669;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'RESUELTA', 'Incidencia resuelta')">Resolver</button>`;
      } else if (status === 'RESUELTA' && isRequester) {
        // E2: Solo el solicitante puede cerrar la incidencia
        shortcutBtn = `<button class="btn-campus-secondary" style="padding: 4px 8px; font-size: 11.5px; margin-right: 6px; border-color: #065F46; color: #065F46;" onclick="window.CampusDesk.quickChangeStatus(${id}, 'CERRADA', 'Cierre confirmado')">Cerrar</button>`;
      }

      return `
        <tr>
          <td style="font-weight: 700; color: #475569;">#${id}</td>
          <td>
            <div style="font-weight: 600; color: #0F172A;">${title}</div>
          </td>
          <td><span class="badge badge-cat">${cat}</span></td>
          <td><span class="badge badge-prio-${prio}">${prio}</span></td>
          <td><span class="badge badge-status-${status}">${status.replace('_', ' ')}</span></td>
          <td style="font-size: 13px;">${solicitante}</td>
          <td style="font-size: 13px;">${tecnico}</td>
          <td style="font-size: 12.5px; color: #64748B;">${fecha}</td>
          <td style="text-align: right; white-space: nowrap;">
            ${shortcutBtn}
            <button class="btn-campus-secondary" style="padding: 5px 10px; font-size: 12px;" onclick="window.CampusDesk.openDetailModal(${id})">
              Ver Detalle
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }

  // 8. Crear / Editar Incidencia (RF-03, F1)
  function openCreateModal() {
    const form = document.getElementById('formCreateTicket');
    if (form) form.reset();

    const editIdInput = document.getElementById('editTicketId');
    if (editIdInput) editIdInput.value = '';

    const modalTitle = document.getElementById('modalCreateTicketTitle');
    if (modalTitle) modalTitle.textContent = 'Crear Nueva Incidencia';

    const submitBtn = document.getElementById('btnSubmitCreate');
    if (submitBtn) submitBtn.textContent = 'Guardar Incidencia';

    openModal('modalCreateTicket');
  }

  async function openEditModal(ticketId) {
    let ticket = AppState.tickets.find(t => t.id === ticketId);
    if (!ticket || !ticket.description) {
      try {
        const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}`);
        if (resp && resp.ok) {
          ticket = await resp.json();
        }
      } catch (e) {
        console.error('Error al obtener datos para edición:', e);
      }
    }

    if (!ticket) {
      showToast('No se pudo cargar la incidencia para edición', 'error');
      return;
    }

    const editIdInput = document.getElementById('editTicketId');
    if (editIdInput) editIdInput.value = ticket.id;

    const modalTitle = document.getElementById('modalCreateTicketTitle');
    if (modalTitle) modalTitle.textContent = `Editar Incidencia #${ticket.id}`;

    const submitBtn = document.getElementById('btnSubmitCreate');
    if (submitBtn) submitBtn.textContent = 'Actualizar Incidencia';

    const titleInput = document.getElementById('newTitle');
    const catInput = document.getElementById('newCategory');
    const prioInput = document.getElementById('newPriority');
    const descInput = document.getElementById('newDescription');

    if (titleInput) titleInput.value = ticket.title || '';
    if (catInput) catInput.value = ticket.category || 'REDES';
    if (prioInput) prioInput.value = ticket.priority || 'MEDIA';
    if (descInput) descInput.value = ticket.description || '';

    closeModal('modalDetailTicket');
    openModal('modalCreateTicket');
  }

  async function handleCreateOrUpdateTicket(e) {
    e.preventDefault();
    const editIdInput = document.getElementById('editTicketId');
    const editId = editIdInput ? editIdInput.value : '';
    const isEditing = Boolean(editId);

    const btnSubmit = document.getElementById('btnSubmitCreate');
    if (btnSubmit) {
      btnSubmit.disabled = true;
      btnSubmit.textContent = isEditing ? 'Actualizando...' : 'Guardando...';
    }

    const payload = {
      title: document.getElementById('newTitle').value.trim(),
      category: document.getElementById('newCategory').value,
      priority: document.getElementById('newPriority').value,
      description: document.getElementById('newDescription').value.trim()
    };

    try {
      const url = isEditing ? `${BASE_URL}/api/tickets/${editId}` : `${BASE_URL}/api/tickets`;
      const method = isEditing ? 'PUT' : 'POST';

      const resp = await apiFetch(url, {
        method: method,
        body: JSON.stringify(payload)
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        const msg = (err && (err.message || err.error)) || (isEditing ? 'Error al actualizar la incidencia' : 'Error al crear la incidencia');
        showToast(msg, 'error');
        return;
      }

      const savedTicket = await resp.json();
      showToast(isEditing ? `Incidencia #${savedTicket.id} actualizada correctamente` : `Incidencia #${savedTicket.id} creada exitosamente`, 'success');

      closeModal('modalCreateTicket');
      const form = document.getElementById('formCreateTicket');
      if (form) form.reset();
      if (editIdInput) editIdInput.value = '';

      // Recargar tablero
      await loadDashboardMetrics();
      await loadTickets();

      // Si se estaba visualizando el detalle, actualizarlo
      if (isEditing && AppState.activeTicket && AppState.activeTicket.id === savedTicket.id) {
        await openDetailModal(savedTicket.id);
      }
    } catch (err) {
      showToast('Error de comunicación con el servidor', 'error');
      console.error(err);
    } finally {
      if (btnSubmit) {
        btnSubmit.disabled = false;
        btnSubmit.textContent = isEditing ? 'Actualizar Incidencia' : 'Guardar Incidencia';
      }
    }
  }

  // 9. Abrir Detalle Integral del Ticket (RF-04, RF-05, RF-06)
  async function openDetailModal(ticketId) {
    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}`);
      if (!resp) return;
      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        const msg = (err && (err.message || err.error)) || 'No se pudo cargar el detalle del ticket';
        showToast(msg, 'error');
        return;
      }

      const ticket = await resp.json();
      AppState.activeTicket = ticket;

      // Renderizar datos básicos
      document.getElementById('detailTicketId').textContent = `#${ticket.id}`;
      document.getElementById('detailTicketTitle').textContent = ticket.title;
      document.getElementById('detailDescription').textContent = ticket.description;

      const statusBadge = `<span class="badge badge-status-${ticket.status}">${ticket.status.replace('_', ' ')}</span>`;
      const prioBadge = `<span class="badge badge-prio-${ticket.priority}">${ticket.priority}</span>`;
      document.getElementById('detailStatusBadge').innerHTML = statusBadge;
      document.getElementById('detailPriorityBadge').innerHTML = prioBadge;

      const solicitante = ticket.solicitante
        ? `${escapeHtml(ticket.solicitante.name)} (${escapeHtml(ticket.solicitante.email)})`
        : '—';
      const tecnico = ticket.tecnico
        ? `${escapeHtml(ticket.tecnico.name)} (${escapeHtml(ticket.tecnico.email)})`
        : '<span style="color: #94A3B8; font-style: italic;">Sin asignar</span>';

      document.getElementById('detailSolicitante').innerHTML = solicitante;
      document.getElementById('detailTecnico').innerHTML = tecnico;

      // Renderizar Panel de Acciones de Flujo de Estados
      renderActionPanel(ticket);

      // Cargar Comentarios e Historial
      await loadComments(ticket.id);
      await loadHistory(ticket.id);

      // Activar la primera pestaña por defecto
      switchTab('tab-info');

      // Abrir modal
      openModal('modalDetailTicket');
    } catch (e) {
      showToast('Error de comunicación con el servidor', 'error');
      console.error(e);
    }
  }

  function renderActionPanel(ticket) {
    const container = document.getElementById('dynamicActionPanel');
    const role = AuthGuard.getRole();
    const userEmail = AuthGuard.getUserEmail();

    // 1. Si el ticket está en estado final (CERRADA o CANCELADA): Inmutabilidad
    if (ticket.status === 'CANCELADA') {
      container.innerHTML = `
        <div class="closed-box">
          <span style="font-size: 18px;">🚫</span>
          <div>
            <div>Esta incidencia fue <strong>CANCELADA</strong> por el solicitante.</div>
            <div style="font-size: 12px; font-weight: normal; color: #64748B; margin-top: 2px;">
              No requiere más atención. Los datos e historial se conservan y ya no pueden modificarse.
            </div>
          </div>
        </div>
      `;
      return;
    }
    if (ticket.status === 'CERRADA') {
      container.innerHTML = `
        <div class="closed-box">
          <span style="font-size: 18px;">🔒</span>
          <div>
            <div>Esta incidencia está <strong>CERRADA</strong>.</div>
            <div style="font-size: 12px; font-weight: normal; color: #64748B; margin-top: 2px;">
              El ciclo de vida ha concluido satisfactoriamente. Los datos e historial son inmutables.
            </div>
          </div>
        </div>
      `;
      return;
    }

    let actionsHtml = '';

    // F1: Solo el solicitante puede editar, mientras la incidencia esté ABIERTA y sin técnico asignado
    const isRequester = ticket.solicitante && ticket.solicitante.email === userEmail;
    if (isRequester && ticket.status === 'ABIERTA' && !ticket.tecnico) {
      actionsHtml += `
        <div class="action-box" style="background: #F0FDF4; border-color: #BBF7D0; margin-bottom: 14px;">
          <div class="action-box-title" style="color: #166534;">✏ Edición de Incidencia (F1)</div>
          <p style="font-size: 13px; color: #15803D; margin-bottom: 12px;">
            La incidencia está abierta y aún no ha sido asignada a ningún técnico de soporte. Puedes modificar el título, la categoría, la prioridad o la descripción.
          </p>
          <button class="btn-campus-secondary" style="background: white; border-color: #16A34A; color: #15803D; font-weight: 600;" onclick="window.CampusDesk.openEditModal(${ticket.id})">
            ✏ Editar Datos de la Incidencia
          </button>
        </div>
      `;
    }

    // Cancelación: el solicitante retira su solicitud (p. ej. ya la resolvió por su cuenta)
    if (isRequester && ['ABIERTA', 'ASIGNADA', 'EN_PROCESO'].includes(ticket.status)) {
      actionsHtml += `
        <div class="action-box" style="background: #FEF2F2; border-color: #FECACA; margin-bottom: 14px;">
          <div class="action-box-title" style="color: #991B1B;">🚫 Cancelar Incidencia</div>
          <p style="font-size: 13px; color: #B91C1C; margin-bottom: 12px;">
            ¿Ya lo solucionaste por tu cuenta? Cancela la solicitud para que el técnico no siga trabajando en ella.
            Esta acción no se puede deshacer.
          </p>
          <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
            <input type="text" id="cancelReasonInput" maxlength="200" placeholder="Motivo (opcional). Ej. Ya lo solucioné, era el cable" style="flex: 1; min-width: 200px; padding: 9px; border: 1px solid #FECACA; border-radius: 6px; font-size: 13px;">
            <button class="btn-campus-primary" style="background: #DC2626;" onclick="window.CampusDesk.cancelTicket(${ticket.id})">
              Cancelar Incidencia
            </button>
          </div>
        </div>
      `;
    }

    // 2. Acciones para ADMINISTRADOR (Asignar o reasignar técnicos, solo en ABIERTA o ASIGNADA)
    if (role === 'ADMIN' && (ticket.status === 'ABIERTA' || ticket.status === 'ASIGNADA')) {
      const techOptions = AppState.technicians.map(t => {
        const isSelected = ticket.tecnico && ticket.tecnico.id === t.id ? 'selected' : '';
        return `<option value="${t.id}" ${isSelected}>${escapeHtml(t.name)} (${escapeHtml(t.email)})</option>`;
      }).join('');

      const actionTitle = ticket.status === 'ASIGNADA' ? 'Reasignación de Técnico' : 'Asignación de Técnico Responsable';
      const actionBtnText = ticket.status === 'ASIGNADA' ? 'Reasignar Técnico' : 'Asignar Técnico';

      actionsHtml += `
        <div class="action-box">
          <div class="action-box-title">⚙ Administración: ${actionTitle}</div>
          <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
            <select id="selectAssignTech" style="flex: 1; min-width: 200px; padding: 9px; border: 1px solid #CBD5E1; border-radius: 6px;">
              <option value="">-- Seleccionar Técnico --</option>
              ${techOptions}
            </select>
            <button class="btn-campus-primary" style="padding: 9px 16px; font-size: 13.5px;" onclick="window.CampusDesk.assignTechnician(${ticket.id})">
              ${actionBtnText}
            </button>
          </div>
        </div>
      `;
    }

    // 3. Acciones para TÉCNICO ASIGNADO (Iniciar atención o Resolver)
    // E2: Exclusivo para el técnico asignado (el administrador no debe saltarse las reglas de atención técnica)
    const isAssignedTech = ticket.tecnico && ticket.tecnico.email === userEmail;
    if (isAssignedTech) {
      if (ticket.status === 'ASIGNADA') {
        actionsHtml += `
          <div class="action-box" style="background: #EFF6FF; border-color: #BFDBFE;">
            <div class="action-box-title" style="color: #1E40AF;">▶ Gestión Técnica: Iniciar Atención</div>
            <p style="font-size: 13px; color: #3B82F6; margin-bottom: 12px;">
              La incidencia se encuentra asignada a tu perfil. Presiona el botón para iniciar el diagnóstico técnico.
            </p>
            <button class="btn-campus-primary" style="background: #2563EB;" onclick="window.CampusDesk.updateStatus(${ticket.id}, 'EN_PROCESO', 'Técnico inicia atención e investigación')">
              Iniciar Atención (Pasar a EN PROCESO)
            </button>
          </div>
        `;
      } else if (ticket.status === 'EN_PROCESO') {
        actionsHtml += `
          <div class="action-box" style="background: #ECFDF5; border-color: #A7F3D0;">
            <div class="action-box-title" style="color: #065F46;">✓ Gestión Técnica: Concluir y Resolver</div>
            <p style="font-size: 13px; color: #047857; margin-bottom: 12px;">
              Indica la solución aplicada para que el usuario solicitante pueda validar y confirmar el cierre.
            </p>
            <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
              <input type="text" id="resolveCommentInput" placeholder="Detalle de solución técnica aplicada..." style="flex: 1; padding: 9px; border: 1px solid #CBD5E1; border-radius: 6px; font-size: 13px;">
              <button class="btn-campus-primary" style="background: #10B981;" onclick="window.CampusDesk.resolveTicket(${ticket.id})">
                Marcar como RESUELTA
              </button>
            </div>
          </div>
        `;
      }
    }

    // 4. Acciones para USUARIO SOLICITANTE (Cerrar ticket resuelto)
    // E2: Exclusivo para el solicitante
    if (isRequester) {
      if (ticket.status === 'RESUELTA') {
        actionsHtml += `
          <div class="action-box" style="background: #F8FAFC; border-color: #CBD5E1;">
            <div class="action-box-title" style="color: #334155;">🔒 Conformidad y Cierre de Incidencia</div>
            <p style="font-size: 13px; color: #64748B; margin-bottom: 12px;">
              El soporte técnico ha marcado el problema como RESUELTO. Si estás conforme con la solución brindada, confirma el cierre definitivo.
            </p>
            <button class="btn-campus-primary" style="background: #334155;" onclick="window.CampusDesk.updateStatus(${ticket.id}, 'CERRADA', 'Solicitante confirma conformidad y cierre definitivo')">
              Confirmar Conformidad y Cerrar Incidencia
            </button>
          </div>
        `;
      }
    }

    container.innerHTML = actionsHtml || `
      <div style="padding: 12px; background: #F8FAFC; border-radius: 6px; font-size: 13px; color: #64748B; text-align: center;">
        No hay transiciones manuales pendientes para tu perfil en el estado actual (${ticket.status.replace('_', ' ')}).
      </div>
    `;
  }

  // 10. Asignar Técnico (RF-04)
  async function assignTechnician(ticketId) {
    const select = document.getElementById('selectAssignTech');
    if (!select || !select.value) {
      showToast('Por favor selecciona un técnico de la lista', 'error');
      return;
    }

    const tecnicoId = parseInt(select.value, 10);
    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}/assign`, {
        method: 'PATCH',
        body: JSON.stringify({ tecnicoId })
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'Error al asignar técnico', 'error');
        return;
      }

      showToast('Técnico asignado exitosamente', 'success');
      await openDetailModal(ticketId);
      await loadDashboardMetrics();
      await loadTickets();
    } catch (e) {
      showToast('Error de red al asignar técnico', 'error');
      console.error(e);
    }
  }

  // 11. Cambiar Estado en la Máquina de Estados (RF-04)
  async function updateStatus(ticketId, newStatus, defaultComment = '') {
    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({
          status: newStatus,
          comentario: defaultComment
        })
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        const msg = (err && (err.message || err.error)) || 'Transición de estado no autorizada';
        showToast(msg, 'error');
        return;
      }

      showToast(`Estado actualizado a ${newStatus.replace('_', ' ')}`, 'success');
      await openDetailModal(ticketId);
      await loadDashboardMetrics();
      await loadTickets();
    } catch (e) {
      showToast('Error de comunicación al actualizar estado', 'error');
      console.error(e);
    }
  }

  async function cancelTicket(ticketId) {
    if (!window.confirm('¿Seguro que deseas cancelar esta incidencia? Esta acción no se puede deshacer.')) return;
    const input = document.getElementById('cancelReasonInput');
    const reason = (input && input.value.trim()) || 'Cancelada por el solicitante: ya la resolvió por su cuenta';
    await updateStatus(ticketId, 'CANCELADA', reason);
  }

  function resolveTicket(ticketId) {
    const input = document.getElementById('resolveCommentInput');
    const comment = (input && input.value.trim()) || 'Solución técnica verificada';
    updateStatus(ticketId, 'RESUELTA', comment);
  }

  // 12. Hilo de Comentarios (RF-05)
  async function loadComments(ticketId) {
    const container = document.getElementById('commentsContainer');
    const countBadge = document.getElementById('detailCommentCount');
    const formContainer = document.getElementById('commentFormContainer');

    if (!container) return;
    container.innerHTML = '<div style="color: #64748B; font-size: 13px;">Cargando notas...</div>';

    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}/comments`);
      if (!resp) return;

      if (!resp.ok) {
        container.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin comentarios registrados.</div>';
        return;
      }

      const comments = await resp.json();
      if (countBadge) countBadge.textContent = comments.length;

      // Si ticket está CERRADO, deshabilitar formulario de comentarios
      if (formContainer) {
        if (AppState.activeTicket && AppState.activeTicket.status === 'CERRADA') {
          formContainer.innerHTML = `
            <div class="closed-box" style="margin-top: 14px;">
              <span>🔒</span> No se pueden registrar comentarios adicionales en un ticket CERRADO.
            </div>
          `;
        } else {
          formContainer.innerHTML = `
            <form id="formAddComment" style="margin-top: 14px;" onsubmit="window.CampusDesk.handleAddComment(event, ${ticketId})">
              <textarea id="commentContent" rows="3" required placeholder="Escriba una nota o actualización técnica sobre esta incidencia..." style="width: 100%; padding: 10px; border: 1px solid #CBD5E1; border-radius: 6px; resize: vertical;"></textarea>
              <div style="text-align: right; margin-top: 8px;">
                <button type="submit" class="btn-campus-primary" style="padding: 8px 16px; font-size: 13.5px;" id="btnSubmitComment">
                  Agregar Comentario
                </button>
              </div>
            </form>
          `;
        }
      }

      if (comments.length === 0) {
        container.innerHTML = '<div style="color: #64748B; font-size: 13px; text-align: center; padding: 20px;">No hay comentarios aún. Escribe el primero abajo.</div>';
        return;
      }

      container.innerHTML = comments.map(c => {
        const authorName = c.author ? escapeHtml(c.author.name || c.author.email) : 'Usuario';
        const authorRole = c.author ? c.author.role : 'USER';
        const dateStr = formatDate(c.createdAt);
        const bodyStr = escapeHtml(c.content);

        return `
          <div class="comment-card">
            <div class="comment-header">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span class="comment-author">${authorName}</span>
                <span class="role-pill ${authorRole.toLowerCase()}" style="font-size: 10px; padding: 2px 6px;">${authorRole}</span>
              </div>
              <span style="color: #94A3B8;">${dateStr}</span>
            </div>
            <div class="comment-body">${bodyStr}</div>
          </div>
        `;
      }).join('');
    } catch (e) {
      console.error(e);
      container.innerHTML = '<div style="color: #E11D48; font-size: 13px;">Error al cargar comentarios.</div>';
    }
  }

  async function handleAddComment(e, ticketId) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitComment');
    const textarea = document.getElementById('commentContent');
    const content = textarea.value.trim();

    if (!content) return;

    if (btn) {
      btn.disabled = true;
      btn.textContent = 'Enviando...';
    }

    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ content })
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'Error al agregar comentario', 'error');
        return;
      }

      showToast('Comentario agregado con éxito', 'success');
      textarea.value = '';
      await loadComments(ticketId);
    } catch (err) {
      showToast('Error de red al agregar comentario', 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = 'Agregar Comentario';
      }
    }
  }

  // 13. Trazabilidad y Auditoría (RF-06)
  async function loadHistory(ticketId) {
    const timeline = document.getElementById('historyTimeline');
    if (!timeline) return;
    timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Cargando historial de auditoría...</div>';

    try {
      const resp = await apiFetch(`${BASE_URL}/api/tickets/${ticketId}/history`);
      if (!resp) return;

      if (!resp.ok) {
        timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin historial registrado.</div>';
        return;
      }

      const history = await resp.json();
      if (history.length === 0) {
        timeline.innerHTML = '<div style="color: #64748B; font-size: 13px;">Sin cambios registrados.</div>';
        return;
      }

      timeline.innerHTML = history.map(h => {
        const dateStr = formatDate(h.fechaCambio);
        const authorName = h.autor ? escapeHtml(h.autor.name || h.autor.email) : 'Sistema';
        const authorRole = h.autor ? h.autor.role : 'SYSTEM';
        const oldState = h.estadoAnterior
          ? `<span class="badge badge-status-${h.estadoAnterior}" style="font-size: 10px;">${h.estadoAnterior.replace('_', ' ')}</span>`
          : '<span style="color: #94A3B8; font-size: 11px;">INICIO</span>';
        const newState = `<span class="badge badge-status-${h.estadoNuevo}" style="font-size: 10px;">${h.estadoNuevo.replace('_', ' ')}</span>`;
        const comment = escapeHtml(h.comentarioTransicion || 'Cambio de estado');

        return `
          <div class="timeline-item">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-meta">
                <span>
                  <strong>${authorName}</strong> (${authorRole})
                </span>
                <span>${dateStr}</span>
              </div>
              <div style="margin: 6px 0; display: flex; align-items: center; gap: 8px;">
                ${oldState} <span style="color: #94A3B8;">&rarr;</span> ${newState}
              </div>
              <div class="timeline-comment">${comment}</div>
            </div>
          </div>
        `;
      }).join('');
    } catch (e) {
      console.error(e);
      timeline.innerHTML = '<div style="color: #E11D48; font-size: 13px;">Error al cargar auditoría.</div>';
    }
  }

  // 14. Modales, Navegación de Vistas y Pestañas
  // 14. Crear Técnico (solo ADMIN) y Cambiar Contraseña (todos los roles)
  const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,100}$/;

  function openCreateTechnicianModal() {
    if (AuthGuard.getRole() !== 'ADMIN') {
      showToast('Solo un administrador puede crear técnicos', 'error');
      return;
    }
    const form = document.getElementById('formCreateTechnician');
    if (form) form.reset();
    openModal('modalCreateTechnician');
  }

  async function handleCreateTechnician(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitCreateTechnician');
    const payload = {
      name: document.getElementById('techName').value.trim(),
      email: document.getElementById('techEmail').value.trim()
    };

    if (!payload.name || !payload.email) {
      showToast('Nombre y correo son obligatorios', 'error');
      return;
    }

    if (btn) { btn.disabled = true; btn.textContent = 'Creando...'; }
    try {
      const resp = await apiFetch(`${BASE_URL}/api/users/technicians`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'No se pudo crear el técnico', 'error');
        return;
      }

      const tech = await resp.json();
      showToast(`Técnico ${tech.name} creado con la contraseña inicial común`, 'success');
      closeModal('modalCreateTechnician');
      await loadTechnicians();
      await loadAdminUsers();
    } catch (err) {
      showToast('Error de comunicación con el servidor', 'error');
      console.error(err);
    } finally {
      if (btn) { btn.disabled = false; btn.textContent = 'Crear Técnico'; }
    }
  }

  function openChangePasswordModal() {
    const form = document.getElementById('formChangePassword');
    if (form) form.reset();
    openModal('modalChangePassword');
  }

  async function handleChangePassword(e) {
    e.preventDefault();
    const current = document.getElementById('currentPassword').value;
    const next = document.getElementById('newPassword').value;
    const confirm = document.getElementById('confirmPassword').value;

    if (!PASSWORD_REGEX.test(next)) {
      showToast('La nueva contraseña debe tener 8 o más caracteres, con mayúscula, minúscula y número', 'error');
      return;
    }
    if (next !== confirm) {
      showToast('La confirmación no coincide con la nueva contraseña', 'error');
      return;
    }

    const btn = document.getElementById('btnSubmitChangePassword');
    if (btn) { btn.disabled = true; btn.textContent = 'Guardando...'; }
    try {
      const resp = await apiFetch(`${BASE_URL}/api/users/me/password`, {
        method: 'PATCH',
        body: JSON.stringify({ currentPassword: current, newPassword: next })
      });
      if (!resp) return;

      if (!resp.ok) {
        const err = await resp.json().catch(() => null);
        showToast((err && (err.message || err.error)) || 'No se pudo cambiar la contraseña', 'error');
        return;
      }

      showToast('Contraseña actualizada. Inicia sesión nuevamente.', 'success');
      closeModal('modalChangePassword');
      // El backend revoca todas las sesiones: se limpia la sesión local y se vuelve al login
      setTimeout(async () => { await AuthGuard.logout(); }, 1500);
    } catch (err) {
      showToast('Error de comunicación con el servidor', 'error');
      console.error(err);
    } finally {
      if (btn) { btn.disabled = false; btn.textContent = 'Guardar Contraseña'; }
    }
  }

  function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('active');
  }

  function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('active');
  }

  function switchTab(targetTabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === targetTabId);
    });
    document.querySelectorAll('.tab-content').forEach(content => {
      content.style.display = content.id === targetTabId ? 'block' : 'none';
    });
  }

  function switchMainView(viewName) {
    const secHelpdesk = document.getElementById('sectionHelpdesk');
    const secAdmin = document.getElementById('sectionAdmin');
    const btnNavHelpdesk = document.getElementById('btnNavHelpdesk');
    const btnNavAdmin = document.getElementById('btnNavAdmin');

    if (viewName === 'admin') {
      if (AuthGuard.getRole() !== 'ADMIN') {
        showToast('Acceso denegado: Solo administradores pueden ver esta sección', 'error');
        return;
      }
      if (secHelpdesk) secHelpdesk.style.display = 'none';
      if (secAdmin) secAdmin.style.display = 'block';
      if (btnNavHelpdesk) btnNavHelpdesk.classList.remove('active-view-btn');
      if (btnNavAdmin) btnNavAdmin.classList.add('active-view-btn');
      loadAdminUsers();
    } else {
      if (secHelpdesk) secHelpdesk.style.display = 'block';
      if (secAdmin) secAdmin.style.display = 'none';
      if (btnNavHelpdesk) btnNavHelpdesk.classList.add('active-view-btn');
      if (btnNavAdmin) btnNavAdmin.classList.remove('active-view-btn');
      loadTickets();
    }
  }

  // 15. Vinculación Integral de Eventos en el DOM (REPARACIÓN CRÍTICA)
  function setupEventListeners() {
    // Logout
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
      btnLogout.addEventListener('click', async () => {
        await AuthGuard.logout();
      });
    }

    // Refresh
    const btnRefresh = document.getElementById('btnRefresh');
    if (btnRefresh) {
      btnRefresh.addEventListener('click', async () => {
        await loadDashboardMetrics();
        await loadTickets();
        showToast('Tablero de incidencias actualizado');
      });
    }

    // Navegación de Vistas (Helpdesk vs Administración)
    const btnNavHelpdesk = document.getElementById('btnNavHelpdesk');
    if (btnNavHelpdesk) {
      btnNavHelpdesk.addEventListener('click', () => switchMainView('helpdesk'));
    }

    const btnNavAdmin = document.getElementById('btnNavAdmin');
    if (btnNavAdmin) {
      btnNavAdmin.addEventListener('click', () => switchMainView('admin'));
    }

    // Eventos de la Vista de Administración
    const btnRefreshAdminUsers = document.getElementById('btnRefreshAdminUsers');
    if (btnRefreshAdminUsers) {
      btnRefreshAdminUsers.addEventListener('click', async () => {
        await loadAdminUsers();
        showToast('Lista de usuarios actualizada');
      });
    }

    const searchUserInput = document.getElementById('searchUserInput');
    if (searchUserInput) {
      searchUserInput.addEventListener('input', e => {
        AppState.userSearchQuery = e.target.value;
        renderAdminUsersTable();
      });
    }

    const filterUserRole = document.getElementById('filterUserRole');
    if (filterUserRole) {
      filterUserRole.addEventListener('change', e => {
        AppState.userFilterRole = e.target.value;
        renderAdminUsersTable();
      });
    }

    const btnResetUserFilters = document.getElementById('btnResetUserFilters');
    if (btnResetUserFilters) {
      btnResetUserFilters.addEventListener('click', () => {
        AppState.userSearchQuery = '';
        AppState.userFilterRole = '';
        if (searchUserInput) searchUserInput.value = '';
        if (filterUserRole) filterUserRole.value = '';
        renderAdminUsersTable();
      });
    }

    // Crear técnico (ADMIN)
    const btnOpenCreateTechnician = document.getElementById('btnOpenCreateTechnician');
    if (btnOpenCreateTechnician) btnOpenCreateTechnician.addEventListener('click', openCreateTechnicianModal);
    ['btnCloseCreateTechnician', 'btnCancelCreateTechnician'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => closeModal('modalCreateTechnician'));
    });
    const formCreateTechnician = document.getElementById('formCreateTechnician');
    if (formCreateTechnician) formCreateTechnician.addEventListener('submit', handleCreateTechnician);

    // Cambiar contraseña (todos los roles)
    const btnChangePassword = document.getElementById('btnChangePassword');
    if (btnChangePassword) btnChangePassword.addEventListener('click', openChangePasswordModal);
    ['btnCloseChangePassword', 'btnCancelChangePassword'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => closeModal('modalChangePassword'));
    });
    const formChangePassword = document.getElementById('formChangePassword');
    if (formChangePassword) formChangePassword.addEventListener('submit', handleChangePassword);

    // Modal Crear / Editar
    const btnOpenCreateModal = document.getElementById('btnOpenCreateModal');
    if (btnOpenCreateModal) {
      btnOpenCreateModal.addEventListener('click', openCreateModal);
    }

    const btnCloseCreateModal = document.getElementById('btnCloseCreateModal');
    if (btnCloseCreateModal) {
      btnCloseCreateModal.addEventListener('click', () => {
        closeModal('modalCreateTicket');
      });
    }

    const btnCancelCreate = document.getElementById('btnCancelCreate');
    if (btnCancelCreate) {
      btnCancelCreate.addEventListener('click', () => {
        closeModal('modalCreateTicket');
      });
    }

    const formCreateTicket = document.getElementById('formCreateTicket');
    if (formCreateTicket) {
      formCreateTicket.addEventListener('submit', handleCreateOrUpdateTicket);
    }

    // Modal Detalle
    const btnCloseDetailModal = document.getElementById('btnCloseDetailModal');
    if (btnCloseDetailModal) {
      btnCloseDetailModal.addEventListener('click', () => {
        closeModal('modalDetailTicket');
      });
    }

    const btnExitDetail = document.getElementById('btnExitDetail');
    if (btnExitDetail) {
      btnExitDetail.addEventListener('click', () => {
        closeModal('modalDetailTicket');
      });
    }

    // Pestañas del Modal
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const tabTarget = btn.getAttribute('data-tab');
        if (tabTarget) {
          switchTab(tabTarget);
        }
      });
    });

    // Filtros de tabla de incidencias
    const filterStatus = document.getElementById('filterStatus');
    if (filterStatus) {
      filterStatus.addEventListener('change', e => {
        AppState.activeFilterStatus = e.target.value;
        updateActiveCard();
        loadTickets();
      });
    }

    const filterPriority = document.getElementById('filterPriority');
    if (filterPriority) {
      filterPriority.addEventListener('change', e => {
        AppState.activeFilterPriority = e.target.value;
        loadTickets();
      });
    }

    const filterCategory = document.getElementById('filterCategory');
    if (filterCategory) {
      filterCategory.addEventListener('change', e => {
        AppState.activeFilterCategory = e.target.value;
        loadTickets();
      });
    }

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
      searchInput.addEventListener('input', e => {
        AppState.searchQuery = e.target.value;
        renderTicketsTable();
      });
    }

    const btnResetFilters = document.getElementById('btnResetFilters');
    if (btnResetFilters) {
      btnResetFilters.addEventListener('click', () => {
        AppState.activeFilterStatus = '';
        AppState.activeFilterPriority = '';
        AppState.activeFilterCategory = '';
        AppState.searchQuery = '';
        if (filterStatus) filterStatus.value = '';
        if (filterPriority) filterPriority.value = '';
        if (filterCategory) filterCategory.value = '';
        if (searchInput) searchInput.value = '';
        updateActiveCard();
        loadTickets();
      });
    }

    // Click en Tarjetas de Métricas para filtrar
    document.querySelectorAll('.metric-card').forEach(card => {
      card.addEventListener('click', () => {
        const filterVal = card.getAttribute('data-filter') || '';
        AppState.activeFilterStatus = filterVal;
        if (filterStatus) filterStatus.value = filterVal;
        updateActiveCard();
        loadTickets();
      });
    });

    // Cerrar modal al presionar Escape
    window.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        closeModal('modalCreateTicket');
        closeModal('modalDetailTicket');
        closeModal('modalCreateTechnician');
        closeModal('modalChangePassword');
      }
    });

    // Cerrar modal al hacer clic en el backdrop / overlay
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', e => {
        if (e.target === overlay) {
          overlay.classList.remove('active');
        }
      });
    });
  }

  function updateActiveCard() {
    document.querySelectorAll('.metric-card').forEach(card => {
      const filter = card.getAttribute('data-filter');
      if (AppState.activeFilterStatus === filter) {
        card.classList.add('active-filter');
      } else {
        card.classList.remove('active-filter');
      }
    });
  }

  // 16. Exposición Global para Acciones Directas en la Tabla
  window.CampusDesk = {
    openDetailModal: openDetailModal,
    openCreateModal: openCreateModal,
    openEditModal: openEditModal,
    openAssignQuick: function(ticketId) {
      openDetailModal(ticketId);
    },
    quickChangeStatus: function(ticketId, newStatus, comment) {
      updateStatus(ticketId, newStatus, comment);
    },
    assignTechnician: assignTechnician,
    updateStatus: updateStatus,
    resolveTicket: resolveTicket,
    cancelTicket: cancelTicket,
    handleAddComment: handleAddComment,
    switchMainView: switchMainView,
    filterTicketsByUser: function(userEmail) {
      switchMainView('helpdesk');
      const searchInput = document.getElementById('searchInput');
      if (searchInput) {
        searchInput.value = userEmail;
        AppState.searchQuery = userEmail;
        renderTicketsTable();
      }
    }
  };

  // 17. Inicialización Principal Robusta
  async function initApp() {
    setupEventListeners();
    await initUserProfile();
    if (AuthGuard.getRole() === 'ADMIN') {
      await loadTechnicians();
    }
    await loadDashboardMetrics();
    await loadTickets();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initApp);
  } else {
    initApp();
  }

})();
