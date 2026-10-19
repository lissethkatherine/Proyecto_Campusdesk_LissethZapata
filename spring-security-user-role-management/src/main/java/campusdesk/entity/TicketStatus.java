package campusdesk.entity;

/**
 * Estados permitidos en el ciclo de vida de una incidencia (RF-04).
 * Flujo normal: ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA.
 * Estado adicional: CANCELADA (el solicitante resolvió el problema por su cuenta y retira la solicitud;
 * es un estado final, igual de inmutable que CERRADA).
 */
public enum TicketStatus {
    ABIERTA,
    ASIGNADA,
    EN_PROCESO,
    RESUELTA,
    CERRADA,
    CANCELADA;

    /** Estados finales: la incidencia ya no admite cambios ni comentarios. */
    public boolean isFinal() {
        return this == CERRADA || this == CANCELADA;
    }
}
