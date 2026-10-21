package campusdesk.service;

import campusdesk.dto.CreateWorklogDTO;
import campusdesk.dto.TicketTiempoTotalDTO;
import campusdesk.dto.WorklogResponseDTO;

/**
 * Ejercicio 4: bitácora de tiempo técnico.
 */
public interface TicketWorklogService {

    /** Registra un bloque de tiempo. Solo el TECHNICIAN asignado y con el ticket EN_PROCESO. */
    WorklogResponseDTO registrarWorklog(Long ticketId, CreateWorklogDTO dto, String userEmail);

    /** Total de minutos (SUM en BD) y detalle de registros de un ticket. */
    TicketTiempoTotalDTO obtenerTiempoTotal(Long ticketId, String userEmail);
}
