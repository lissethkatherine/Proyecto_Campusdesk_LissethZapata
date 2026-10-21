package campusdesk.controller;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.service.TicketTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ejercicio 1: endpoints de etiquetas (tags) y categorización dinámica.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Etiquetas", description = "Etiquetado dinámico de incidencias (Ejercicio 1)")
@SecurityRequirement(name = "bearerAuth")
public class TicketTagController {

    private final TicketTagService ticketTagService;

    @Operation(summary = "Asociar etiquetas a un ticket",
            description = "Recibe una lista de nombres, por ejemplo [\"URGENTE\",\"VPN\"]. Crea las que no existan. Solo ADMIN o TECHNICIAN asignado")
    @PostMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    public ResponseEntity<TicketExtendedResponseDTO> addTags(
            @PathVariable Long id,
            @RequestBody List<String> nombres,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketTagService.addTags(id, nombres, principal.getName()));
    }

    @Operation(summary = "Buscar tickets por etiqueta", description = "Respeta la visibilidad por rol (USER, TECHNICIAN, ADMIN)")
    @GetMapping("/por-tag")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketExtendedResponseDTO>> findByTag(
            @RequestParam String nombre,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketTagService.findTicketsByTag(nombre, principal.getName()));
    }
}
