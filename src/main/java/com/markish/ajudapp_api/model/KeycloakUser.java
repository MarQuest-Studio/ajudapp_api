package com.markish.ajudapp_api.model;

import java.util.Set;

public record KeycloakUser(
        String keycloakId,
        String email,
        String fullName,
        Set<String> roles
) {}
