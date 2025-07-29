package com.viewTrack.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class ImageResponseDto {
    private UUID uploadId;

    private String filename;
}
