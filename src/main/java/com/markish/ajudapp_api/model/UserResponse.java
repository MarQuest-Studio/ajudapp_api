package com.markish.ajudapp_api.model;


import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String keycloakId,
        String email,
        String fullName,
        String profilePicture,
        Instant createdAt,
        Instant updatedAt,
        Instant privacyAcceptedAt,
        Instant deletedAt,
        boolean anonymized
) {}
