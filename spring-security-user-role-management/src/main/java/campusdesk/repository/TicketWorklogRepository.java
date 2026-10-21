package campusdesk.repository;

import campusdesk.entity.TicketWorklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejercicio 4: repositorio de la bitácora de tiempo técnico, con agregación SUM.
 */
@Repository
public interface TicketWorklogRepository extends JpaRepository<TicketWorklog, Long> {

    // SUM devuelve null cuando el ticket no tiene registros: el servicio lo convierte en 0
    @Query("SELECT SUM(w.minutosDedicados) FROM TicketWorklog w WHERE w.ticket.id = :ticketId")
    Long sumMinutosByTicketId(@Param("ticketId") Long ticketId);

    @Query("SELECT w FROM TicketWorklog w " +
           "JOIN FETCH w.tecnico " +
           "WHERE w.ticket.id = :ticketId " +
           "ORDER BY w.fechaRegistro ASC")
    List<TicketWorklog> findByTicketIdWithTecnico(@Param("ticketId") Long ticketId);
}
