package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.extractor.KeycloakUserExtractor;
import com.markish.ajudapp_api.mapper.UserMapper;
import com.markish.ajudapp_api.model.KeycloakUser;
import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.model.UserSettingsUpdateRequest;
import com.markish.ajudapp_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserExtractor extractor;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Jwt jwt;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private KeycloakUser testKeycloakUser;
    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);

        testKeycloakUser = new KeycloakUser(
                "keycloak-123",
                "test@example.com",
                "Test User",
                Set.of("user", "admin")
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-123")
                .email("test@example.com")
                .fullName("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUserResponse = new UserResponse(
                testUser.getId(),
                testUser.getKeycloakId(),
                testUser.getEmail(),
                testUser.getFullName(),
                null,
                testUser.getCreatedAt(),
                testUser.getUpdatedAt(),
                null,
                null,
                false
        );
    }

    @Test
    void testFindOrCreateCurrentUserWhenUserExists() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findOrCreateCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getKeycloakId()).isEqualTo("keycloak-123");
        verify(userRepository).findByKeycloakId("keycloak-123");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testFindOrCreateCurrentUserWhenUserDoesNotExist() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.findOrCreateCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo("keycloak-123");
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(any(User.class), eq("USER_CREATED"), anyString());
    }

    @Test
    void testGetOrCreateCurrentUserReturnsResponse() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse result = userService.getOrCreateCurrentUser();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testUser.getId());
        assertThat(result.email()).isEqualTo(testUser.getEmail());
        verify(userMapper).toResponse(testUser);
    }

    @Test
    void testUpdateSettingsSuccessfully() {
        // Arrange
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest(true);
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse result = userService.updateSettings(request);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(any(User.class), eq("UPDATE_SETTINGS"), anyString());
    }

    @Test
    void testAnonymizeCurrentUserSuccessfully() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        testUser.setProfilePicture("https://example.com/profile.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.anonymizeCurrentUser();

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User anonymizedUser = userCaptor.getValue();
        assertThat(anonymizedUser.isAnonymized()).isTrue();
        assertThat(anonymizedUser.getDeletedAt()).isNotNull();
        assertThat(anonymizedUser.getEmail()).startsWith("deleted-");
        assertThat(anonymizedUser.getFullName()).isEmpty();
        assertThat(anonymizedUser.getProfilePicture()).isNull();

        verify(objectStorageService).delete(anyString());
        verify(auditLogService).log(any(User.class), eq("USER_DELETED"), anyString());
    }

    @Test
    void testUploadProfilePictureSuccessfully() throws Exception {
        // Arrange
        byte[] fileContent = "fake image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", fileContent);

        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        when(objectStorageService.upload(anyString(), eq(file))).thenReturn("https://example.com/profile.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse result = userService.uploadProfilePicture(file);

        // Assert
        assertThat(result).isNotNull();
        verify(objectStorageService).upload(anyString(), eq(file));
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(any(User.class), eq("UPLOADED_PFP"), anyString());
    }

    @Test
    void testUploadProfilePictureWhenUserNotFound() {
        // Arrange
        byte[] fileContent = "fake image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", fileContent);

        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.uploadProfilePicture(file))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void testUpdateIfNeededWhenEmailChanges() {
        // Arrange
        KeycloakUser updatedKeycloakUser = new KeycloakUser(
                "keycloak-123",
                "newemail@example.com",
                "Test User",
                Set.of("user")
        );
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(extractor.extract(any())).thenReturn(testKeycloakUser);

        // Act
        User result = userService.findOrCreateCurrentUser();

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void testUpdateIfNeededWhenFullNameChanges() {
        // Arrange
        testUser.setFullName("Old Name");
        KeycloakUser updatedKeycloakUser = new KeycloakUser(
                "keycloak-123",
                "test@example.com",
                "New Name",
                Set.of("user")
        );

        when(extractor.extract(jwt)).thenReturn(updatedKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.findOrCreateCurrentUser();

        // Assert
        verify(auditLogService).log(any(User.class), eq("UPDATE_FROM_KEYCLOAK"), anyString());
    }

    @Test
    void testUpdateIfNeededWhenNothingChanges() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findOrCreateCurrentUser();

        // Assert
        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), anyString(), anyString());
    }

    @Test
    void testCreateUserSetsTimestamps() {
        // Arrange
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Instant beforeCreation = Instant.now();

        // Act
        User result = userService.findOrCreateCurrentUser();

        Instant afterCreation = Instant.now();

        // Assert
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getCreatedAt()).isAfter(beforeCreation.minusSeconds(1));
        assertThat(result.getCreatedAt()).isBefore(afterCreation.plusSeconds(1));
    }

    @Test
    void testAnonymizeCurrentUserWithoutProfilePicture() {
        // Arrange
        testUser.setProfilePicture(null);
        when(extractor.extract(jwt)).thenReturn(testKeycloakUser);
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.anonymizeCurrentUser();

        // Assert
        verify(objectStorageService, never()).delete(anyString());
        verify(auditLogService).log(any(User.class), eq("USER_DELETED"), anyString());
    }
}
