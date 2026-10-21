package campusdesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ejercicio 4: registro de un bloque de tiempo de trabajo (5 a 480 minutos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorklogDTO {

    @NotNull(message = "Los minutos dedicados son obligatorios")
    @Min(value = 5, message = "El mínimo por registro es de 5 minutos")
    @Max(value = 480, message = "El máximo por registro es de 480 minutos (8 horas)")
    private Integer minutosDedicados;

    @NotBlank(message = "La descripción de la actividad es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcionActividad;
}
