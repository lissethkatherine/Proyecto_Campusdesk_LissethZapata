package campusdesk.service.impl;

import campusdesk.service.TicketCommentService;
import campusdesk.dto.CommentResponseDTO;
import campusdesk.dto.CreateCommentDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketComment;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.TicketClosedException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.repository.TicketCommentRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de la gestión de comentarios para incidencias (RF-05).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCommentServiceImpl implements TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponseDTO addComment(Long ticketId, CreateCommentDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        // 1. Inmutabilidad de tickets en estado final (CERRADA o CANCELADA)
        if (ticket.getStatus().isFinal()) {
            throw new TicketClosedException("No se pueden agregar comentarios a una incidencia en estado " + ticket.getStatus());
        }

        // 2. Control de acceso
        validateUserAccess(ticket, user, "comentar en");

        // Ejercicio 9 - paso 1: ¿el comentario se pide como interno?
        // Boolean.TRUE.equals(x) es seguro frente a null: devuelve true solo si x vale exactamente true.
        boolean interno = Boolean.TRUE.equals(dto.getInterno());
        // Ejercicio 9 - paso 2: regla de rol. Un USER (solicitante) NO puede crear notas internas -> 403 Forbidden.
        // (Que el usuario tenga acceso al ticket ya se validó arriba con validateUserAccess.)
        if (interno && user.getRole() == Role.USER) {
            throw new UnauthorizedTicketActionException("Solo el personal de soporte puede crear comentarios internos");
        }

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(user)
                .content(dto.getContent().trim())
                .interno(interno) // Ejercicio 9 - paso 3: se guarda la bandera junto con el comentario
                .build();

        TicketComment savedComment = commentRepository.save(comment);
        log.info("Comentario #{} agregado al ticket #{} por [{}]", savedComment.getId(), ticketId, userEmail);

        return mapToDTO(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByTicketId(Long ticketId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        validateUserAccess(ticket, user, "consultar los comentarios de");

        List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        // Ejercicio 9 - lectura: filtro de visibilidad según el rol de quien consulta.
        // El filtro se aplica en el SERVIDOR (no en el frontend): así un USER nunca recibe los datos internos,
        // ni siquiera inspeccionando la respuesta con las herramientas del navegador.
        boolean ocultarInternos = user.getRole() == Role.USER;
        return comments.stream()
                // Se conserva el comentario si NO hay que ocultar internos O si el comentario no es interno.
                // Es decir, solo se descarta cuando (ocultarInternos = true) Y (el comentario es interno).
                .filter(c -> !(ocultarInternos && c.isInterno()))
                .map(this::mapToDTO)
                .toList();
    }

    private void validateUserAccess(Ticket ticket, User user, String actionDescription) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (user.getRole() == Role.USER) {
            if (ticket.getSolicitante() == null || !ticket.getSolicitante().getId().equals(user.getId())) {
                throw new UnauthorizedTicketActionException("No tiene permisos para " + actionDescription + " esta incidencia");
            }
            return;
        }

        if (user.getRole() == Role.TECHNICIAN) {
            if (ticket.getTecnico() == null || !ticket.getTecnico().getId().equals(user.getId())) {
                throw new UnauthorizedTicketActionException("Solo el técnico asignado puede " + actionDescription + " esta incidencia");
            }
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + id));
    }

    private CommentResponseDTO mapToDTO(TicketComment comment) {
        User author = comment.getAuthor();
        UserResponseDTO authorDto = author != null ? UserResponseDTO.builder()
                .id(author.getId())
                .name(author.getName())
                .email(author.getEmail())
                .role(author.getRole())
                .build() : null;

        return CommentResponseDTO.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .content(comment.getContent())
                .interno(comment.isInterno()) // Ejercicio 9: el DTO informa si la nota es interna
                .author(authorDto)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
