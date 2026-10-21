package campusdesk.service.impl;

import campusdesk.dto.CommentResponseDTO;
import campusdesk.dto.QuickResponseRequestDTO;
import campusdesk.dto.QuickResponseResponseDTO;
import campusdesk.entity.QuickResponse;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketComment;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.InvalidStateTransitionException;
import campusdesk.exception.ResourceConflictException;
import campusdesk.exception.ResourceNotFoundException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.QuickResponseRepository;
import campusdesk.repository.TicketCommentRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.QuickResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ejercicio 5: CRUD de plantillas con filtro de pertenencia y aplicación rápida como comentario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuickResponseServiceImpl implements QuickResponseService {

    private final QuickResponseRepository quickResponseRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public QuickResponseResponseDTO create(QuickResponseRequestDTO dto, String userEmail) {
        User tecnico = findUserByEmail(userEmail);
        String titulo = dto.getTitulo().trim();

        // Título único por técnico (409)
        if (quickResponseRepository.existsByTecnicoIdAndTituloIgnoreCase(tecnico.getId(), titulo)) {
            throw new ResourceConflictException("Ya existe una plantilla con el título '" + titulo + "'");
        }

        QuickResponse saved = quickResponseRepository.save(QuickResponse.builder()
                .titulo(titulo)
                .contenido(dto.getContenido().trim())
                .categoria(dto.getCategoria())
                .tecnico(tecnico)
                .build());

        log.info("Plantilla #{} creada por el técnico [{}]", saved.getId(), userEmail);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuickResponseResponseDTO> listMine(String userEmail) {
        User tecnico = findUserByEmail(userEmail);
        return quickResponseRepository.findByTecnicoIdOrderByTituloAsc(tecnico.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public QuickResponseResponseDTO update(Long plantillaId, QuickResponseRequestDTO dto, String userEmail) {
        User tecnico = findUserByEmail(userEmail);
        QuickResponse plantilla = findOwnedTemplate(plantillaId, tecnico);
        String titulo = dto.getTitulo().trim();

        if (quickResponseRepository.existsByTecnicoIdAndTituloIgnoreCaseAndIdNot(tecnico.getId(), titulo, plantillaId)) {
            throw new ResourceConflictException("Ya existe una plantilla con el título '" + titulo + "'");
        }

        plantilla.setTitulo(titulo);
        plantilla.setContenido(dto.getContenido().trim());
        plantilla.setCategoria(dto.getCategoria());
        return toResponse(quickResponseRepository.save(plantilla));
    }

    @Override
    @Transactional
    public void delete(Long plantillaId, String userEmail) {
        User tecnico = findUserByEmail(userEmail);
        QuickResponse plantilla = findOwnedTemplate(plantillaId, tecnico);
        quickResponseRepository.delete(plantilla);
        log.info("Plantilla #{} eliminada por el técnico [{}]", plantillaId, userEmail);
    }

    @Override
    @Transactional
    public CommentResponseDTO applyToTicket(Long ticketId, Long plantillaId, String userEmail) {
        User tecnico = findUserByEmail(userEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // La plantilla debe existir (404) y pertenecer al técnico autenticado (403)
        QuickResponse plantilla = findOwnedTemplate(plantillaId, tecnico);

        // El técnico debe tener asignado el ticket (403)
        if (ticket.getTecnico() == null || !ticket.getTecnico().getId().equals(tecnico.getId())) {
            throw new UnauthorizedTicketActionException("Solo el técnico asignado puede responder en este ticket");
        }

        // El ticket debe estar EN_PROCESO (409)
        if (ticket.getStatus() != TicketStatus.EN_PROCESO) {
            throw new InvalidStateTransitionException(
                    "Solo se pueden aplicar plantillas en tickets EN_PROCESO (estado actual: " + ticket.getStatus() + ")");
        }

        TicketComment saved = ticketCommentRepository.save(TicketComment.builder()
                .ticket(ticket)
                .author(tecnico)
                .content(plantilla.getContenido())
                .build());

        log.info("Plantilla #{} aplicada al ticket #{} por [{}]", plantillaId, ticketId, userEmail);
        return CommentResponseDTO.builder()
                .id(saved.getId())
                .ticketId(ticketId)
                .content(saved.getContent())
                .author(ticketMapper.toUserDto(tecnico))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // Filtro de pertenencia: un técnico solo puede ver o modificar sus propias plantillas
    private QuickResponse findOwnedTemplate(Long plantillaId, User tecnico) {
        QuickResponse plantilla = quickResponseRepository.findById(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con ID: " + plantillaId));
        if (!plantilla.getTecnico().getId().equals(tecnico.getId())) {
            throw new UnauthorizedTicketActionException("La plantilla no pertenece al técnico autenticado");
        }
        return plantilla;
    }

    private QuickResponseResponseDTO toResponse(QuickResponse plantilla) {
        return QuickResponseResponseDTO.builder()
                .id(plantilla.getId())
                .titulo(plantilla.getTitulo())
                .contenido(plantilla.getContenido())
                .categoria(plantilla.getCategoria())
                .tecnicoId(plantilla.getTecnico().getId())
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
