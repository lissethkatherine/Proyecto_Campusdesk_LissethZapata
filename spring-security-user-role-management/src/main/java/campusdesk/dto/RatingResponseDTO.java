package campusdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ejercicio 7: calificación tal como se devuelve al cliente (sin exponer la entidad).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDTO {

    private Long id;

    private Long ticketId;

    private Integer puntaje;

    private String comentario;

    private Instant fecha;
}
