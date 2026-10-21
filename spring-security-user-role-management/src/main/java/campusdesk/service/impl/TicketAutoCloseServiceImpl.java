package campusdesk.service.impl;

import campusdesk.dto.AutoCloseResultDTO;
import campusdesk.entity.Ticket;
import campusdesk.entity.TicketHistory;
import campusdesk.entity.TicketStatus;
import campusdesk.repository.TicketHistoryRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.service.TicketAutoCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejercicio 10: lógica del autocierre.
 *
 * Por qué es un servicio aparte y no código dentro de la tarea programada: así el MISMO método lo usan
 * (1) el planificador automático y (2) el endpoint manual para ADMIN. Además se puede probar sin esperar al reloj.
 */
@Service
@RequiredArgsConstructor // solo incluye los campos final; el campo @Value de abajo NO es final y se inyecta por separado
@Slf4j
public class TicketAutoCloseServiceImpl implements TicketAutoCloseService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    // @Value lee una propiedad de configuración. "${app.autoclose.hours:72}" = usa app.autoclose.hours si existe
    // (por ejemplo como variable de entorno APP_AUTOCLOSE_HOURS en Railway) y, si no, 72. Así se cambia sin recompilar.
    @Value("${app.autoclose.hours:72}")
    private long horasLimite;

    @Override
    // Todo el barrido es UNA transacción: o se cierran todos los que corresponden, o ninguno si algo falla.
    @Transactional
    public AutoCloseResultDTO cerrarResueltosVencidos() {
        // 1) Instante de corte: todo lo resuelto ANTES de este momento ya superó el plazo.
        //    Instant.now().minus(Duration.ofHours(72)) = "hace 72 horas".
        Instant limite = Instant.now().minus(Duration.ofHours(horasLimite));

        // 2) Aquí iremos anotando los ids de los tickets que se cierren
        List<Long> cerrados = new ArrayList<>();

        // 3) Candidatos: solo tickets en RESUELTA (método de consulta que ya existe en el repositorio)
        for (Ticket ticket : ticketRepository.findByStatusOrderByCreatedAtDesc(TicketStatus.RESUELTA)) {

            // 4) ¿Cuándo pasó a RESUELTA? Se lee del historial (última transición a RESUELTA).
            //    Si no hubiera registro (datos antiguos), se usa la fecha de última actualización como alternativa.
            Instant resueltaEn = ticketHistoryRepository
                    .findLastTransitionDate(ticket.getId(), TicketStatus.RESUELTA)
                    .orElse(ticket.getUpdatedAt());

            // 5) Si la resolución es anterior al instante de corte, el plazo se superó: toca cerrar
            if (resueltaEn.isBefore(limite)) {
                // 6) Cambiamos el estado (RESUELTA -> CERRADA es una transición válida de la máquina de estados)
                ticket.setStatus(TicketStatus.CERRADA);
                ticketRepository.save(ticket);

                // 7) Auditoría. TicketHistory exige un autor y no existe un "usuario sistema",
                //    así que se registra al solicitante (quien habría cerrado el ticket), dejando claro en el comentario que fue automático.
                ticketHistoryRepository.save(TicketHistory.builder()
                        .ticket(ticket)
                        .estadoAnterior(TicketStatus.RESUELTA)
                        .estadoNuevo(TicketStatus.CERRADA)
                        .autor(ticket.getSolicitante())
                        .comentarioTransicion("Cierre automático: más de " + horasLimite + " horas en RESUELTA sin respuesta")
                        .build());

                // 8) Anotamos el id para el resumen
                cerrados.add(ticket.getId());
            }
        }

        // 9) Log con el resultado de la ejecución
        log.info("Autocierre ejecutado: {} ticket(s) cerrados (plazo {} h)", cerrados.size(), horasLimite);

        // 10) Resumen para quien lo invoque (el endpoint manual lo devuelve como JSON)
        return AutoCloseResultDTO.builder()
                .ticketsCerrados(cerrados.size())
                .horasLimite(horasLimite)
                .ticketIds(cerrados)
                .build();
    }
}
