package campusdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 2: solicitud de reapertura de un ticket RESUELTA (motivo obligatorio, máx. 250 caracteres).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReopenTicketDTO {

    @NotBlank(message = "El motivo de la reapertura es obligatorio")
    @Size(max = 250, message = "El motivo no puede exceder los 250 caracteres")
    private String motivo;
}
