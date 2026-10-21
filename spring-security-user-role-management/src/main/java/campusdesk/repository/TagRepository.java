package campusdesk.repository;

import campusdesk.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Ejercicio 1: repositorio de etiquetas (el nombre ya llega normalizado en mayúsculas).
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNombre(String nombre);
}
