package com.viewTrack.service.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import com.viewTrack.data.entity.Image;
import com.viewTrack.data.entity.User;
import com.viewTrack.data.repository.ImageRepository;
import com.viewTrack.data.repository.UserRepository;
import com.viewTrack.s3storage.S3File;
import com.viewTrack.service.FileService;
import com.viewTrack.service.ImageService;
import jakarta.persistence.EntityNotFoundException;
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

    private final UserRepository userRepository;

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

    @Override
    public String uploadProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Поддерживаются только изображения");
        }

        S3File s3File = upload(file);

        Image image = Image.builder()
                .filename(s3File.getFilename())
                .uploadId(s3File.getUploadId())
                .build();

        Image savedImage = imageRepository.save(image);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if (user.getProfileImage() != null) {
            Image oldImage = user.getProfileImage();

            delete(oldImage.getFilename());

            user.setProfileImage(null);

            imageRepository.delete(oldImage);
        }

        user.setProfileImage(savedImage);
        userRepository.save(user);

        return s3File.getFilename();
    }
}
