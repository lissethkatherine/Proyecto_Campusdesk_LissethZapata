package campusdesk.mapper;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.Tag;
import campusdesk.entity.Ticket;
import campusdesk.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte entidades en DTOs para los endpoints de los ejercicios de examen.
 * Debe usarse dentro de una transacción (las etiquetas y usuarios son LAZY).
 */
@Component
public class TicketMapper {

    public TicketExtendedResponseDTO toExtended(Ticket ticket) {
        List<String> tags = ticket.getTags() == null
                ? List.of()
                : ticket.getTags().stream().map(Tag::getNombre).sorted().toList();

        return TicketExtendedResponseDTO.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .solicitante(toUserDto(ticket.getSolicitante()))
                .tecnico(toUserDto(ticket.getTecnico()))
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .tags(tags)
                .reaperturasCount(ticket.getReaperturasCount())
                .fechaLimite(ticket.getFechaLimite())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public UserResponseDTO toUserDto(User user) {
        if (user == null) {
            return null;
        }
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
