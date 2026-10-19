package campusdesk.service;

import campusdesk.dto.DashboardMetricsDTO;

/**
 * Contrato de servicio para el cómputo de métricas del panel de control (RF-07).
 */
public interface DashboardService {

    DashboardMetricsDTO getMetrics(String userEmail);
}
