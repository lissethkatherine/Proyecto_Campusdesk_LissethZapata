package campusdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para que cualquier usuario autenticado (por ejemplo, un técnico) cambie su propia contraseña.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
        message = "La nueva contraseña debe tener entre 8 y 100 caracteres, e incluir al menos una mayúscula, una minúscula y un número"
    )
    private String newPassword;
}
