package campusdesk.dto;

import campusdesk.entity.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 5: datos para crear o editar una plantilla de respuesta rápida.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickResponseRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100, message = "El título no puede exceder los 100 caracteres")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    @NotNull(message = "La categoría es obligatoria")
    private TicketCategory categoria;
}
