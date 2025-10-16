package com.example.docudeep.service.storage;

import com.example.docudeep.config.StorageProperties;
import com.example.docudeep.service.storage.dto.PresignedUpload;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final StorageProperties properties;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Override
    public PresignedUpload prepareUpload(String storageKey, String mimeType, long contentLength) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .contentType(mimeType)
                .contentLength(contentLength)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(properties.getPresignTtl())
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        Map<String, String> headers = new HashMap<>();
        presigned.httpRequest().headers().forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        });

        return new PresignedUpload(presigned.url().toString(), presigned.httpRequest().method().name(), headers, properties.getPresignTtl());
    }

    @Override
    public byte[] load(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .build();
        return s3Client.getObject(request, ResponseTransformer.toBytes()).asByteArray();
    }

    @Override
    public String resolveUrl(String storageKey) {
        return "s3://" + properties.getBucket() + "/" + storageKey;
    }
}
