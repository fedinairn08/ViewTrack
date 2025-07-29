package com.viewTrack.mapper;

import com.viewTrack.data.entity.Image;
import com.viewTrack.dto.response.ImageResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    ImageResponseDto toImageResponseDto(Image image);
}
