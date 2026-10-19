package campusdesk.controller;

import campusdesk.dto.ChangePasswordDTO;
import campusdesk.dto.CreateTechnicianDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.User;
import campusdesk.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consulta de usuarios y técnicos del sistema (RF-01, RF-04).
 */
@RestController
@RequestMapping({"/api/users", "/users"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Consultas de usuarios y personal técnico")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar todos los usuarios registrados (F2)", description = "Exclusivo para ADMIN. Retorna lista de usuarios sin contraseñas ni hashes")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Listar técnicos disponibles (E4)", description = "Permite exclusivamente a los administradores obtener la lista de técnicos para asignación de incidencias")
    @GetMapping("/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getTechnicians() {
        return ResponseEntity.ok(userService.getTechnicians());
    }

    @Operation(summary = "Crear un técnico (solo ADMIN)",
            description = "Crea una cuenta con rol TECHNICIAN y la contraseña inicial común configurada en TECHNICIAN_DEFAULT_PASSWORD. El técnico puede cambiarla luego con PATCH /api/users/me/password")
    @PostMapping("/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createTechnician(@Valid @RequestBody CreateTechnicianDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createTechnician(dto));
    }

    @Operation(summary = "Cambiar la contraseña propia",
            description = "Cualquier usuario autenticado cambia su contraseña indicando la actual. Al terminar se cierran todas sus sesiones")
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changeMyPassword(@Valid @RequestBody ChangePasswordDTO dto, Principal principal) {
        userService.changePassword(principal.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente. Inicia sesión nuevamente."));
    }

    @Operation(summary = "Obtener perfil del usuario autenticado")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyProfile(Principal principal) {
        User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + principal.getName()));

        return ResponseEntity.ok(UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build());
    }
}
