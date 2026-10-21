package campusdesk.dto;

import campusdesk.entity.TicketPriority;
import campusdesk.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ejercicio 8: estado del SLA (plazo de atención) de un ticket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSlaDTO {

    private Long ticketId;

    private TicketStatus status;

    private TicketPriority prioridad;

    // Horas que concede el SLA para esa prioridad (CRITICA 4, ALTA 24, MEDIA 48, BAJA 72)
    private int slaHoras;

    private Instant fechaCreacion;

    private Instant fechaLimite;

    // true si ya pasó la fecha límite y el ticket sigue activo
    private boolean vencido;

    // Horas que faltan para vencer; NEGATIVO si ya venció (por ejemplo -5 = vencido hace 5 horas)
    private long horasRestantes;
}
