package campusdesk.controller;

import campusdesk.dto.ReopenTicketDTO;
import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.service.TicketReopenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ejercicio 2: reapertura de tickets resueltos.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Reapertura", description = "Reapertura de tickets RESUELTA dentro de 48 horas (Ejercicio 2)")
@SecurityRequirement(name = "bearerAuth")
public class TicketReopenController {

    private final TicketReopenService ticketReopenService;

    @Operation(summary = "Reabrir un ticket RESUELTA",
            description = "Solo el solicitante, dentro de las 48 horas posteriores a la resolución. Pasa a EN_PROCESO")
    @PostMapping("/{id}/reabrir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketExtendedResponseDTO> reopen(
            @PathVariable Long id,
            @Valid @RequestBody ReopenTicketDTO dto,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketReopenService.reopenTicket(id, dto, principal.getName()));
    }

    @Operation(summary = "Listar tickets reabiertos", description = "Exclusivo para ADMIN: tickets con reaperturasCount > 0")
    @GetMapping("/reabiertos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketExtendedResponseDTO>> findReopened() {
        return ResponseEntity.ok(ticketReopenService.findReopenedTickets());
    }
}
