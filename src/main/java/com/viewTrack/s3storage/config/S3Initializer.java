package com.viewTrack.s3storage.config;

import com.viewTrack.s3storage.S3Client;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3Initializer implements CommandLineRunner {

    private final S3Client s3Client;

    private final S3Properties s3Properties;

    @SneakyThrows
    @Override
    public void run(String... args) {
        s3Client.createBucketIfNotExist(s3Properties.getBucket());
    }
}
