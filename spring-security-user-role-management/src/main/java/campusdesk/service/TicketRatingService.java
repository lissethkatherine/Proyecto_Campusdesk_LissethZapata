package campusdesk.service;

import campusdesk.dto.RateTicketDTO;
import campusdesk.dto.RatingResponseDTO;
import campusdesk.dto.TecnicoCalificacionDTO;

/**
 * Ejercicio 7: calificación del servicio recibido.
 */
public interface TicketRatingService {

    /** El solicitante califica (1 a 5) su ticket CERRADA. Solo una vez por ticket. */
    RatingResponseDTO calificar(Long ticketId, RateTicketDTO dto, String userEmail);

    /** Promedio y cantidad de calificaciones de un técnico. Lo ve un ADMIN o el propio técnico. */
    TecnicoCalificacionDTO obtenerCalificacionTecnico(Long tecnicoId, String userEmail);
}
