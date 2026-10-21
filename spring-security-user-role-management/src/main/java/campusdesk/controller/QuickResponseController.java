package campusdesk.controller;

import campusdesk.dto.CommentResponseDTO;
import campusdesk.dto.QuickResponseRequestDTO;
import campusdesk.dto.QuickResponseResponseDTO;
import campusdesk.service.QuickResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ejercicio 5: plantillas de solución rápida (canned responses) de los técnicos.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Plantillas", description = "Plantillas de solución rápida del técnico (Ejercicio 5)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TECHNICIAN')")
public class QuickResponseController {

    private final QuickResponseService quickResponseService;

    @Operation(summary = "Crear plantilla para el técnico autenticado", description = "El título debe ser único por técnico")
    @PostMapping("/api/tecnicos/plantillas")
    public ResponseEntity<QuickResponseResponseDTO> create(
            @Valid @RequestBody QuickResponseRequestDTO dto,
            Principal principal
    ) {
        QuickResponseResponseDTO response = quickResponseService.create(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar mis plantillas")
    @GetMapping("/api/tecnicos/plantillas")
    public ResponseEntity<List<QuickResponseResponseDTO>> listMine(Principal principal) {
        return ResponseEntity.ok(quickResponseService.listMine(principal.getName()));
    }

    @Operation(summary = "Editar una de mis plantillas")
    @PutMapping("/api/tecnicos/plantillas/{id}")
    public ResponseEntity<QuickResponseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody QuickResponseRequestDTO dto,
            Principal principal
    ) {
        return ResponseEntity.ok(quickResponseService.update(id, dto, principal.getName()));
    }

    @Operation(summary = "Eliminar una de mis plantillas")
    @DeleteMapping("/api/tecnicos/plantillas/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        quickResponseService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Aplicar una plantilla como comentario",
            description = "La plantilla debe ser del técnico autenticado, el ticket debe estar asignado a él y EN_PROCESO")
    @PostMapping("/api/tickets/{id}/aplicar-plantilla/{plantillaId}")
    public ResponseEntity<CommentResponseDTO> applyTemplate(
            @PathVariable Long id,
            @PathVariable Long plantillaId,
            Principal principal
    ) {
        CommentResponseDTO response = quickResponseService.applyToTicket(id, plantillaId, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
