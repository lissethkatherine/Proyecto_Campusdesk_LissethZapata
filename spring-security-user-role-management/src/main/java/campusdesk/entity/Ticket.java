package campusdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad principal que representa una incidencia o ticket de soporte en CAMPUSDESK (RF-03).
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"solicitante", "tecnico", "tags"})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede exceder los 150 caracteres")
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "El solicitante es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private User solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private User tecnico;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;

    @NotNull(message = "La prioridad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TicketPriority priority;

    @NotNull(message = "La categoría es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TicketCategory category;

    // Ejercicio 1: etiquetas transversales (dueño de la relación ManyToMany)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ticket_tags",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    // Ejercicio 2: cantidad de veces que el solicitante reabrió el ticket
    @Column(name = "reaperturas_count", nullable = false, columnDefinition = "integer default 0")
    private int reaperturasCount;

    // Ejercicio 8: fecha límite de atención (SLA). Se calcula sola: createdAt + horas según la prioridad.
    // Es nullable (sin "nullable = false") para no romper los tickets que ya existen en la base de datos.
    @Column(name = "fecha_limite")
    private Instant fechaLimite;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TicketStatus.ABIERTA;
        }
        recalcularFechaLimite();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        // Si la prioridad cambió (p. ej. al escalar en el Ejercicio 6), el SLA se recalcula automáticamente.
        // Se recalcula SIEMPRE porque la fórmula es pura (solo depende de createdAt y prioridad): no hace falta detectar el cambio.
        recalcularFechaLimite();
    }

    /** SLA = fecha de creación + horas de la prioridad actual. Es una función pura, por eso se puede recalcular siempre. */
    private void recalcularFechaLimite() {
        // Protección contra null: en un objeto aún sin createdAt o sin prioridad no se puede calcular nada
        if (this.createdAt != null && this.priority != null) {
            // Instant.plus(Duration) suma horas a una fecha; Duration.ofHours convierte el número del enum en duración
            this.fechaLimite = this.createdAt.plus(Duration.ofHours(this.priority.getSlaHoras()));
        }
    }
}
