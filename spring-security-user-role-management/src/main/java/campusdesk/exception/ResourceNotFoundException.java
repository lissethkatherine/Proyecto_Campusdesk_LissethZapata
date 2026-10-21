package campusdesk.exception;

/**
 * Recurso auxiliar no encontrado, por ejemplo una plantilla (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
