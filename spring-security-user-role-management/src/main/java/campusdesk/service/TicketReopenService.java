package campusdesk.service;

import campusdesk.dto.ReopenTicketDTO;
import campusdesk.dto.TicketExtendedResponseDTO;

import java.util.List;

/**
 * Ejercicio 2: reapertura de tickets RESUELTA dentro de una ventana de 48 horas.
 */
public interface TicketReopenService {

    /** RESUELTA -> EN_PROCESO por el solicitante, dentro de las 48 horas posteriores a la resolución. */
    TicketExtendedResponseDTO reopenTicket(Long ticketId, ReopenTicketDTO dto, String userEmail);

    /** Tickets con al menos una reapertura (reaperturasCount > 0). */
    List<TicketExtendedResponseDTO> findReopenedTickets();
}
