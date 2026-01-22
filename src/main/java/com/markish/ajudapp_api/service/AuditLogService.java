package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.AuditLog;
import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String details) {
        auditLogRepository.save(
                new AuditLog(
                        UUID.randomUUID(),
                        user.getId(),
                        action,
                        details,
                        Instant.now()
                )
        );
    }
}
