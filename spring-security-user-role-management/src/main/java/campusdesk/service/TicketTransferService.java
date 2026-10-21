package campusdesk.service;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TransferTicketDTO;

/**
 * Ejercicio 3: transferencia de tickets entre categorías/áreas.
 */
public interface TicketTransferService {

    /** Cambia la categoría, desvincula al técnico, vuelve a ABIERTA y deja una nota automática en los comentarios. */
    TicketExtendedResponseDTO transferTicket(Long ticketId, TransferTicketDTO dto, String userEmail);
}
