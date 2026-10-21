package campusdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ejercicio 10: resultado de una ejecución del autocierre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoCloseResultDTO {

    // Cantidad de tickets que se cerraron en esta ejecución
    private int ticketsCerrados;

    // Horas en RESUELTA que se toleran antes de cerrar automáticamente (configurable)
    private long horasLimite;

    // Ids de los tickets cerrados, para poder verificarlos
    private List<Long> ticketIds;
}
