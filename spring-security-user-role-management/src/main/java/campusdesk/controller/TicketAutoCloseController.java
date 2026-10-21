package campusdesk.controller;

import campusdesk.dto.AutoCloseResultDTO;
import campusdesk.service.TicketAutoCloseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ejercicio 10: disparo manual del autocierre (útil para probarlo sin esperar a la próxima hora en punto).
 */
@RestController
@RequestMapping("/api/tickets/autocierre")
@RequiredArgsConstructor
@Tag(name = "Autocierre", description = "Cierre automático de tickets RESUELTA (Ejercicio 10)")
@SecurityRequirement(name = "bearerAuth")
public class TicketAutoCloseController {

    private final TicketAutoCloseService autoCloseService;

    @Operation(summary = "Ejecutar el autocierre ahora", description = "Solo ADMIN. Cierra los tickets RESUELTA que superaron el plazo configurado")
    @PostMapping("/ejecutar") // POST porque la acción CAMBIA datos (un GET nunca debe modificar nada)
    @PreAuthorize("hasRole('ADMIN')") // solo administradores
    public ResponseEntity<AutoCloseResultDTO> ejecutar() {
        return ResponseEntity.ok(autoCloseService.cerrarResueltosVencidos());
    }
}
