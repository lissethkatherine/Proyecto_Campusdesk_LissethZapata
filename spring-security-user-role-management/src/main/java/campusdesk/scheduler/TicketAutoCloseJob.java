package campusdesk.scheduler;

import campusdesk.dto.AutoCloseResultDTO;
import campusdesk.service.TicketAutoCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejercicio 10: tarea programada que dispara el autocierre automáticamente.
 *
 * Una tarea programada es un método que Spring llama solo, según un calendario. Aquí usamos una expresión CRON
 * de 6 campos:  segundo  minuto  hora  día-del-mes  mes  día-de-la-semana
 *   "0 0 * * * *"  =  en el segundo 0 del minuto 0 de CADA hora  ->  una vez por hora, en punto.
 */
@Component // bean genérico de Spring (no es servicio ni controlador, es una pieza de infraestructura)
@RequiredArgsConstructor
@Slf4j
public class TicketAutoCloseJob {

    // Se llama al SERVICIO, no se escribe lógica aquí: la tarea solo "despierta" al servicio
    private final TicketAutoCloseService autoCloseService;

    // El cron se puede cambiar con la propiedad app.autoclose.cron (por defecto, cada hora en punto)
    @Scheduled(cron = "${app.autoclose.cron:0 0 * * * *}")
    public void ejecutarAutocierre() {
        try {
            // Ejecuta el barrido y obtiene el resumen
            AutoCloseResultDTO resultado = autoCloseService.cerrarResueltosVencidos();
            log.info("Tarea de autocierre finalizada: {} ticket(s) cerrados", resultado.getTicketsCerrados());
        } catch (Exception e) {
            // Se captura cualquier error para que quede registrado y la tarea siga ejecutándose en la próxima hora
            log.error("Falló la tarea de autocierre", e);
        }
    }
}
