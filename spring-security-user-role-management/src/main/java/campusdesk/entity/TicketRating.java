package campusdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Ejercicio 7: calificación (1 a 5) que el solicitante deja sobre la atención recibida en un ticket CERRADA.
 * Un ticket solo puede tener UNA calificación: por eso la relación con Ticket es @OneToOne y la columna es única.
 */
@Entity // Esta clase es una tabla de la base de datos
@Table(name = "ticket_ratings") // Nombre de la tabla
@Getter // Lombok: getters de todos los campos
@Setter // Lombok: setters de todos los campos
@NoArgsConstructor // Constructor vacío (JPA lo necesita para crear objetos al leer de la BD)
@AllArgsConstructor // Constructor con todos los campos (lo usa @Builder)
@Builder // Permite TicketRating.builder().campo(...).build()
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode solo con el campo marcado (el id)
@ToString(exclude = {"ticket", "solicitante"}) // evita cargar relaciones LAZY al imprimir el objeto
public class TicketRating {

    public static final int MIN_PUNTAJE = 1; // menor calificación permitida
    public static final int MAX_PUNTAJE = 5; // mayor calificación permitida

    @Id // clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // la BD genera el id autoincremental
    @EqualsAndHashCode.Include // el id es lo que identifica al objeto
    @Column(name = "id", nullable = false, updatable = false) // nunca nulo y nunca se modifica
    private Long id;

    // OneToOne = un ticket <-> una calificación. LAZY = no se carga el ticket hasta que se use (más eficiente)
    @OneToOne(fetch = FetchType.LAZY)
    // unique = true: la BASE DE DATOS impide dos calificaciones del mismo ticket aunque falle la validación del servicio
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    // Quién calificó (siempre el solicitante del ticket). ManyToOne: un usuario puede dejar muchas calificaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private User solicitante;

    @NotNull(message = "El puntaje es obligatorio") // validación Bean Validation al persistir
    @Min(value = MIN_PUNTAJE, message = "El puntaje mínimo es 1") // rango inferior
    @Max(value = MAX_PUNTAJE, message = "El puntaje máximo es 5") // rango superior
    @Column(name = "puntaje", nullable = false)
    private Integer puntaje;

    @Size(max = 250, message = "El comentario no puede exceder los 250 caracteres")
    @Column(name = "comentario", length = 250) // opcional: puede ser null
    private String comentario;

    @Column(name = "fecha", nullable = false, updatable = false) // se fija una vez y no cambia
    private Instant fecha;

    // Callback de JPA: se ejecuta justo ANTES del primer INSERT; así la fecha siempre queda registrada
    @PrePersist
    protected void onCreate() {
        this.fecha = Instant.now();
    }
}
