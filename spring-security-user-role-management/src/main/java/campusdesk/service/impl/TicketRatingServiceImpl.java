package campusdesk.service.impl;

import campusdesk.dto.RateTicketDTO;
import campusdesk.dto.RatingResponseDTO;
import campusdesk.dto.TecnicoCalificacionDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketRating;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.exception.InvalidStateTransitionException;
import campusdesk.exception.ResourceConflictException;
import campusdesk.exception.ResourceNotFoundException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.repository.TicketRatingRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketRatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ejercicio 7: lógica de calificación del servicio.
 *
 * Orden de validaciones (el mismo en todos los ejercicios): existe (404) -> permiso (403) -> estado (409) -> datos (400).
 * Así el cliente recibe siempre el error más "fundamental" primero.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketRatingServiceImpl implements TicketRatingService {

    private final TicketRatingRepository ratingRepository; // calificaciones
    private final TicketRepository ticketRepository; // tickets
    private final UserRepository userRepository; // usuarios

    @Override
    @Transactional // validar + guardar en una sola unidad atómica
    public RatingResponseDTO calificar(Long ticketId, RateTicketDTO dto, String userEmail) {
        // Quién califica (viene del token)
        User user = findUserByEmail(userEmail);

        // 404 si el ticket no existe
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // 403: solo el solicitante (quien creó el ticket) puede calificarlo. Se compara por id, no por objeto.
        if (ticket.getSolicitante() == null || !ticket.getSolicitante().getId().equals(user.getId())) {
            throw new UnauthorizedTicketActionException("Solo el solicitante del ticket puede calificar la atención");
        }

        // 409: solo se califica cuando la atención terminó del todo (CERRADA)
        if (ticket.getStatus() != TicketStatus.CERRADA) {
            throw new InvalidStateTransitionException(
                    "Solo se puede calificar un ticket CERRADA (estado actual: " + ticket.getStatus() + ")");
        }

        // 409: una única calificación por ticket. La consulta existsBy... es barata (COUNT > 0)
        if (ratingRepository.existsByTicketId(ticketId)) {
            throw new ResourceConflictException("Este ticket ya fue calificado");
        }

        // Normalizamos el comentario: null o vacío se guarda como null; si trae texto se recortan los espacios
        String comentario = (dto.getComentario() == null || dto.getComentario().isBlank())
                ? null
                : dto.getComentario().trim();

        // Construimos y guardamos la calificación. La fecha la pone el callback @PrePersist de la entidad.
        TicketRating saved = ratingRepository.save(TicketRating.builder()
                .ticket(ticket) // a qué ticket pertenece
                .solicitante(user) // quién la dejó
                .puntaje(dto.getPuntaje()) // 1..5 (ya validado por @Valid en el controlador)
                .comentario(comentario) // opcional
                .build());

        log.info("Ticket #{} calificado con {} por [{}]", ticketId, saved.getPuntaje(), userEmail);

        // Respondemos con un DTO
        return RatingResponseDTO.builder()
                .id(saved.getId())
                .ticketId(ticketId)
                .puntaje(saved.getPuntaje())
                .comentario(saved.getComentario())
                .fecha(saved.getFecha())
                .build();
    }

    @Override
    @Transactional(readOnly = true) // solo lectura
    public TecnicoCalificacionDTO obtenerCalificacionTecnico(Long tecnicoId, String userEmail) {
        // Quién consulta
        User user = findUserByEmail(userEmail);

        // Permiso (403): un ADMIN ve a cualquier técnico; un técnico solo se ve a sí mismo
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean esElMismoTecnico = user.getRole() == Role.TECHNICIAN && user.getId().equals(tecnicoId);
        if (!isAdmin && !esElMismoTecnico) {
            throw new UnauthorizedTicketActionException("No tiene permisos para consultar la calificación de este técnico");
        }

        // 404 si el id no existe o no pertenece a un técnico (filter descarta usuarios con otro rol)
        User tecnico = userRepository.findById(tecnicoId)
                .filter(u -> u.getRole() == Role.TECHNICIAN)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico no encontrado con ID: " + tecnicoId));

        // AVG puede devolver null si no hay calificaciones; por eso el tipo es Double (objeto)
        Double promedio = ratingRepository.promedioPorTecnico(tecnicoId);
        // COUNT nunca es null
        long total = ratingRepository.contarPorTecnico(tecnicoId);

        // Redondeo a 2 decimales: Math.round(4.3333 * 100) = 433 -> 433 / 100.0 = 4.33
        double promedioRedondeado = promedio == null ? 0.0 : Math.round(promedio * 100.0) / 100.0;

        return TecnicoCalificacionDTO.builder()
                .tecnicoId(tecnico.getId())
                .tecnicoNombre(tecnico.getName())
                .promedio(promedioRedondeado)
                .totalCalificaciones(total)
                .build();
    }

    // Auxiliar: busca el usuario por correo o lanza error
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
