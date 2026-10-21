package campusdesk.service.impl;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketHistory;
import campusdesk.entity.TicketPriority;
import campusdesk.entity.User;
import campusdesk.exception.TicketClosedException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TicketHistoryRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketPriorityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ejercicio 6: lógica de negocio del escalamiento de prioridad.
 *
 * Idea central: la prioridad es un enum ORDENADO (BAJA, MEDIA, ALTA, CRITICA). Como Java numera las
 * constantes de un enum con ordinal() (0, 1, 2, 3), "subir un nivel" es simplemente tomar la siguiente
 * constante. Esa lógica vive en TicketPriority.siguiente(), así el servicio no repite el cálculo.
 */
@Service // Registra la clase como bean de Spring: el framework la instancia y la inyecta donde se pida TicketPriorityService
@RequiredArgsConstructor // Lombok crea el constructor con los campos final; Spring lo usa para inyectar las dependencias
@Slf4j // Lombok crea la variable estática "log" para escribir en los logs
public class TicketPriorityServiceImpl implements TicketPriorityService {

    // Acceso a la tabla tickets
    private final TicketRepository ticketRepository;
    // Acceso a la tabla de historial (auditoría de cambios)
    private final TicketHistoryRepository ticketHistoryRepository;
    // Acceso a la tabla users (para saber quién ejecuta la acción)
    private final UserRepository userRepository;
    // Convierte entidades en DTOs para no exponer la entidad JPA directamente
    private final TicketMapper ticketMapper;

    @Override
    // @Transactional: todo el método es UNA transacción. Si algo falla (excepción), se revierte el cambio
    // de prioridad Y el registro de historial: nunca queda uno guardado sin el otro.
    @Transactional
    public TicketExtendedResponseDTO escalarPrioridad(Long ticketId, String userEmail) {
        // 1) Identificamos al usuario autenticado por su correo (viene del token JWT, no del body: no se puede falsificar)
        User user = findUserByEmail(userEmail);

        // 2) Buscamos el ticket; si no existe lanzamos TicketNotFoundException -> el handler responde 404
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // 3) Permisos: ADMIN siempre puede; un TECHNICIAN solo si es el técnico asignado a ESTE ticket
        boolean isAdmin = user.getRole() == Role.ADMIN;
        // Se comprueba "tecnico != null" primero porque un ticket sin asignar tiene tecnico = null (evita NullPointerException)
        boolean isAssignedTechnician = ticket.getTecnico() != null && ticket.getTecnico().getId().equals(user.getId());
        if (!isAdmin && !isAssignedTechnician) {
            // Excepción propia -> ApiExceptionHandler la traduce a HTTP 403 Forbidden
            throw new UnauthorizedTicketActionException(
                    "Solo un administrador o el técnico asignado puede escalar la prioridad");
        }

        // 4) Regla de estado: un ticket CERRADA/CANCELADA es inmutable en todo el sistema -> HTTP 409 Conflict
        if (ticket.getStatus().isFinal()) {
            throw new TicketClosedException("No se puede escalar una incidencia en estado " + ticket.getStatus());
        }

        // 5) Guardamos la prioridad actual: la necesitamos para el mensaje de error y para el historial
        TicketPriority prioridadAnterior = ticket.getPriority();

        // 6) Regla de datos: CRITICA es el tope, no hay nivel superior -> IllegalArgumentException = HTTP 400 Bad Request
        if (prioridadAnterior == TicketPriority.CRITICA) {
            throw new IllegalArgumentException("El ticket ya tiene la prioridad máxima (CRITICA)");
        }

        // 7) Calculamos el nuevo nivel con el método del enum (ALTA.ordinal()=2 -> values()[3] = CRITICA)
        TicketPriority nuevaPrioridad = prioridadAnterior.siguiente();

        // 8) Aplicamos el cambio sobre la entidad (aún no se escribe en la base de datos)
        ticket.setPriority(nuevaPrioridad);

        // 9) saveAndFlush = guardar Y enviar el UPDATE a la BD ya mismo. Es necesario porque el callback @PreUpdate
        //    de Ticket (que recalcula fechaLimite según la nueva prioridad) solo se ejecuta al hacer flush.
        //    Con save() normal el flush ocurriría al final de la transacción, DESPUÉS de armar la respuesta,
        //    y el DTO devolvería la fecha límite vieja.
        Ticket saved = ticketRepository.saveAndFlush(ticket);

        // 10) Auditoría: dejamos constancia de quién escaló y de qué a qué.
        //     La tabla de historial exige estado anterior/nuevo; aquí el estado NO cambia, así que repetimos el actual.
        TicketHistory history = TicketHistory.builder()
                .ticket(saved) // a qué ticket pertenece la entrada
                .estadoAnterior(saved.getStatus()) // el estado no cambia...
                .estadoNuevo(saved.getStatus()) // ...por eso anterior y nuevo coinciden
                .autor(user) // quién ejecutó la acción
                .comentarioTransicion("Prioridad escalada de " + prioridadAnterior + " a " + nuevaPrioridad) // qué se hizo
                .build(); // construye el objeto (patrón Builder de Lombok)
        // Se guarda dentro de la MISMA transacción que el cambio de prioridad
        ticketHistoryRepository.save(history);

        // 11) Registro técnico en logs (útil para depurar en Railway)
        log.info("Ticket #{} escalado de {} a {} por [{}]", ticketId, prioridadAnterior, nuevaPrioridad, userEmail);

        // 12) Devolvemos un DTO (nunca la entidad): incluye la nueva prioridad y la nueva fechaLimite
        return ticketMapper.toExtended(saved);
    }

    @Override
    // readOnly = true: solo lectura, Hibernate no revisa cambios (más rápido) y garantiza que no se modifique nada.
    // También mantiene abierta la sesión para poder leer las relaciones LAZY (solicitante, tecnico, tags) al mapear.
    @Transactional(readOnly = true)
    public List<TicketExtendedResponseDTO> findCriticalTickets(String userEmail) {
        // Usuario autenticado
        User user = findUserByEmail(userEmail);

        // Filtros de visibilidad por rol. null significa "sin filtro" para esa columna en la consulta.
        Long solicitanteId = null; // ADMIN y TECHNICIAN no filtran por solicitante
        Long tecnicoId = null; // ADMIN y USER no filtran por técnico
        if (user.getRole() == Role.USER) {
            // El usuario normal solo ve los tickets que él creó
            solicitanteId = user.getId();
        } else if (user.getRole() == Role.TECHNICIAN) {
            // El técnico solo ve los tickets asignados a él
            tecnicoId = user.getId();
        }

        // Reutilizamos la consulta multicriterio que ya existe en el proyecto (status, priority, category, solicitante, técnico)
        return ticketRepository.findWithFilters(null, TicketPriority.CRITICA, null, solicitanteId, tecnicoId)
                .stream() // recorremos la lista como un flujo
                .filter(ticket -> !ticket.getStatus().isFinal()) // descartamos los ya cerrados o cancelados: ya no son urgentes
                .map(ticketMapper::toExtended) // entidad -> DTO
                .toList(); // flujo -> lista inmutable
    }

    // Método auxiliar: evita repetir la búsqueda del usuario y su manejo de error
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
