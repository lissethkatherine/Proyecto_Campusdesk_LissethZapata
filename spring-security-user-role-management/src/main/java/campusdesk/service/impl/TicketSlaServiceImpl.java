package campusdesk.service.impl;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TicketSlaDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketSlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Ejercicio 8: SLA (Service Level Agreement = plazo máximo de atención).
 *
 * Cómo funciona: cada prioridad concede unas horas (TicketPriority.getSlaHoras()). La entidad Ticket calcula sola
 * fechaLimite = createdAt + esas horas, tanto al crearse (@PrePersist) como al cambiar la prioridad (@PreUpdate).
 * Un ticket está "vencido" cuando fechaLimite < ahora Y todavía no está CERRADA/CANCELADA.
 */
@Service
@RequiredArgsConstructor
public class TicketSlaServiceImpl implements TicketSlaService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TicketExtendedResponseDTO> findOverdueTickets(String userEmail) {
        // Usuario autenticado
        User user = findUserByEmail(userEmail);

        // Filtros de visibilidad por rol (null = sin filtro), igual que en el resto de listados
        Long solicitanteId = null;
        Long tecnicoId = null;
        if (user.getRole() == Role.USER) {
            solicitanteId = user.getId(); // solo sus tickets
        } else if (user.getRole() == Role.TECHNICIAN) {
            tecnicoId = user.getId(); // solo los asignados a él
        }

        // Lista de estados "finales" obtenida del propio enum (isFinal()), sin escribirlos a mano:
        // si mañana se agrega otro estado final, esta consulta lo contempla automáticamente.
        List<TicketStatus> estadosFinales = Arrays.stream(TicketStatus.values())
                .filter(TicketStatus::isFinal)
                .toList();

        // La consulta compara fechaLimite con "ahora" en la base de datos (ver TicketRepository.findOverdue)
        return ticketRepository.findOverdue(Instant.now(), estadosFinales, solicitanteId, tecnicoId)
                .stream()
                .map(ticketMapper::toExtended)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSlaDTO consultarSla(Long ticketId, String userEmail) {
        User user = findUserByEmail(userEmail);

        // 404 si no existe
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // 403 por visibilidad: ADMIN todo; USER solo el suyo; TECHNICIAN solo asignados
        if (user.getRole() == Role.USER
                && (ticket.getSolicitante() == null || !ticket.getSolicitante().getId().equals(user.getId()))) {
            throw new UnauthorizedTicketActionException("No tiene permisos para consultar el SLA de esta incidencia");
        }
        if (user.getRole() == Role.TECHNICIAN
                && (ticket.getTecnico() == null || !ticket.getTecnico().getId().equals(user.getId()))) {
            throw new UnauthorizedTicketActionException("Un técnico solo puede consultar incidencias asignadas a su perfil");
        }

        // Si el ticket es antiguo y su columna fecha_limite está vacía, la calculamos al vuelo con la misma fórmula
        Instant fechaLimite = ticket.getFechaLimite() != null
                ? ticket.getFechaLimite()
                : ticket.getCreatedAt().plus(Duration.ofHours(ticket.getPriority().getSlaHoras()));

        // "Ahora" se toma una sola vez para que todos los cálculos de la respuesta sean coherentes entre sí
        Instant ahora = Instant.now();

        // Vencido = ya pasó el límite y el ticket sigue activo (un ticket cerrado ya no "vence")
        boolean vencido = !ticket.getStatus().isFinal() && ahora.isAfter(fechaLimite);

        // ChronoUnit.HOURS.between(a, b) = horas completas de a hasta b; si b es anterior a a, el resultado es negativo
        long horasRestantes = ChronoUnit.HOURS.between(ahora, fechaLimite);

        return TicketSlaDTO.builder()
                .ticketId(ticket.getId())
                .status(ticket.getStatus())
                .prioridad(ticket.getPriority())
                .slaHoras(ticket.getPriority().getSlaHoras())
                .fechaCreacion(ticket.getCreatedAt())
                .fechaLimite(fechaLimite)
                .vencido(vencido)
                .horasRestantes(horasRestantes)
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
