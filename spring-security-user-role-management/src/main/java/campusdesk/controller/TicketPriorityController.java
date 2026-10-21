package campusdesk.controller;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.service.TicketPriorityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ejercicio 6: endpoints de escalamiento de prioridad.
 * El controlador NO contiene lógica de negocio: solo recibe la petición HTTP, delega en el servicio y devuelve la respuesta.
 */
@RestController // Controlador REST: lo que devuelven los métodos se convierte a JSON automáticamente
@RequestMapping("/api/tickets") // Prefijo común de todas las rutas de esta clase
@RequiredArgsConstructor // Constructor con el servicio inyectado
@Tag(name = "Prioridad", description = "Escalamiento de prioridad (Ejercicio 6)") // Agrupa estos endpoints en Swagger
@SecurityRequirement(name = "bearerAuth") // Swagger muestra el candado: estos endpoints piden token JWT
public class TicketPriorityController {

    // El servicio con la lógica (se inyecta por constructor)
    private final TicketPriorityService ticketPriorityService;

    @Operation(summary = "Escalar la prioridad un nivel",
            description = "BAJA→MEDIA→ALTA→CRITICA. ADMIN o TECHNICIAN asignado. CRITICA ya no se puede escalar (400)")
    @PatchMapping("/{id}/escalar") // PATCH = modificación parcial del recurso (solo cambia un campo)
    // Primera barrera de seguridad: solo ADMIN o TECHNICIAN llegan al método; la segunda (técnico ASIGNADO) está en el servicio
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<TicketExtendedResponseDTO> escalar(
            @PathVariable Long id, // toma el {id} de la URL
            Principal principal // usuario autenticado según el JWT; getName() devuelve su correo
    ) {
        // 200 OK con el ticket actualizado en el cuerpo
        return ResponseEntity.ok(ticketPriorityService.escalarPrioridad(id, principal.getName()));
    }

    @Operation(summary = "Listar tickets críticos activos", description = "Respeta la visibilidad por rol")
    @GetMapping("/criticos") // ruta literal: Spring la prefiere sobre "/{id}", por eso no hay conflicto
    @PreAuthorize("isAuthenticated()") // cualquier usuario con sesión válida; el servicio filtra qué ve cada rol
    public ResponseEntity<List<TicketExtendedResponseDTO>> criticos(Principal principal) {
        return ResponseEntity.ok(ticketPriorityService.findCriticalTickets(principal.getName()));
    }
}
