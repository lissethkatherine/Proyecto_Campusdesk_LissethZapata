package campusdesk.repository;

import campusdesk.entity.QuickResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejercicio 5: repositorio de plantillas de respuesta rápida, siempre filtrado por técnico propietario.
 */
@Repository
public interface QuickResponseRepository extends JpaRepository<QuickResponse, Long> {

    List<QuickResponse> findByTecnicoIdOrderByTituloAsc(Long tecnicoId);

    boolean existsByTecnicoIdAndTituloIgnoreCase(Long tecnicoId, String titulo);

    boolean existsByTecnicoIdAndTituloIgnoreCaseAndIdNot(Long tecnicoId, String titulo, Long id);
}
