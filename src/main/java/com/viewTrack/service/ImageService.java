package com.viewTrack.service;

import com.viewTrack.s3storage.S3File;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageService {
    S3File upload(MultipartFile file);

    void delete(String filename);

    S3File getMediaFileByUuid(UUID uuid);
}
