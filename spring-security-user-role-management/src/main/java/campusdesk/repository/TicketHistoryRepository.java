package campusdesk.repository;

import campusdesk.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para el historial y trazabilidad inmutable de estados de cada incidencia (RF-06).
 */
@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    @Query("SELECT h FROM TicketHistory h " +
           "JOIN FETCH h.autor a " +
           "WHERE h.ticket.id = :ticketId " +
           "ORDER BY h.fechaCambio ASC")
    List<TicketHistory> findByTicketIdOrderByFechaCambioAsc(@Param("ticketId") Long ticketId);
}
