package com.viewTrack.service.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import com.viewTrack.data.entity.Image;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.s3storage.S3File;
import com.viewTrack.service.FileService;
import com.viewTrack.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;

    private final FileService fileService;

    @Override
    @SneakyThrows
    public S3File upload(MultipartFile file) {
        S3File s3File = new S3File(file.getOriginalFilename(), file.getBytes());
        fileService.save(s3File);
        return s3File;
    }

    @Override
    public void delete(String filename) {
        fileService.delete(filename);
    }

    @Override
    public S3File getMediaFileByUuid(UUID uuid) {
        Image photo = imageRepository.findByUploadId(uuid).orElseThrow(() ->
                new NotFoundException(String.format("Photo with uuid: %s -- is not found", uuid)));
        return fileService.get(photo.getFilename());
    }
}
