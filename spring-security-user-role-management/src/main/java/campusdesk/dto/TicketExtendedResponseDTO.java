package campusdesk.dto;

import campusdesk.entity.TicketCategory;
import campusdesk.entity.TicketPriority;
import campusdesk.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO de ticket con los campos de los ejercicios de examen: etiquetas y contador de reaperturas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketExtendedResponseDTO {

    private Long id;

    private String title;

    private String description;

    private UserResponseDTO solicitante;

    private UserResponseDTO tecnico;

    private TicketStatus status;

    private TicketPriority priority;

    private TicketCategory category;

    private List<String> tags;

    private int reaperturasCount;

    // Extra 3: fecha límite de atención según el SLA de la prioridad
    private Instant fechaLimite;

    private Instant createdAt;

    private Instant updatedAt;
}
