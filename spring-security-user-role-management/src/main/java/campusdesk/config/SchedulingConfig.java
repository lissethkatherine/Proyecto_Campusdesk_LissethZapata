package campusdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ejercicio 10: activa el soporte de tareas programadas de Spring.
 * Sin @EnableScheduling las anotaciones @Scheduled se ignoran en silencio y la tarea nunca se ejecuta.
 */
@Configuration // clase de configuración: Spring la procesa al arrancar
@EnableScheduling // habilita el planificador interno que ejecuta los métodos @Scheduled
public class SchedulingConfig {
    // No necesita código: la anotación hace todo el trabajo
}
