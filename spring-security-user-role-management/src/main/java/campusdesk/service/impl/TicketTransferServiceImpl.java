package campusdesk.service.impl;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TransferTicketDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketCategory;
import campusdesk.entity.TicketComment;
import campusdesk.entity.TicketHistory;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.TicketClosedException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TicketCommentRepository;
import campusdesk.repository.TicketHistoryRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ejercicio 3: transferencia interdepartamental con desvinculación segura del técnico y auditoría transaccional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTransferServiceImpl implements TicketTransferService {

    private static final int MAX_COMENTARIO_HISTORIAL = 255; // longitud de la columna comentario_transicion

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public TicketExtendedResponseDTO transferTicket(Long ticketId, TransferTicketDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // Solo ADMIN o el TECHNICIAN actualmente asignado (403)
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isAssignedTechnician = ticket.getTecnico() != null && ticket.getTecnico().getId().equals(user.getId());
        if (!isAdmin && !isAssignedTechnician) {
            throw new UnauthorizedTicketActionException(
                    "Solo un administrador o el técnico asignado actualmente puede transferir esta incidencia");
        }

        // Un ticket CERRADA/CANCELADA es inmutable en todo el sistema (409)
        if (ticket.getStatus().isFinal()) {
            throw new TicketClosedException("No se puede transferir una incidencia en estado " + ticket.getStatus());
        }

        // La nueva categoría debe ser distinta a la actual (400)
        TicketCategory categoriaAnterior = ticket.getCategory();
        TicketCategory nuevaCategoria = dto.getNuevaCategoria();
        if (nuevaCategoria == categoriaAnterior) {
            throw new IllegalArgumentException("La nueva categoría debe ser distinta a la actual (" + categoriaAnterior + ")");
        }

        TicketStatus estadoAnterior = ticket.getStatus();
        User tecnicoAnterior = ticket.getTecnico();
        String justificacion = dto.getJustificacion().trim();

        // Cambio de área: se desvincula al técnico y el estado retrocede obligatoriamente a ABIERTA
        ticket.setCategory(nuevaCategoria);
        ticket.setTecnico(null);
        ticket.setStatus(TicketStatus.ABIERTA);
        Ticket saved = ticketRepository.save(ticket);

        String tecnicoDesvinculado = tecnicoAnterior != null ? tecnicoAnterior.getName() : "ninguno";

        // Auditoría en el historial (misma transacción)
        TicketHistory history = TicketHistory.builder()
                .ticket(saved)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(TicketStatus.ABIERTA)
                .autor(user)
                .comentarioTransicion(truncar("Transferencia de " + categoriaAnterior + " a " + nuevaCategoria
                        + " (técnico desvinculado: " + tecnicoDesvinculado + ")"))
                .build();
        ticketHistoryRepository.save(history);

        // Nota automática del sistema en el hilo de comentarios (autor: quien ejecuta la transferencia)
        TicketComment nota = TicketComment.builder()
                .ticket(saved)
                .author(user)
                .content("[SISTEMA] Ticket transferido de la categoría " + categoriaAnterior + " a " + nuevaCategoria
                        + ". Técnico desvinculado: " + tecnicoDesvinculado + ". Motivo: " + justificacion)
                .build();
        ticketCommentRepository.save(nota);

        log.info("Ticket #{} transferido de {} a {} por [{}]", ticketId, categoriaAnterior, nuevaCategoria, userEmail);
        return ticketMapper.toExtended(saved);
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
