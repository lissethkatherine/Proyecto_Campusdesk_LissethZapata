package campusdesk.service.impl;

import campusdesk.dto.CreateWorklogDTO;
import campusdesk.dto.TicketTiempoTotalDTO;
import campusdesk.dto.WorklogResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.TicketWorklog;
import campusdesk.entity.User;
import campusdesk.exception.InvalidStateTransitionException;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.TicketWorklogRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketWorklogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ejercicio 4: bitácora de tiempo técnico con agregación SUM en el repositorio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketWorklogServiceImpl implements TicketWorklogService {

    private final TicketRepository ticketRepository;
    private final TicketWorklogRepository worklogRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public WorklogResponseDTO registrarWorklog(Long ticketId, CreateWorklogDTO dto, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        // Solo el TECHNICIAN asignado (403). Ni siquiera un ADMIN puede registrar tiempo ajeno.
        if (user.getRole() != Role.TECHNICIAN || !isAssignedTechnician(ticket, user)) {
            throw new UnauthorizedTicketActionException("Solo el técnico asignado puede registrar tiempo en este ticket");
        }

        // El ticket debe estar EN_PROCESO (409)
        if (ticket.getStatus() != TicketStatus.EN_PROCESO) {
            throw new InvalidStateTransitionException(
                    "Solo se puede registrar tiempo en tickets EN_PROCESO (estado actual: " + ticket.getStatus() + ")");
        }

        // Defensa adicional al @Min/@Max del DTO: rango permitido por registro
        int minutos = dto.getMinutosDedicados();
        if (minutos < TicketWorklog.MIN_MINUTOS || minutos > TicketWorklog.MAX_MINUTOS) {
            throw new IllegalArgumentException("Los minutos dedicados deben estar entre "
                    + TicketWorklog.MIN_MINUTOS + " y " + TicketWorklog.MAX_MINUTOS);
        }

        TicketWorklog worklog = TicketWorklog.builder()
                .ticket(ticket)
                .tecnico(user)
                .minutosDedicados(minutos)
                .descripcionActividad(dto.getDescripcionActividad().trim())
                .build();
        TicketWorklog saved = worklogRepository.save(worklog);

        log.info("Worklog de {} min registrado en ticket #{} por [{}]", minutos, ticketId, userEmail);
        return toResponse(saved, ticketId);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketTiempoTotalDTO obtenerTiempoTotal(Long ticketId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = findTicketById(ticketId);

        // Misma visibilidad que el detalle del ticket: ADMIN todo; USER solo el suyo; TECHNICIAN solo asignados
        if (user.getRole() == Role.USER && !isRequester(ticket, user)) {
            throw new UnauthorizedTicketActionException("No tiene permisos para consultar el tiempo de este ticket");
        }
        if (user.getRole() == Role.TECHNICIAN && !isAssignedTechnician(ticket, user)) {
            throw new UnauthorizedTicketActionException("Un técnico solo puede consultar incidencias asignadas a su perfil");
        }

        Long total = worklogRepository.sumMinutosByTicketId(ticketId); // null si no hay registros
        List<WorklogResponseDTO> registros = worklogRepository.findByTicketIdWithTecnico(ticketId).stream()
                .map(w -> toResponse(w, ticketId))
                .toList();

        return TicketTiempoTotalDTO.builder()
                .ticketId(ticketId)
                .totalMinutos(total == null ? 0L : total)
                .registros(registros)
                .build();
    }

    private WorklogResponseDTO toResponse(TicketWorklog worklog, Long ticketId) {
        return WorklogResponseDTO.builder()
                .id(worklog.getId())
                .ticketId(ticketId)
                .minutosDedicados(worklog.getMinutosDedicados())
                .descripcionActividad(worklog.getDescripcionActividad())
                .fechaRegistro(worklog.getFechaRegistro())
                .tecnico(ticketMapper.toUserDto(worklog.getTecnico()))
                .build();
    }

    private boolean isAssignedTechnician(Ticket ticket, User user) {
        return ticket.getTecnico() != null && ticket.getTecnico().getId().equals(user.getId());
    }

    private boolean isRequester(Ticket ticket, User user) {
        return ticket.getSolicitante() != null && ticket.getSolicitante().getId().equals(user.getId());
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
