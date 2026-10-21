package campusdesk.service;

import campusdesk.dto.TicketExtendedResponseDTO;

import java.util.List;

/**
 * Ejercicio 6: escalamiento de prioridad de una incidencia.
 * La interfaz define el "contrato" (qué se puede hacer); la lógica real vive en service/impl.
 */
public interface TicketPriorityService {

    /** Sube la prioridad un nivel (BAJA -> MEDIA -> ALTA -> CRITICA). Solo ADMIN o TECHNICIAN asignado. */
    TicketExtendedResponseDTO escalarPrioridad(Long ticketId, String userEmail);

    /** Lista los tickets CRITICA todavía activos, respetando la visibilidad del rol de quien consulta. */
    List<TicketExtendedResponseDTO> findCriticalTickets(String userEmail);
}
