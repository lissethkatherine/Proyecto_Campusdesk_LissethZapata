package campusdesk.dto;

import campusdesk.entity.TicketCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 5: plantilla de respuesta rápida expuesta por la API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickResponseResponseDTO {

    private Long id;

    private String titulo;

    private String contenido;

    private TicketCategory categoria;

    private Long tecnicoId;
}
