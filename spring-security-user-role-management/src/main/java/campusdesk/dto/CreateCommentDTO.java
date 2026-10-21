package campusdesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para agregar un nuevo comentario a una incidencia (RF-05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDTO {

    @NotBlank(message = "El contenido del comentario no puede estar vacío")
    private String content;

    // Ejercicio 9: true = nota interna visible solo para el personal de soporte (opcional).
    // Se usa Boolean (objeto) y no boolean (primitivo): si el cliente no envía el campo llega null y se interpreta como false.
    private Boolean interno;

    /**
     * Constructor de un solo argumento. @AllArgsConstructor ahora genera uno de dos parámetros (content, interno),
     * pero las pruebas existentes hacen new CreateCommentDTO("texto"); este constructor extra las mantiene funcionando.
     */
    public CreateCommentDTO(String content) {
        this.content = content;
    }
}
