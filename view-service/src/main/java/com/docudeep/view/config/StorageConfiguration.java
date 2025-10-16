package com.example.docudeep.config;

import com.example.docudeep.service.storage.S3StorageService;
import com.example.docudeep.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class StorageConfiguration {

    private final StorageProperties properties;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(properties.getRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(properties.getRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public StorageService storageService(S3Presigner presigner, S3Client s3Client) {
        return new S3StorageService(properties, presigner, s3Client);
    }
}
