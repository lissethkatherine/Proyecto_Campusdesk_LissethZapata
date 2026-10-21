package campusdesk.exception;

/**
 * Conflicto de unicidad en un recurso auxiliar, por ejemplo un título de plantilla repetido (HTTP 409).
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
