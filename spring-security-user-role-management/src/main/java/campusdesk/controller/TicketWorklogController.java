package campusdesk.controller;

import campusdesk.dto.CreateWorklogDTO;
import campusdesk.dto.TicketTiempoTotalDTO;
import campusdesk.dto.WorklogResponseDTO;
import campusdesk.service.TicketWorklogService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Ejercicio 4: bitácora de registro de tiempo técnico (worklog).
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Worklog", description = "Registro de tiempo técnico (Ejercicio 4)")
@SecurityRequirement(name = "bearerAuth")
public class TicketWorklogController {

    private final TicketWorklogService ticketWorklogService;

    @Operation(summary = "Registrar un bloque de tiempo de trabajo",
            description = "Solo el TECHNICIAN asignado, con el ticket EN_PROCESO y entre 5 y 480 minutos")
    @PostMapping("/{id}/worklogs")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<WorklogResponseDTO> registrarWorklog(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorklogDTO dto,
            Principal principal
    ) {
        WorklogResponseDTO response = ticketWorklogService.registrarWorklog(id, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar tiempo total acumulado de un ticket",
            description = "Total de minutos calculado con SUM en el repositorio y detalle de los registros")
    @GetMapping("/{id}/tiempo-total")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketTiempoTotalDTO> obtenerTiempoTotal(
            @PathVariable Long id,
            Principal principal
    ) {
        return ResponseEntity.ok(ticketWorklogService.obtenerTiempoTotal(id, principal.getName()));
    }
}
