package campusdesk.entity;

/**
 * Prioridad de una incidencia.
 * Cada nivel define las horas máximas de atención (SLA): a mayor urgencia, menor plazo (Extra 3).
 * El orden de declaración importa: sirve para "escalar" al siguiente nivel con ordinal() (Extra 1).
 */
public enum TicketPriority {
    BAJA(72),
    MEDIA(48),
    ALTA(24),
    CRITICA(4);

    private final int slaHoras;

    TicketPriority(int slaHoras) {
        this.slaHoras = slaHoras;
    }

    /** Horas máximas para atender un ticket de esta prioridad. */
    public int getSlaHoras() {
        return slaHoras;
    }

    /** Siguiente nivel de urgencia. CRITICA es el tope y se devuelve a sí misma. */
    public TicketPriority siguiente() {
        return this == CRITICA ? this : values()[ordinal() + 1];
    }
}
