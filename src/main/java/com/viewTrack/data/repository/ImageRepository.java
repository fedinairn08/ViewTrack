package com.viewTrack.data.repository;

import com.viewTrack.data.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByFilename(String filename);

    Optional<Image> findByUploadId(UUID uploadId);
}
