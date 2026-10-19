package campusdesk.service.impl;

import campusdesk.service.DashboardService;
import campusdesk.dto.DashboardMetricsDTO;
import campusdesk.entity.Role;
import campusdesk.entity.TicketStatus;
import campusdesk.entity.User;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del cálculo de métricas de incidencias según el perfil de usuario (RF-07).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsDTO getMetrics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userEmail));

        long abiertas;
        long asignadas;
        long enProceso;
        long resueltas;
        long cerradas;
        long total;

        if (user.getRole() == Role.ADMIN) {
            abiertas = ticketRepository.countByStatus(TicketStatus.ABIERTA);
            asignadas = ticketRepository.countByStatus(TicketStatus.ASIGNADA);
            enProceso = ticketRepository.countByStatus(TicketStatus.EN_PROCESO);
            resueltas = ticketRepository.countByStatus(TicketStatus.RESUELTA);
            cerradas = ticketRepository.countByStatus(TicketStatus.CERRADA);
            total = ticketRepository.count();
        } else if (user.getRole() == Role.TECHNICIAN) {
            abiertas = ticketRepository.countByTecnicoIdAndStatus(user.getId(), TicketStatus.ABIERTA);
            asignadas = ticketRepository.countByTecnicoIdAndStatus(user.getId(), TicketStatus.ASIGNADA);
            enProceso = ticketRepository.countByTecnicoIdAndStatus(user.getId(), TicketStatus.EN_PROCESO);
            resueltas = ticketRepository.countByTecnicoIdAndStatus(user.getId(), TicketStatus.RESUELTA);
            cerradas = ticketRepository.countByTecnicoIdAndStatus(user.getId(), TicketStatus.CERRADA);
            total = ticketRepository.countByTecnicoId(user.getId());
        } else {
            abiertas = ticketRepository.countBySolicitanteIdAndStatus(user.getId(), TicketStatus.ABIERTA);
            asignadas = ticketRepository.countBySolicitanteIdAndStatus(user.getId(), TicketStatus.ASIGNADA);
            enProceso = ticketRepository.countBySolicitanteIdAndStatus(user.getId(), TicketStatus.EN_PROCESO);
            resueltas = ticketRepository.countBySolicitanteIdAndStatus(user.getId(), TicketStatus.RESUELTA);
            cerradas = ticketRepository.countBySolicitanteIdAndStatus(user.getId(), TicketStatus.CERRADA);
            total = ticketRepository.countBySolicitanteId(user.getId());
        }

        return DashboardMetricsDTO.builder()
                .total(total)
                .abiertas(abiertas)
                .asignadas(asignadas)
                .enProceso(enProceso)
                .resueltas(resueltas)
                .cerradas(cerradas)
                .build();
    }
}
