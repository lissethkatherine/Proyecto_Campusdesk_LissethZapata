package campusdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO que representa un comentario dentro del hilo de una incidencia (RF-05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    private Long id;

    private Long ticketId;

    private String content;

    // Ejercicio 9: indica si es una nota interna del personal de soporte (el front puede mostrarla con otro estilo)
    private boolean interno;

    private UserResponseDTO author;

    private Instant createdAt;
}
