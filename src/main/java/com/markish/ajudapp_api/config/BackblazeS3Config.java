package com.markish.ajudapp_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Slf4j
public class BackblazeS3Config {

    @Value("${b2.s3.endpoint}")
    private String endpoint;

    @Value("${b2.s3.region}")
    private String region;

    @Value("${b2.s3.access-key}")
    private String accessKey;

    @Value("${b2.s3.secret-key}")
    private String secretKey;

    @Bean
    public S3Client b2S3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
                .build();
    }

    @Bean
    public AwsCredentialsProvider b2CredentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        accessKey,
                        secretKey
                )
        );
    }

    @Bean
    public S3Presigner s3Presigner(
            AwsCredentialsProvider credentialsProvider
    ) {
        return S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .build();
    }


}
