package campusdesk.repository;

import campusdesk.entity.Ticket;
import campusdesk.entity.TicketCategory;
import campusdesk.entity.TicketPriority;
import campusdesk.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio JPA para operaciones de consulta y persistencia sobre la entidad Ticket (RF-03, RF-07, RF-08).
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findBySolicitanteIdOrderByCreatedAtDesc(Long solicitanteId);

    List<Ticket> findByTecnicoIdOrderByCreatedAtDesc(Long tecnicoId);

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    // Métricas del Dashboard globales (ADMIN)
    long countByStatus(TicketStatus status);

    // Métricas del Dashboard por Solicitante (USER)
    long countBySolicitanteId(Long solicitanteId);
    long countBySolicitanteIdAndStatus(Long solicitanteId, TicketStatus status);

    // Métricas del Dashboard por Técnico (TECHNICIAN)
    long countByTecnicoId(Long tecnicoId);
    long countByTecnicoIdAndStatus(Long tecnicoId, TicketStatus status);

    // Búsqueda y filtrado multicriterio dinámico con soporte de roles
    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN FETCH t.solicitante s " +
           "LEFT JOIN FETCH t.tecnico te " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:solicitanteId IS NULL OR t.solicitante.id = :solicitanteId) " +
           "AND (:tecnicoId IS NULL OR t.tecnico.id = :tecnicoId) " +
           "ORDER BY t.createdAt DESC")
    List<Ticket> findWithFilters(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            @Param("solicitanteId") Long solicitanteId,
            @Param("tecnicoId") Long tecnicoId
    );

    // Ejercicio 1: tickets asociados a una etiqueta, con filtros de visibilidad por rol (null = sin filtro)
    @Query("SELECT t FROM Ticket t " +
           "JOIN t.tags g " +
           "LEFT JOIN FETCH t.solicitante " +
           "LEFT JOIN FETCH t.tecnico " +
           "WHERE g.nombre = :nombre " +
           "AND (:solicitanteId IS NULL OR t.solicitante.id = :solicitanteId) " +
           "AND (:tecnicoId IS NULL OR t.tecnico.id = :tecnicoId) " +
           "ORDER BY t.createdAt DESC")
    List<Ticket> findByTagNombre(
            @Param("nombre") String nombre,
            @Param("solicitanteId") Long solicitanteId,
            @Param("tecnicoId") Long tecnicoId
    );

    // Ejercicio 2: tickets con al menos una reapertura
    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN FETCH t.solicitante " +
           "LEFT JOIN FETCH t.tecnico " +
           "WHERE t.reaperturasCount > 0 " +
           "ORDER BY t.updatedAt DESC")
    List<Ticket> findReopenedTickets();

    // Ejercicio 8: tickets VENCIDOS = fecha límite ya pasada y ticket todavía no finalizado.
    // Cómo se lee la consulta JPQL (se escribe con nombres de CLASES y CAMPOS Java, no de tablas SQL):
    //   - LEFT JOIN FETCH: trae solicitante y técnico en la MISMA consulta (evita una consulta extra por ticket).
    //   - t.fechaLimite < :ahora : compara la fecha límite con el instante actual que pasamos como parámetro.
    //   - t.status NOT IN :estadosFinales : excluye CERRADA/CANCELADA (recibe una colección de estados).
    //   - (:solicitanteId IS NULL OR ...) : filtro opcional; si el parámetro es null se ignora esa condición.
    //   - ORDER BY t.fechaLimite ASC : primero los que vencieron hace más tiempo (los más urgentes).
    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN FETCH t.solicitante " +
           "LEFT JOIN FETCH t.tecnico " +
           "WHERE t.fechaLimite < :ahora " +
           "AND t.status NOT IN :estadosFinales " +
           "AND (:solicitanteId IS NULL OR t.solicitante.id = :solicitanteId) " +
           "AND (:tecnicoId IS NULL OR t.tecnico.id = :tecnicoId) " +
           "ORDER BY t.fechaLimite ASC")
    List<Ticket> findOverdue(
            @Param("ahora") Instant ahora,
            @Param("estadosFinales") Collection<TicketStatus> estadosFinales,
            @Param("solicitanteId") Long solicitanteId,
            @Param("tecnicoId") Long tecnicoId
    );
}
