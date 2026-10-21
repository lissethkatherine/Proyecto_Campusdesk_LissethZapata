package campusdesk.controller;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TicketSlaDTO;
import campusdesk.service.TicketSlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ejercicio 8: endpoints del SLA (plazos de atención).
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "SLA", description = "Plazos de atención y tickets vencidos (Ejercicio 8)")
@SecurityRequirement(name = "bearerAuth")
public class TicketSlaController {

    private final TicketSlaService ticketSlaService;

    @Operation(summary = "Listar tickets vencidos", description = "Fecha límite pasada y ticket aún activo. Respeta la visibilidad por rol")
    @GetMapping("/vencidos") // ruta literal, no choca con "/{id}"
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketExtendedResponseDTO>> vencidos(Principal principal) {
        return ResponseEntity.ok(ticketSlaService.findOverdueTickets(principal.getName()));
    }

    @Operation(summary = "Consultar el SLA de un ticket", description = "Fecha límite, si venció y horas restantes (negativas si ya venció)")
    @GetMapping("/{id}/sla")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketSlaDTO> sla(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ticketSlaService.consultarSla(id, principal.getName()));
    }
}
