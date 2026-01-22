package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static com.markish.ajudapp_api.common.ApiConstants.ObjectStorage.PROFILES_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class B2ProfileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private B2ProfileStorageService storageService;

    private User testUser;
    private String testBucket;

    @BeforeEach
    void setUp() {
        testBucket = "test-bucket";
        testUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId("keycloak-123")
                .email("test@example.com")
                .fullName("Test User")
                .profilePicture("https://example.com/profiles/test-profile.jpg")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Use reflection to set the bucket field
        try {
            java.lang.reflect.Field bucketField = B2ProfileStorageService.class.getDeclaredField("bucket");
            bucketField.setAccessible(true);
            bucketField.set(storageService, testBucket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUploadSuccessfully() throws IOException {
        // Arrange
        String key = "profiles/test-key";
        byte[] fileContent = "test file content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", fileContent);

        var s3UtilitiesMock = mock(software.amazon.awssdk.services.s3.S3Utilities.class);
        when(s3Client.utilities()).thenReturn(s3UtilitiesMock);
        when(s3UtilitiesMock.getUrl(any(Consumer.class))).thenReturn(new URL("https://example.com/profiles/test.jpg"));

        // Act
        String result = storageService.upload(key, file);

        // Assert
        assertThat(result).isEqualTo("https://example.com/profiles/test.jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3UtilitiesMock).getUrl(any(Consumer.class));
    }

    @Test
    void testUploadWithDifferentContentTypes() throws IOException {
        // Arrange
        String[] contentTypes = {"image/jpeg", "image/png", "image/webp"};
        var s3UtilitiesMock = mock(software.amazon.awssdk.services.s3.S3Utilities.class);
        when(s3Client.utilities()).thenReturn(s3UtilitiesMock);
        when(s3UtilitiesMock.getUrl(any(Consumer.class))).thenReturn(new URL("https://example.com/file.jpg"));

        for (String contentType : contentTypes) {
            // Act
            MultipartFile file = new MockMultipartFile("file", "test", contentType, "content".getBytes());
            String result = storageService.upload("profiles/test-" + contentType, file);

            // Assert
            assertThat(result).isNotNull();
        }
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadThrowsExceptionOnIOError() throws IOException {
        // Arrange
        String key = "profiles/test-key";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("Upload failed"));

        // Act & Assert
        assertThatThrownBy(() -> storageService.upload(key, file))
                .isInstanceOf(IOException.class)
                .hasMessage("Upload failed");
    }

    @Test
    void testDeleteSuccessfully() {
        // Act
        storageService.delete("profiles/test-key");

        // Assert
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());

        DeleteObjectRequest request = deleteCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(testBucket);
        assertThat(request.key()).isEqualTo("profiles/test-key");
    }

    @Test
    void testDeleteMultipleKeys() {
        // Arrange
        String[] keys = {"profiles/key1", "profiles/key2", "profiles/key3"};

        // Act
        for (String key : keys) {
            storageService.delete(key);
        }

        // Assert
        verify(s3Client, times(3)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testGetProfileUrl() throws MalformedURLException {
        // Arrange
        testUser.setProfilePicture(PROFILES_PREFIX + "/path/to/profile-" + System.currentTimeMillis() + ".jpg");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://presigned-url.com/profile.jpg?sig=123"));

        when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presignedRequest);

        // Act
        String result = storageService.getProfileUrl(testUser);

        // Assert
        assertThat(result).contains("https://presigned-url.com/profile.jpg");
        verify(s3Presigner).presignGetObject(any(Consumer.class));
    }

    @Test
    void testGetProfileUrlWithComplexPath() throws MalformedURLException {
        // Arrange
        testUser.setProfilePicture(PROFILES_PREFIX + "/complex/nested/path/profile.jpg");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://presigned.com/file.jpg"));

        when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presignedRequest);

        // Act
        String result = storageService.getProfileUrl(testUser);

        // Assert
        assertThat(result)
                .isNotNull()
                .contains("https://presigned.com/file.jpg");
    }

    @Test
    void testUploadPreservesFileMetadata() throws IOException {
        // Arrange
        String key = "profiles/test-key";
        byte[] fileContent = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", fileContent);

        var s3UtilitiesMock = mock(software.amazon.awssdk.services.s3.S3Utilities.class);
        when(s3Client.utilities()).thenReturn(s3UtilitiesMock);
        when(s3UtilitiesMock.getUrl(any(Consumer.class))).thenReturn(new URL("https://example.com/file.jpg"));

        // Act
        storageService.upload(key, file);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(testBucket);
        assertThat(request.key()).isEqualTo(key);
        assertThat(request.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void testUploadEmptyFile() throws IOException {
        // Arrange
        String key = "profiles/empty";
        MultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        var s3UtilitiesMock = mock(software.amazon.awssdk.services.s3.S3Utilities.class);
        when(s3Client.utilities()).thenReturn(s3UtilitiesMock);
        when(s3UtilitiesMock.getUrl(any(Consumer.class))).thenReturn(new URL("https://example.com/empty.jpg"));

        // Act
        String result = storageService.upload(key, file);

        // Assert
        assertThat(result).isNotNull();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDeleteNonExistentKey() {
        // Act - should not throw exception
        storageService.delete("non/existent/key");

        // Assert
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
