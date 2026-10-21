package campusdesk.service;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TicketSlaDTO;

import java.util.List;

/**
 * Ejercicio 8: control de plazos de atención (SLA) según la prioridad.
 */
public interface TicketSlaService {

    /** Tickets activos cuya fecha límite ya pasó, con la visibilidad del rol de quien consulta. */
    List<TicketExtendedResponseDTO> findOverdueTickets(String userEmail);

    /** Detalle del SLA de un ticket: fecha límite, si venció y cuántas horas quedan. */
    TicketSlaDTO consultarSla(Long ticketId, String userEmail);
}
