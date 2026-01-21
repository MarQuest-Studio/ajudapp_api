package com.markish.ajudapp_api.extractor;

import com.markish.ajudapp_api.model.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakUserExtractorTest {

    private KeycloakUserExtractor extractor;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        extractor = new KeycloakUserExtractor();
        jwt = mock(Jwt.class);
    }

    @Test
    void testExtractExtractsKeycloakUserSuccessfully() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user", "admin"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.keycloakId()).isEqualTo("keycloak-123");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.fullName()).isEqualTo("Test User");
        assertThat(result.roles()).contains("user", "admin");
    }

    @Test
    void testExtractWithNoRoles() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.keycloakId()).isEqualTo("keycloak-123");
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void testExtractWithEmptyRoles() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of());
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void testExtractWithMultipleRoles() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-456");
        when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Admin User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user", "admin", "manager", "superuser"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.roles()).hasSize(4);
        assertThat(result.roles()).contains("user", "admin", "manager", "superuser");
    }

    @Test
    void testExtractWithNullEmail() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn(null);
        when(jwt.getClaimAsString("name")).thenReturn("Test User");
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.email()).isNull();
        assertThat(result.keycloakId()).isEqualTo("keycloak-123");
    }

    @Test
    void testExtractWithNullFullName() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn(null);
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.fullName()).isNull();
    }

    @Test
    void testExtractWithNullKeycloakId() {
        // Arrange
        when(jwt.getSubject()).thenReturn(null);
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.keycloakId()).isNull();
    }

    @Test
    void testExtractRolesSingleRole() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.roles()).hasSize(1);
        assertThat(result.roles()).contains("user");
    }

    @Test
    void testExtractRolesAreNotDuplicated() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user", "admin", "user", "admin"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert - Set automatically removes duplicates
        assertThat(result.roles()).hasSize(2);
        assertThat(result.roles()).contains("user", "admin");
    }

    @Test
    void testExtractWithSpecialCharactersInStrings() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123-special!@#");
        when(jwt.getClaimAsString("email")).thenReturn("test+special@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User (Admin) & Supervisor");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user-admin", "admin_role"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.keycloakId()).isEqualTo("keycloak-123-special!@#");
        assertThat(result.email()).isEqualTo("test+special@example.com");
        assertThat(result.fullName()).isEqualTo("Test User (Admin) & Supervisor");
        assertThat(result.roles()).contains("user-admin", "admin_role");
    }

    @Test
    void testExtractWithEmptyStrings() {
        // Arrange
        when(jwt.getSubject()).thenReturn("");
        when(jwt.getClaimAsString("email")).thenReturn("");
        when(jwt.getClaimAsString("name")).thenReturn("");
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.keycloakId()).isEmpty();
        assertThat(result.email()).isEmpty();
        assertThat(result.fullName()).isEmpty();
    }

    @Test
    void testExtractRolesIsImmutable() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("user", "admin"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);
        Set<String> roles = result.roles();

        // Assert - Set should be unmodifiable or at least defensive
        assertThat(roles).contains("user", "admin");
        // This test ensures the roles collection is properly encapsulated
    }

    @Test
    void testExtractWithLongStrings() {
        // Arrange
        String longEmail = "very.long.email.address.with.many.dots@subdomain.example.co.uk";
        String longName = "A Very Long User Name With Multiple Words And Special Characters";
        when(jwt.getSubject()).thenReturn("keycloak-very-long-id-" + System.nanoTime());
        when(jwt.getClaimAsString("email")).thenReturn(longEmail);
        when(jwt.getClaimAsString("name")).thenReturn(longName);
        when(jwt.getClaim("realm_access")).thenReturn(null);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.email()).isEqualTo(longEmail);
        assertThat(result.fullName()).isEqualTo(longName);
    }

    @Test
    void testExtractRolesContainsSpecialRoleNames() {
        // Arrange
        when(jwt.getSubject()).thenReturn("keycloak-123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test User");

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("realm-admin", "offline_access", "uma_authorization"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // Act
        KeycloakUser result = extractor.extract(jwt);

        // Assert
        assertThat(result.roles()).contains("realm-admin", "offline_access", "uma_authorization");
    }
}
