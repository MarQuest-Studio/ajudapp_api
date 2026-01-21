package com.markish.ajudapp_api.repository;

import com.markish.ajudapp_api.entity.AuditLog;
import com.markish.ajudapp_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
