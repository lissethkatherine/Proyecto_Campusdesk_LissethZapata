package campusdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 7: resumen de calificaciones de un técnico (promedio calculado con AVG en la BD).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TecnicoCalificacionDTO {

    private Long tecnicoId;

    private String tecnicoNombre;

    // Promedio redondeado a 2 decimales; 0.0 si todavía no tiene calificaciones
    private double promedio;

    private long totalCalificaciones;
}
