package com.markish.ajudapp_api.service;

import com.markish.ajudapp_api.common.ApiConstants;
import com.markish.ajudapp_api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class B2ProfileStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${b2.s3.bucket}")
    private String bucket;

    @Override
    public String upload(String key, MultipartFile file) throws IOException {

        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );


            // B2 S3 endpoints serve files like this:
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw e;
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    @Override
    public String getProfileUrl(User user) {
        PresignedGetObjectRequest presigned =
                s3Presigner.presignGetObject(r -> r
                        .signatureDuration(Duration.ofDays(1))
                        .getObjectRequest(g -> g
                                .bucket(bucket)
                                .key(user.getProfilePicture().substring(user.getProfilePicture().indexOf(ApiConstants.ObjectStorage.PROFILES_PREFIX)))
                        )
                );

        return presigned.url().toString();
    }
}
