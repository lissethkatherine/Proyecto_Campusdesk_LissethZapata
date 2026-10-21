package campusdesk.service;

import campusdesk.dto.CommentResponseDTO;
import campusdesk.dto.QuickResponseRequestDTO;
import campusdesk.dto.QuickResponseResponseDTO;

import java.util.List;

/**
 * Ejercicio 5: plantillas de solución rápida de cada técnico.
 */
public interface QuickResponseService {

    QuickResponseResponseDTO create(QuickResponseRequestDTO dto, String userEmail);

    List<QuickResponseResponseDTO> listMine(String userEmail);

    QuickResponseResponseDTO update(Long plantillaId, QuickResponseRequestDTO dto, String userEmail);

    void delete(Long plantillaId, String userEmail);

    /** Publica el contenido de la plantilla como comentario en un ticket asignado y EN_PROCESO. */
    CommentResponseDTO applyToTicket(Long ticketId, Long plantillaId, String userEmail);
}
