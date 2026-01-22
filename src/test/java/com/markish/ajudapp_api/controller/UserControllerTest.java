package com.markish.ajudapp_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.markish.ajudapp_api.model.UserResponse;
import com.markish.ajudapp_api.model.UserSettingsUpdateRequest;
import com.markish.ajudapp_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse testUserResponse;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        Instant now = Instant.now();
        testUserResponse = new UserResponse(
                testUserId,
                "keycloak-123",
                "test@example.com",
                "Test User",
                "https://example.com/profile.jpg",
                now,
                now,
                now,
                null,
                false
        );
    }

    @Test
    void testMeEndpointReturnsCurrentUser() throws Exception {
        // Arrange
        when(userService.getOrCreateCurrentUser()).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(get("/api/user/me").with(jwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.anonymized").value(false));

        verify(userService).getOrCreateCurrentUser();
    }

    @Test
    void testUpdateSettingsSuccessfully() throws Exception {
        // Arrange
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest(true);
        when(userService.updateSettings(any(UserSettingsUpdateRequest.class))).thenReturn(testUserResponse);

        String requestBody = new ObjectMapper().writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/user/settings").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).updateSettings(any(UserSettingsUpdateRequest.class));
    }

    @Test
    void testUpdateSettingsWithFalseNotifications() throws Exception {
        // Arrange
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest(false);
        when(userService.updateSettings(any(UserSettingsUpdateRequest.class))).thenReturn(testUserResponse);

        String requestBody = new ObjectMapper().writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/user/settings").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(userService).updateSettings(any(UserSettingsUpdateRequest.class));
    }

    @Test
    void testDeleteMeReturnsNoContent() throws Exception {
        // Arrange
        doNothing().when(userService).anonymizeCurrentUser();

        // Act & Assert
        mockMvc.perform(delete("/api/user/me").with(jwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).anonymizeCurrentUser();
    }

    @Test
    void testUploadProfilePictureSuccessfully() throws Exception {
        // Arrange
        byte[] fileContent = "fake image content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                fileContent
        );

        when(userService.uploadProfilePicture(any(MultipartFile.class))).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/user/me/upload-picture")
                .file(file)
                .with(jwt())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").value("https://example.com/profile.jpg"));

        verify(userService).uploadProfilePicture(any(MultipartFile.class));
    }

    @Test
    void testUploadProfilePictureWithPngFile() throws Exception {
        // Arrange
        byte[] pngContent = "PNG fake content".getBytes();
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                pngContent
        );

        when(userService.uploadProfilePicture(any(MultipartFile.class))).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/user/me/upload-picture")
                .file(pngFile)
                .with(jwt())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(userService).uploadProfilePicture(any(MultipartFile.class));
    }

    @Test
    void testUploadProfilePictureWithWebpFile() throws Exception {
        // Arrange
        byte[] webpContent = "WEBP fake content".getBytes();
        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "profile.webp",
                "image/webp",
                webpContent
        );

        when(userService.uploadProfilePicture(any(MultipartFile.class))).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/user/me/upload-picture")
                .file(webpFile)
                .with(jwt())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(userService).uploadProfilePicture(any(MultipartFile.class));
    }

    @Test
    void testMeEndpointRequiresAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/user/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getOrCreateCurrentUser();
    }

    @Test
    void testUpdateSettingsRequiresAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notificationsEnabled\": true}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateSettings(any());
    }

    @Test
    void testDeleteMeRequiresAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/user/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).anonymizeCurrentUser();
    }

    @Test
    void testUploadProfilePictureRequiresAuthentication() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[0]);

        // Act & Assert
        mockMvc.perform(multipart("/api/user/me/upload-picture")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).uploadProfilePicture(any());
    }

    @Test
    void testUploadProfilePictureWithEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        when(userService.uploadProfilePicture(any(MultipartFile.class))).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/user/me/upload-picture")
                .file(emptyFile).with(jwt())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(userService).uploadProfilePicture(any(MultipartFile.class));
    }

    @Test
    void testMeEndpointReturnsCorrectContentType() throws Exception {
        // Arrange
        when(userService.getOrCreateCurrentUser()).thenReturn(testUserResponse);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/user/me").with(jwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("application/json");
    }

    @Test
    void testUpdateSettingsWithInvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/user/settings").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateSettings(any());
    }

    @Test
    void testUploadProfilePictureWithoutFile() throws Exception {
        // Act & Assert - Missing the file part
        mockMvc.perform(multipart("/api/user/me/upload-picture").with(jwt())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(userService, never()).uploadProfilePicture(any());
    }

    @Test
    void testMeEndpointResponseStructure() throws Exception {
        // Arrange
        when(userService.getOrCreateCurrentUser()).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(get("/api/user/me").with(jwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.keycloakId").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.profilePicture").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser
    void testUpdateSettingsResponseStructure() throws Exception {
        // Arrange
        when(userService.updateSettings(any(UserSettingsUpdateRequest.class))).thenReturn(testUserResponse);

        // Act & Assert
        mockMvc.perform(put("/api/user/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notificationsEnabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.fullName").exists());
    }
}
