package campusdesk.controller;

import campusdesk.dto.DashboardMetricsDTO;
import campusdesk.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Controlador REST para métricas e indicadores de gestión del Dashboard (RF-07).
 */
@RestController
@RequestMapping({"/api/dashboard", "/dashboard"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Métricas e indicadores globales y contextuales de soporte")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtener métricas consolidadas del panel de control", description = "Calcula métricas según el rol: Globales para ADMIN, asignadas para TECHNICIAN, personales para USER")
    @GetMapping("/metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardMetricsDTO> getMetrics(Principal principal) {
        log.info("Consultando métricas de dashboard para [{}]", principal.getName());
        DashboardMetricsDTO metrics = dashboardService.getMetrics(principal.getName());
        return ResponseEntity.ok(metrics);
    }
}
