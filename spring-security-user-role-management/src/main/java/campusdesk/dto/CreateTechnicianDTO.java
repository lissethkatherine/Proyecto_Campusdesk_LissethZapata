package campusdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para que un administrador cree una cuenta de técnico.
 * No incluye contraseña ni rol: la contraseña inicial es común para todos los técnicos
 * (configurada por variable de entorno) y el rol siempre es TECHNICIAN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTechnicianDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String name;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe proporcionar un correo con formato válido")
    @Size(max = 150, message = "El correo no puede exceder los 150 caracteres")
    private String email;
}
