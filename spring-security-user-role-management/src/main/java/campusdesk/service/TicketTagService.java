package campusdesk.service;

import campusdesk.dto.TicketExtendedResponseDTO;

import java.util.List;

/**
 * Ejercicio 1: etiquetado dinámico de incidencias.
 */
public interface TicketTagService {

    /** Asocia una o varias etiquetas a un ticket (crea las que no existan). Solo ADMIN o TECHNICIAN asignado. */
    TicketExtendedResponseDTO addTags(Long ticketId, List<String> nombres, String userEmail);

    /** Lista los tickets de una etiqueta respetando la visibilidad del rol. */
    List<TicketExtendedResponseDTO> findTicketsByTag(String nombre, String userEmail);
}
