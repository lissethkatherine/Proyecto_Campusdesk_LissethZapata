package campusdesk.controller;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.dto.TransferTicketDTO;
import campusdesk.service.TicketTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Ejercicio 3: transferencia de tickets entre departamentos.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Transferencia", description = "Transferencia de tickets entre áreas (Ejercicio 3)")
@SecurityRequirement(name = "bearerAuth")
public class TicketTransferController {

    private final TicketTransferService ticketTransferService;

    @Operation(summary = "Transferir un ticket a otra categoría",
            description = "ADMIN o TECHNICIAN asignado. Desvincula al técnico, vuelve a ABIERTA y deja una nota del sistema")
    @PutMapping("/{id}/transferir")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<TicketExtendedResponseDTO> transfer(
            @PathVariable Long id,
            @Valid @RequestBody TransferTicketDTO dto,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketTransferService.transferTicket(id, dto, principal.getName()));
    }
}
