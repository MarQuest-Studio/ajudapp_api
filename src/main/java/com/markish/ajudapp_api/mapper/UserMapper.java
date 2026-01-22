package com.markish.ajudapp_api.mapper;

import com.markish.ajudapp_api.entity.User;
import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ObjectStorageService objectStorageService;

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return new UserResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getEmail(),
                user.getFullName(),
                user.getProfilePicture() != null ? objectStorageService.getProfileUrl(user) : null,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getPrivacyAcceptedAt(),
                user.getDeletedAt(),
                user.isAnonymized()
        );
    }

    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) return List.of();
        return users.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();
    }
}
