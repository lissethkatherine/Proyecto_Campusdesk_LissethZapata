package campusdesk.service.impl;

import campusdesk.dto.TicketExtendedResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.Tag;
import campusdesk.entity.Ticket;
import campusdesk.entity.User;
import campusdesk.exception.TicketNotFoundException;
import campusdesk.exception.UnauthorizedTicketActionException;
import campusdesk.mapper.TicketMapper;
import campusdesk.repository.TagRepository;
import campusdesk.repository.TicketRepository;
import campusdesk.repository.UserRepository;
import campusdesk.service.TicketTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ejercicio 1: lógica de negocio de etiquetas (find-or-create) y búsqueda por etiqueta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTagServiceImpl implements TicketTagService {

    private static final int MAX_ETIQUETAS_POR_PETICION = 20;

    private final TicketRepository ticketRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public TicketExtendedResponseDTO addTags(Long ticketId, List<String> nombres, String userEmail) {
        User user = findUserByEmail(userEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Incidencia no encontrada con ID: " + ticketId));

        // Solo ADMIN o el TECHNICIAN asignado pueden etiquetar (403)
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isAssignedTechnician = ticket.getTecnico() != null && ticket.getTecnico().getId().equals(user.getId());
        if (!isAdmin && !isAssignedTechnician) {
            throw new UnauthorizedTicketActionException(
                    "Solo un administrador o el técnico asignado puede etiquetar esta incidencia");
        }

        Set<String> nombresNormalizados = normalizarNombres(nombres);

        // Find-or-create: reutiliza la etiqueta si ya existe, o la crea automáticamente
        for (String nombre : nombresNormalizados) {
            Tag tag = tagRepository.findByNombre(nombre)
                    .orElseGet(() -> tagRepository.save(Tag.builder().nombre(nombre).build()));
            ticket.getTags().add(tag); // Set: una etiqueta repetida en el ticket no se duplica
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("Etiquetas {} asociadas al ticket #{} por [{}]", nombresNormalizados, ticketId, userEmail);
        return ticketMapper.toExtended(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketExtendedResponseDTO> findTicketsByTag(String nombre, String userEmail) {
        User user = findUserByEmail(userEmail);

        String nombreNormalizado = Tag.normalizar(nombre);
        if (nombreNormalizado.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la etiqueta es obligatorio");
        }

        // Visibilidad por rol: USER solo sus tickets, TECHNICIAN solo los asignados, ADMIN todos
        Long solicitanteFilter = null;
        Long tecnicoFilter = null;
        if (user.getRole() == Role.USER) {
            solicitanteFilter = user.getId();
        } else if (user.getRole() == Role.TECHNICIAN) {
            tecnicoFilter = user.getId();
        }

        return ticketRepository.findByTagNombre(nombreNormalizado, solicitanteFilter, tecnicoFilter)
                .stream()
                .map(ticketMapper::toExtended)
                .toList();
    }

    private Set<String> normalizarNombres(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos una etiqueta");
        }
        if (nombres.size() > MAX_ETIQUETAS_POR_PETICION) {
            throw new IllegalArgumentException("Solo se permiten hasta " + MAX_ETIQUETAS_POR_PETICION + " etiquetas por petición");
        }
        Set<String> resultado = new LinkedHashSet<>(); // elimina repetidas conservando el orden
        for (String raw : nombres) {
            String nombre = Tag.normalizar(raw);
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("Las etiquetas no pueden estar vacías");
            }
            if (nombre.length() > Tag.MAX_LENGTH) {
                throw new IllegalArgumentException("La etiqueta '" + nombre + "' excede los " + Tag.MAX_LENGTH + " caracteres");
            }
            resultado.add(nombre);
        }
        return resultado;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + email));
    }
}
