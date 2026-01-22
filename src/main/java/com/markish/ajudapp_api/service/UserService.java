package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.extractor.KeycloakUserExtractor;
import com.markish.ajudapp_api.mapper.UserMapper;
import com.markish.ajudapp_api.model.KeycloakUser;
import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.model.UserSettingsUpdateRequest;
import com.markish.ajudapp_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.markish.ajudapp_api.common.ApiConstants.ObjectStorage.KEY_FORMAT;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakUserExtractor extractor;
    private final AuditLogService auditLogService;
    private final ObjectStorageService objectStorageService;
    private final UserMapper userMapper;

    public User findOrCreateCurrentUser() {
        Jwt jwt = getJwt();
        KeycloakUser keycloakUser = extractor.extract(jwt);

        return userRepository.findByKeycloakId(keycloakUser.keycloakId())
                .map(existing -> updateIfNeeded(existing, keycloakUser))
                .orElseGet(() -> createUser(keycloakUser));
    }

    public UserResponse getOrCreateCurrentUser() {
        Jwt jwt = getJwt();
        KeycloakUser keycloakUser = extractor.extract(jwt);

        return userMapper.toResponse(userRepository.findByKeycloakId(keycloakUser.keycloakId())
                .map(existing -> updateIfNeeded(existing, keycloakUser))
                .orElseGet(() -> createUser(keycloakUser)));
    }


    private User createUser(KeycloakUser keycloakUser) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .keycloakId(keycloakUser.keycloakId())
                .email(keycloakUser.email())
                .fullName(keycloakUser.fullName())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user = userRepository.save(user);

        auditLogService.log(user, "USER_CREATED", "User created for the first time");
        return user;
    }

    private User updateIfNeeded(User user, KeycloakUser keycloakUser) {
        boolean dirty = false;

        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(user.getEmail(), keycloakUser.email())) {
            user.setEmail(keycloakUser.email());
            changedFields.add("email");
            dirty = true;
        }

        if (!Objects.equals(user.getFullName(), keycloakUser.fullName())) {
            user.setFullName(keycloakUser.fullName());
            changedFields.add("fullName");
            dirty = true;
        }

        if (dirty) {
            user = userRepository.save(user);
            auditLogService.log(user, "UPDATE_FROM_KEYCLOAK", "User updated the fields " + changedFields.stream().reduce((prev, field) -> prev + ", " + field).orElse("  NONE").substring(2));
        }

        return user;
    }

    private Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Jwt) auth.getPrincipal();
    }

    @Transactional
    public UserResponse updateSettings(UserSettingsUpdateRequest request) {
        User user = findOrCreateCurrentUser();

        //user.set(request.notificationsEnabled());

        auditLogService.log(user, "UPDATE_SETTINGS", "User updated the fields " + " MANUALLY"); //TODO: Add after fields
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void anonymizeCurrentUser() {
        User user = findOrCreateCurrentUser();

        if (user.getProfilePicture() != null) {
            objectStorageService.delete(getProfileKey(user));
        }
        user.setEmail("deleted-" + user.getId() + "@example.com");
        user.setFullName("");
        user.setProfilePicture(null);
        //user.setNotificationsEnabled(false);
        user.setAnonymized(true);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);
        auditLogService.log(user, "USER_DELETED", "User was anonymized"); //TODO: Add after fields
    }


    @Transactional
    public UserResponse uploadProfilePicture(MultipartFile file) throws IOException {

        Jwt jwt = getJwt();
        KeycloakUser keycloakUser = extractor.extract(jwt);

        User user = userRepository.findByKeycloakId(keycloakUser.keycloakId())
                .orElseThrow(NoSuchElementException::new);

        String key = getProfileKey(user);

        String url = objectStorageService.upload(key, file);

        user.setProfilePicture(url);
        user.setUpdatedAt(Instant.now());

        user = userRepository.save(user);
        auditLogService.log(user, "UPLOADED_PFP", "Profile pic was created for user with id " + user.getId()); //TODO: Add after fields

        return userMapper.toResponse(user);
    }

    private static String getProfileKey(User user) {
        return String.format(KEY_FORMAT, user.getId(), System.currentTimeMillis());
    }
}
