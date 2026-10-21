package campusdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Ejercicio 1: etiqueta transversal para clasificar incidencias (ej. URGENTE, VPN, SEGURIDAD).
 * El nombre es único y siempre se guarda normalizado en mayúsculas.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "tickets")
public class Tag {

    public static final int MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "El nombre de la etiqueta es obligatorio")
    @Size(max = MAX_LENGTH, message = "La etiqueta no puede exceder los 50 caracteres")
    @Column(name = "nombre", nullable = false, unique = true, length = MAX_LENGTH)
    @EqualsAndHashCode.Include
    private String nombre;

    // Lado inverso de la relación ManyToMany (el dueño es Ticket.tags)
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Ticket> tickets = new HashSet<>();

    /** Normaliza un nombre de etiqueta: sin espacios en los extremos y en MAYÚSCULAS. */
    public static String normalizar(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }
}
