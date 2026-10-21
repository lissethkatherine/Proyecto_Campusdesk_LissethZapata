package campusdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ejercicio 4: tiempo total acumulado de un ticket (SUM en BD) y detalle de los registros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTiempoTotalDTO {

    private Long ticketId;

    private long totalMinutos;

    private List<WorklogResponseDTO> registros;
}
