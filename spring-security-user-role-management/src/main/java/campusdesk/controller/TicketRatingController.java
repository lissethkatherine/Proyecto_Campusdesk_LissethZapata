package campusdesk.controller;

import campusdesk.dto.RateTicketDTO;
import campusdesk.dto.RatingResponseDTO;
import campusdesk.dto.TecnicoCalificacionDTO;
import campusdesk.service.TicketRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Ejercicio 7: endpoints de calificación del servicio.
 * No lleva @RequestMapping de clase porque sus dos rutas tienen prefijos distintos (/api/tickets y /api/tecnicos).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Calificaciones", description = "Calificación del servicio (Ejercicio 7)")
@SecurityRequirement(name = "bearerAuth")
public class TicketRatingController {

    private final TicketRatingService ticketRatingService;

    @Operation(summary = "Calificar un ticket CERRADA", description = "Solo el solicitante, una vez por ticket, puntaje de 1 a 5")
    @PostMapping("/api/tickets/{id}/calificar")
    @PreAuthorize("isAuthenticated()") // el permiso fino (¿es el solicitante?) lo valida el servicio
    public ResponseEntity<RatingResponseDTO> calificar(
            @PathVariable Long id,
            @Valid @RequestBody RateTicketDTO dto, // @Valid activa @NotNull/@Min/@Max del DTO -> 400 si fallan
            Principal principal
    ) {
        // 201 Created: se creó un recurso nuevo (la calificación)
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketRatingService.calificar(id, dto, principal.getName()));
    }

    @Operation(summary = "Promedio de calificaciones de un técnico", description = "ADMIN o el propio técnico")
    @GetMapping("/api/tecnicos/{id}/calificacion")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')") // un USER normal no tiene acceso
    public ResponseEntity<TecnicoCalificacionDTO> calificacionTecnico(
            @PathVariable Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketRatingService.obtenerCalificacionTecnico(id, principal.getName()));
    }
}
