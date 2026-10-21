package campusdesk.repository;

import campusdesk.entity.TicketRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Ejercicio 7: acceso a datos de las calificaciones, con agregaciones AVG y COUNT.
 */
@Repository
public interface TicketRatingRepository extends JpaRepository<TicketRating, Long> {

    // Consulta DERIVADA: Spring Data lee el nombre del método y genera el SQL "SELECT COUNT(*) > 0 ... WHERE ticket_id = ?".
    // Sirve para impedir que un ticket se califique dos veces.
    boolean existsByTicketId(Long ticketId);

    // AVG calcula el promedio EN LA BASE DE DATOS (más eficiente que traer todas las filas y promediar en Java).
    // "r.ticket.tecnico.id" navega: calificación -> ticket -> técnico asignado -> su id.
    // Devuelve Double (objeto, no double) porque AVG retorna NULL cuando el técnico aún no tiene calificaciones.
    @Query("SELECT AVG(r.puntaje) FROM TicketRating r WHERE r.ticket.tecnico.id = :tecnicoId")
    Double promedioPorTecnico(@Param("tecnicoId") Long tecnicoId);

    // COUNT siempre devuelve un número (0 si no hay filas), por eso aquí sí basta el tipo primitivo long
    @Query("SELECT COUNT(r) FROM TicketRating r WHERE r.ticket.tecnico.id = :tecnicoId")
    long contarPorTecnico(@Param("tecnicoId") Long tecnicoId);
}
