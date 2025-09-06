package com.doc_manager.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService {

    private MinioClient minioClient;

    @Value("${app.minio.url}") private String url;
    @Value("${app.minio.access-key}") private String accessKey;
    @Value("${app.minio.secret-key}") private String secretKey;
    @Value("${app.minio.bucket}") private String bucket;

    @PostConstruct
    public void init() throws Exception {
        minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if(!found){
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    public void putFile(String objectName, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    public InputStream getFileStream(String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    public void removeFile(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    public String getPresignedUrl(String objectName, int expireMinutes) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(expireMinutes, TimeUnit.MINUTES)
                        .build()
        );
    }

    public String getContentType(String objectName) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build());
        return stat.contentType();
    }
}
