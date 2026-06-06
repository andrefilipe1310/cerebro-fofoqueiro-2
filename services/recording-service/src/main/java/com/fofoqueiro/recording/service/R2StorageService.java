package com.fofoqueiro.recording.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public R2StorageService(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.access-key}") String accessKey,
            @Value("${r2.secret-key}") String secretKey,
            @Value("${r2.bucket}") String bucket) {
        this.bucket = bucket;
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
        var uri = URI.create(endpoint);
        this.s3Client = S3Client.builder()
                .endpointOverride(uri)
                .credentialsProvider(credentials)
                .region(Region.US_EAST_1)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(uri)
                .credentialsProvider(credentials)
                .region(Region.US_EAST_1)
                .build();
    }

    public String generatePresignedDownloadUrl(String r2Key, Duration expiry) {
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(r -> r.bucket(bucket).key(r2Key))
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteObject(String r2Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(r2Key).build());
        } catch (Exception e) {
            log.warn("Failed to delete R2 object {}: {}", r2Key, e.getMessage());
        }
    }
}
