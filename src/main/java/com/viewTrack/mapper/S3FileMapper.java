package com.viewTrack.mapper;

import com.viewTrack.dto.response.ImageResponseDto;
import com.viewTrack.s3storage.S3File;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface S3FileMapper {
    ImageResponseDto toImageResponseDto(S3File s3File);
}
