package com.viewTrack.controller;

import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.response.ImageResponseDto;
import com.viewTrack.mapper.S3FileMapper;
import com.viewTrack.s3storage.S3Client;
import com.viewTrack.s3storage.S3File;
import com.viewTrack.service.ImageService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/api/image")
@AllArgsConstructor
public class ImageController {

    private final ImageService imageService;

    private final S3Client s3Client;

    private final S3FileMapper s3FileMapper;

    private final String BUCKET_NAME = "view-track";

    @GetMapping( "/get/{filename}")
    public ResponseEntity<Resource> getImageByFilename(@PathVariable String filename) {
        try {
            byte[] imageBytes = s3Client.get(filename, BUCKET_NAME);
            ByteArrayResource resource = new ByteArrayResource(imageBytes);

            String contentType = guessContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(imageBytes.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BasicApiResponse<ImageResponseDto>> uploadImage(
            @RequestPart("file") MultipartFile file) {
        S3File s3File = imageService.upload(file);
        return ResponseEntity.ok(new BasicApiResponse<>(s3FileMapper.toImageResponseDto(s3File)));
    }

    private String guessContentType(String filename) {
        if (filename.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
