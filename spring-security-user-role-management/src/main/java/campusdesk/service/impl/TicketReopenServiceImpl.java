package campusdesk.service.impl;

import campusdesk.dto.ReopenTicketDTO;
import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketHistory;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.InvalidStateTransitionException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TicketHistoryRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketReopenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Ejercicio 2: reapertura de tickets RESUELTA (ventana de 48 horas, contador e historial consistentes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketReopenServiceImpl implements TicketReopenService {

    private static final long VENTANA_REAPERTURA_HORAS = 48;
    private static final int MAX_COMENTARIO_HISTORIAL = 255; // longitud de la columna comentario_transicion

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public TicketExtendedResponseDTO reopenTicket(Long ticketId, ReopenTicketDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // Solo el solicitante (creador) puede reabrir (403)
        if (ticket.getSolicitante() == null || !ticket.getSolicitante().getId().equals(user.getId())) {
            throw new UnauthorizedTicketActionException("Solo el usuario creador del ticket puede reabrirlo");
        }

        // Solo desde RESUELTA (409)
        if (ticket.getStatus() != TicketStatus.RESUELTA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede reabrir una incidencia en estado RESUELTA (estado actual: " + ticket.getStatus() + ")");
        }

        // Ventana de 48 horas contadas desde la última vez que el ticket pasó a RESUELTA (400)
        Instant resueltaEn = ticketHistoryRepository.findLastTransitionDate(ticketId, TicketStatus.RESUELTA)
                .orElse(ticket.getUpdatedAt());
        long horasTranscurridas = ChronoUnit.HOURS.between(resueltaEn, Instant.now());
        if (horasTranscurridas >= VENTANA_REAPERTURA_HORAS) {
            throw new IllegalArgumentException("El plazo de " + VENTANA_REAPERTURA_HORAS
                    + " horas para reabrir este ticket ya venció (resuelto hace " + horasTranscurridas
                    + " horas). Debe crearse un nuevo ticket");
        }

        String motivo = dto.getMotivo().trim();

        ticket.setStatus(TicketStatus.EN_PROCESO);
        ticket.setReaperturasCount(ticket.getReaperturasCount() + 1);
        Ticket saved = ticketRepository.save(ticket);

        // Historial en la misma transacción: si falla, se revierte también el cambio de estado
        TicketHistory history = TicketHistory.builder()
                .ticket(saved)
                .estadoAnterior(TicketStatus.RESUELTA)
                .estadoNuevo(TicketStatus.EN_PROCESO)
                .autor(user)
                .comentarioTransicion(truncar("Reapertura #" + saved.getReaperturasCount() + ": " + motivo))
                .build();
        ticketHistoryRepository.save(history);

        log.info("Ticket #{} reabierto por [{}] (reaperturas: {})", ticketId, userEmail, saved.getReaperturasCount());
        return ticketMapper.toExtended(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketExtendedResponseDTO> findReopenedTickets() {
        return ticketRepository.findReopenedTickets().stream().map(ticketMapper::toExtended).toList();
    }

    private String truncar(String texto) {
        return texto.length() <= MAX_COMENTARIO_HISTORIAL
                ? texto
                : texto.substring(0, MAX_COMENTARIO_HISTORIAL - 3) + "...";
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
