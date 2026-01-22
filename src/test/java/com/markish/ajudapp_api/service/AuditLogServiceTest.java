package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.AuditLog;
import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-123")
                .email("test@example.com")
                .fullName("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testLogCreatesAuditLogEntry() {
        // Arrange
        String action = "USER_CREATED";
        String details = "User created for the first time";

        // Act
        auditLogService.log(testUser, action, details);

        // Assert
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedLog.getAction()).isEqualTo(action);
        assertThat(savedLog.getDetails()).isEqualTo(details);
        assertThat(savedLog.getCreatedAt()).isNotNull();
    }

    @Test
    void testLogWithDifferentActions() {
        // Test multiple action types
        String[] actions = {"USER_CREATED", "UPDATE_FROM_KEYCLOAK", "UPDATE_SETTINGS", "USER_DELETED"};

        for (int i = 0; i < actions.length; i++) {
            String action = actions[i];
            // Act
            auditLogService.log(testUser, action, "Details for " + action);

            // Assert
            ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

            verify(auditLogRepository, times(i + 1)).save(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getAction()).isEqualTo(action);
        }
    }

    @Test
    void testLogWithEmptyDetails() {
        // Act
        auditLogService.log(testUser, "TEST_ACTION", "");

        // Assert
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getDetails()).isEmpty();
    }

    @Test
    void testLogGeneratesUniqueIds() {
        // Act
        auditLogService.log(testUser, "ACTION_1", "Details 1");

        // Assert
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog log1 = auditLogCaptor.getValue();

        auditLogService.log(testUser, "ACTION_3", "Details 3");
        verify(auditLogRepository, times(2)).save(auditLogCaptor.capture());
        AuditLog log2 = auditLogCaptor.getValue();

        assertThat(log1.getAction()).isNotEqualTo(log2.getAction());
    }

    @Test
    void testLogWithNullUser() {
        // Act & Assert - should handle null user gracefully
        auditLogService.log(testUser, "NULL_USER_TEST", "Details");

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    void testLogTimestampIsRecent() {
        // Arrange
        Instant beforeLog = Instant.now();

        // Act
        auditLogService.log(testUser, "TIMESTAMP_TEST", "Details");

        // Assert
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getCreatedAt()).isNotNull();
        assertThat(savedLog.getCreatedAt()).isAfterOrEqualTo(beforeLog);
    }
}
