package campusdesk.service;

import campusdesk.dto.AutoCloseResultDTO;

/**
 * Ejercicio 10: cierre automático de tickets que llevan demasiado tiempo en RESUELTA.
 */
public interface TicketAutoCloseService {

    /** Cierra (CERRADA) todo ticket RESUELTA cuya resolución superó el plazo configurado. Devuelve un resumen. */
    AutoCloseResultDTO cerrarResueltosVencidos();
}
