package campusdesk.service;

import campusdesk.dto.ChangePasswordDTO;
import campusdesk.dto.CreateTechnicianDTO;
import campusdesk.dto.UserRegistrationDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.User;
import campusdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de usuarios del sistema CAMPUSDESK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /** Mismo criterio que el registro: 8 a 100 caracteres con mayúscula, minúscula y número. */
    private static final String PASSWORD_POLICY = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisSessionService redisSessionService;

    /** Contraseña inicial común para los técnicos nuevos (variable TECHNICIAN_DEFAULT_PASSWORD). */
    @Value("${app.technician.default-password:}")
    private String technicianDefaultPassword;

    /**
     * Registra un nuevo usuario en el sistema.
     * Encripta la contraseña usando BCrypt y asigna estrictamente el rol USER por defecto.
     *
     * @param registrationDTO Datos de registro del usuario.
     * @return DTO con la información pública del usuario creado.
     */
    @Transactional
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        // 1. Validar que el correo no esté ya registrado
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("The email is already registered");
        }

        // 2. Encriptar contraseña con algoritmo BCrypt
        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        // 3. Crear entidad asignando estrictamente el rol USER por defecto
        User newUser = User.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        // 4. Guardar en base de datos
        User savedUser = userRepository.save(newUser);

        // 5. Retornar DTO de respuesta sin exponer la contraseña
        return UserResponseDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email Correo electrónico a consultar.
     * @return Optional con la entidad User si existe.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Obtiene la lista de usuarios con rol TECHNICIAN.
     *
     * @return Lista de técnicos disponibles.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getTechnicians() {
        return userRepository.findByRole(Role.TECHNICIAN).stream()
                .map(u -> UserResponseDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();
    }

    /**
     * Obtiene la lista completa de todos los usuarios registrados (F2).
     * Exclusivo para administradores. Retorna DTO seguro sin contraseñas ni hashes.
     *
     * @return Lista de todos los usuarios registrados.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserResponseDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();
    }

    /**
     * Crea una cuenta de técnico (solo el administrador puede invocarlo desde el controlador).
     * El rol es siempre TECHNICIAN y la contraseña inicial es la configurada para todos los técnicos;
     * cada técnico puede cambiarla después con {@link #changePassword}.
     *
     * @param dto Nombre y correo del nuevo técnico.
     * @return DTO público del técnico creado (sin contraseña).
     */
    @Transactional
    public UserResponseDTO createTechnician(CreateTechnicianDTO dto) {
        String email = dto.getEmail().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("The email is already registered");
        }
        if (technicianDefaultPassword == null || !technicianDefaultPassword.matches(PASSWORD_POLICY)) {
            throw new IllegalArgumentException(
                    "La contraseña inicial de técnicos no está configurada correctamente (TECHNICIAN_DEFAULT_PASSWORD)");
        }

        User technician = User.builder()
                .name(dto.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(technicianDefaultPassword))
                .role(Role.TECHNICIAN)
                .build();

        User saved = userRepository.save(technician);
        log.info("Técnico creado: [{}] (id {})", saved.getEmail(), saved.getId());

        return UserResponseDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .build();
    }

    /**
     * Cambia la contraseña del usuario autenticado. Exige la contraseña actual,
     * guarda la nueva con BCrypt y revoca todas sus sesiones activas (debe iniciar sesión de nuevo).
     *
     * @param email Correo del usuario autenticado (proviene del token, no del cliente).
     * @param dto   Contraseña actual y nueva contraseña.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        redisSessionService.invalidateAllSessionsByUser(email);
        log.info("Contraseña actualizada para [{}]; sesiones revocadas", email);
    }
}
