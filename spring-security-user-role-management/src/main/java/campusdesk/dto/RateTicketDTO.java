package campusdesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 7: cuerpo de la petición para calificar un ticket.
 * Las anotaciones se validan solas cuando el controlador usa @Valid: si fallan, el handler responde 400.
 */
@Data // getters, setters, equals, hashCode y toString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateTicketDTO {

    @NotNull(message = "El puntaje es obligatorio") // Integer (objeto) permite detectar "no enviado"
    @Min(value = 1, message = "El puntaje mínimo es 1")
    @Max(value = 5, message = "El puntaje máximo es 5")
    private Integer puntaje;

    @Size(max = 250, message = "El comentario no puede exceder los 250 caracteres")
    private String comentario; // opcional
}
