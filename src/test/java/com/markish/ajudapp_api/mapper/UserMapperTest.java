package com.markish.ajudapp_api.mapper;

import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.service.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private UserMapper userMapper;

    private User testUser;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        testUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-123")
                .email("test@example.com")
                .fullName("Test User")
                .profilePicture("https://example.com/profile.jpg")
                .createdAt(now)
                .updatedAt(now)
                .privacyAcceptedAt(now)
                .deletedAt(null)
                .anonymized(false)
                .build();
    }

    @Test
    void testToResponseMapsUserSuccessfully() {
        // Arrange
        when(objectStorageService.getProfileUrl(testUser)).thenReturn("https://presigned-url.com/profile.jpg");

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testUser.getId());
        assertThat(response.keycloakId()).isEqualTo(testUser.getKeycloakId());
        assertThat(response.email()).isEqualTo(testUser.getEmail());
        assertThat(response.fullName()).isEqualTo(testUser.getFullName());
        assertThat(response.profilePicture()).isEqualTo("https://presigned-url.com/profile.jpg");
        assertThat(response.createdAt()).isEqualTo(testUser.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(testUser.getUpdatedAt());
        assertThat(response.privacyAcceptedAt()).isEqualTo(testUser.getPrivacyAcceptedAt());
        assertThat(response.deletedAt()).isNull();
        assertThat(response.anonymized()).isFalse();

        verify(objectStorageService).getProfileUrl(testUser);
    }

    @Test
    void testToResponseWithNullProfilePicture() {
        // Arrange
        testUser.setProfilePicture(null);

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.profilePicture()).isNull();
        // getProfileUrl should not be called
    }

    @Test
    void testToResponseWithAnonymizedUser() {
        // Arrange
        testUser.setAnonymized(true);
        testUser.setDeletedAt(now);
        when(objectStorageService.getProfileUrl(testUser)).thenReturn(null);

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.anonymized()).isTrue();
        assertThat(response.deletedAt()).isEqualTo(now);
    }

    @Test
    void testToResponseWithNullUser() {
        // Act
        UserResponse response = userMapper.toResponse(null);

        // Assert
        assertThat(response).isNull();
    }

    @Test
    void testToResponseListMapsMultipleUsers() {
        // Arrange
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-1")
                .email("user1@example.com")
                .fullName("User One")
                .profilePicture("https://example.com/1.jpg")
                .createdAt(now)
                .updatedAt(now)
                .anonymized(false)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-2")
                .email("user2@example.com")
                .fullName("User Two")
                .profilePicture(null)
                .createdAt(now)
                .updatedAt(now)
                .anonymized(false)
                .build();

        List<User> users = Arrays.asList(user1, user2);
        when(objectStorageService.getProfileUrl(user1)).thenReturn("https://presigned-1.com/profile.jpg");

        // Act
        List<UserResponse> responses = userMapper.toResponseList(users);

        // Assert
        assertThat(responses)
                .isNotNull()
                .hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(user1.getId());
        assertThat(responses.get(0).email()).isEqualTo("user1@example.com");
        assertThat(responses.get(1).id()).isEqualTo(user2.getId());
        assertThat(responses.get(1).email()).isEqualTo("user2@example.com");
        assertThat(responses.get(1).profilePicture()).isNull();

        verify(objectStorageService).getProfileUrl(user1);
    }

    @Test
    void testToResponseListWithNullList() {
        // Act
        List<UserResponse> responses = userMapper.toResponseList(null);

        // Assert
        assertThat(responses)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testToResponseListWithEmptyList() {
        // Act
        List<UserResponse> responses = userMapper.toResponseList(List.of());

        // Assert
        assertThat(responses)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testToResponseListFiltersNullElements() {
        // Arrange
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-1")
                .email("user1@example.com")
                .fullName("User One")
                .createdAt(now)
                .updatedAt(now)
                .anonymized(false)
                .build();

        List<User> users = Arrays.asList(user1, null, user1);

        // Act
        List<UserResponse> responses = userMapper.toResponseList(users);

        // Assert
        assertThat(responses)
                .isNotNull()
                .hasSize(2); // null element filtered out
    }

    @Test
    void testToResponsePreservesAllTimestamps() {
        // Arrange
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");
        Instant privacyAccepted = Instant.parse("2024-01-03T10:00:00Z");
        Instant deleted = Instant.parse("2024-01-04T10:00:00Z");

        testUser.setCreatedAt(created);
        testUser.setUpdatedAt(updated);
        testUser.setPrivacyAcceptedAt(privacyAccepted);
        testUser.setDeletedAt(deleted);

        when(objectStorageService.getProfileUrl(testUser)).thenReturn("https://url.com/profile.jpg");

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
        assertThat(response.privacyAcceptedAt()).isEqualTo(privacyAccepted);
        assertThat(response.deletedAt()).isEqualTo(deleted);
    }

    @Test
    void testToResponseCallsObjectStorageServiceForProfileUrl() {
        // Arrange
        String expectedUrl = "https://s3-presigned.com/file.jpg?signature=abc";
        when(objectStorageService.getProfileUrl(testUser)).thenReturn(expectedUrl);

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response.profilePicture()).isEqualTo(expectedUrl);
        verify(objectStorageService).getProfileUrl(testUser);
    }

    @Test
    void testToResponseListWithMixedUsers() {
        // Arrange
        User userWithProfile = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-1")
                .email("user1@example.com")
                .fullName("User One")
                .profilePicture("https://example.com/1.jpg")
                .createdAt(now)
                .updatedAt(now)
                .anonymized(false)
                .build();

        User userWithoutProfile = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-2")
                .email("user2@example.com")
                .fullName("User Two")
                .profilePicture(null)
                .createdAt(now)
                .updatedAt(now)
                .anonymized(false)
                .build();

        User anonymizedUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-3")
                .email("deleted-user@example.com")
                .fullName("")
                .profilePicture(null)
                .createdAt(now)
                .updatedAt(now)
                .anonymized(true)
                .build();

        List<User> users = Arrays.asList(userWithProfile, userWithoutProfile, anonymizedUser);
        when(objectStorageService.getProfileUrl(userWithProfile)).thenReturn("https://presigned.com/profile.jpg");

        // Act
        List<UserResponse> responses = userMapper.toResponseList(users);

        // Assert
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).profilePicture()).isNotNull();
        assertThat(responses.get(1).profilePicture()).isNull();
        assertThat(responses.get(2).anonymized()).isTrue();
    }

    @Test
    void testToResponseWithEmptyStrings() {
        // Arrange
        testUser.setFullName("");
        testUser.setEmail("");
        when(objectStorageService.getProfileUrl(testUser)).thenReturn("https://url.com/profile.jpg");

        // Act
        UserResponse response = userMapper.toResponse(testUser);

        // Assert
        assertThat(response.fullName()).isEmpty();
        assertThat(response.email()).isEmpty();
    }
}
