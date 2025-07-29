package com.viewTrack.s3storage.impl;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.viewTrack.s3storage.S3Client;
import com.viewTrack.s3storage.S3File;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MinIoClient implements S3Client {

    private final AmazonS3 s3;

    public void createBucketIfNotExist(String bucket) {
        if (!s3.doesBucketExistV2(bucket)) {
            s3.createBucket(bucket);
        }
    }

    @Override
    @SneakyThrows
    public void put(S3File s3File, String bucket) {
        byte[] content = s3File.getContent();
        @Cleanup InputStream inputStream = new ByteArrayInputStream(content);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);

        s3.putObject(new PutObjectRequest(bucket, s3File.getFilename(), inputStream, metadata));
    }

    @Override
    @SneakyThrows
    public byte[] get(String filename, String bucket) {
        S3Object s3Object = s3.getObject(
                new GetObjectRequest(bucket, filename)
        );
        return s3Object.getObjectContent().readAllBytes();
    }

    @Override
    public boolean isFileExist(String filename, String bucket) {
        return s3.doesObjectExist(bucket, filename);
    }

    @Override
    public void delete(String filename, String bucket) {
        s3.deleteObject(new DeleteObjectRequest(bucket, filename));
    }
}
