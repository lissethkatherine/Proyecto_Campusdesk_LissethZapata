package campusdesk.service;

import campusdesk.dto.ChangePasswordDTO;
import campusdesk.dto.CreateTechnicianDTO;
import campusdesk.dto.UserRegistrationDTO;
import campusdesk.dto.UserResponseDTO;
import campusdesk.entity.Role;
import campusdesk.entity.User;
import campusdesk.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisSessionService redisSessionService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("registerUser debe encriptar la contraseña y asignar el rol USER por defecto")
    void testRegisterUserAssignsUserRoleAndEncodesPassword() {
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .name("Maria Fernandez")
                .email("maria@campusdesk.com")
                .password("RawPassword123")
                .build();

        when(userRepository.existsByEmail("maria@campusdesk.com")).thenReturn(false);
        when(passwordEncoder.encode("RawPassword123")).thenReturn("$2a$10$encodedHash");

        User savedUser = User.builder()
                .id(10L)
                .name("Maria Fernandez")
                .email("maria@campusdesk.com")
                .password("$2a$10$encodedHash")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = userService.registerUser(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Maria Fernandez");
        assertThat(response.getEmail()).isEqualTo("maria@campusdesk.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);

        verify(passwordEncoder, times(1)).encode("RawPassword123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser con email duplicado debe lanzar IllegalArgumentException")
    void testRegisterUserDuplicateEmailThrowsException() {
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .name("Maria Fernandez")
                .email("maria@campusdesk.com")
                .password("RawPassword123")
                .build();

        when(userRepository.existsByEmail("maria@campusdesk.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("findByEmail retorna el usuario cuando existe")
    void testFindByEmailReturnsUser() {
        User user = User.builder()
                .id(1L)
                .email("test@campusdesk.com")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("test@campusdesk.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("test@campusdesk.com");

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("createTechnician crea un usuario TECHNICIAN con la contraseña inicial común cifrada")
    void testCreateTechnicianUsesSharedDefaultPassword() {
        ReflectionTestUtils.setField(userService, "technicianDefaultPassword", "Tecnico2026*");
        CreateTechnicianDTO dto = CreateTechnicianDTO.builder().name("Laura Gomez").email("laura@campusdesk.com").build();

        when(userRepository.existsByEmail("laura@campusdesk.com")).thenReturn(false);
        when(passwordEncoder.encode("Tecnico2026*")).thenReturn("$2a$10$hashTecnico");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        UserResponseDTO response = userService.createTechnician(dto);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getRole()).isEqualTo(Role.TECHNICIAN);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashTecnico");
    }

    @Test
    @DisplayName("createTechnician rechaza correos ya registrados")
    void testCreateTechnicianDuplicateEmail() {
        ReflectionTestUtils.setField(userService, "technicianDefaultPassword", "Tecnico2026*");
        CreateTechnicianDTO dto = CreateTechnicianDTO.builder().name("Laura").email("laura@campusdesk.com").build();
        when(userRepository.existsByEmail("laura@campusdesk.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createTechnician(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createTechnician falla si la contraseña inicial no está configurada")
    void testCreateTechnicianWithoutConfiguredPassword() {
        ReflectionTestUtils.setField(userService, "technicianDefaultPassword", "");
        CreateTechnicianDTO dto = CreateTechnicianDTO.builder().name("Laura").email("laura@campusdesk.com").build();
        when(userRepository.existsByEmail("laura@campusdesk.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.createTechnician(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TECHNICIAN_DEFAULT_PASSWORD");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword guarda la nueva contraseña cifrada y revoca las sesiones")
    void testChangePasswordSuccess() {
        User tech = User.builder().id(2L).email("tech@campusdesk.com").password("oldHash").role(Role.TECHNICIAN).build();
        ChangePasswordDTO dto = ChangePasswordDTO.builder().currentPassword("Tecnico2026*").newPassword("NuevaClave123").build();

        when(userRepository.findByEmail("tech@campusdesk.com")).thenReturn(Optional.of(tech));
        when(passwordEncoder.matches("Tecnico2026*", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("NuevaClave123", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("newHash");

        userService.changePassword("tech@campusdesk.com", dto);

        assertThat(tech.getPassword()).isEqualTo("newHash");
        verify(userRepository).save(tech);
        verify(redisSessionService).invalidateAllSessionsByUser("tech@campusdesk.com");
    }

    @Test
    @DisplayName("changePassword rechaza una contraseña actual incorrecta")
    void testChangePasswordWrongCurrent() {
        User tech = User.builder().id(2L).email("tech@campusdesk.com").password("oldHash").role(Role.TECHNICIAN).build();
        ChangePasswordDTO dto = ChangePasswordDTO.builder().currentPassword("Incorrecta1").newPassword("NuevaClave123").build();

        when(userRepository.findByEmail("tech@campusdesk.com")).thenReturn(Optional.of(tech));
        when(passwordEncoder.matches("Incorrecta1", "oldHash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("tech@campusdesk.com", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actual es incorrecta");
        verify(userRepository, never()).save(any(User.class));
        verify(redisSessionService, never()).invalidateAllSessionsByUser(any());
    }

    @Test
    @DisplayName("changePassword rechaza una nueva contraseña igual a la actual")
    void testChangePasswordSameAsCurrent() {
        User tech = User.builder().id(2L).email("tech@campusdesk.com").password("oldHash").role(Role.TECHNICIAN).build();
        ChangePasswordDTO dto = ChangePasswordDTO.builder().currentPassword("Tecnico2026*").newPassword("Tecnico2026*").build();

        when(userRepository.findByEmail("tech@campusdesk.com")).thenReturn(Optional.of(tech));
        when(passwordEncoder.matches("Tecnico2026*", "oldHash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword("tech@campusdesk.com", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente");
        verify(userRepository, never()).save(any(User.class));
    }
}
