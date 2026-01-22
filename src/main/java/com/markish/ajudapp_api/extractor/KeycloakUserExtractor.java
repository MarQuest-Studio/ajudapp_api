package com.markish.ajudapp_api.extractor;

import com.markish.ajudapp_api.model.KeycloakUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakUserExtractor {

    public KeycloakUser extract(Jwt jwt) {
        return new KeycloakUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                extractRoles(jwt)
        );
    }

    private Set<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return Set.of();
        return new HashSet<>((List<String>) realmAccess.get("roles"));
    }
}

