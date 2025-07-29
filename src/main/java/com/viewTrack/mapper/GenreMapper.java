package com.viewTrack.mapper;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.dto.request.GenreRequestDto;
import com.viewTrack.dto.response.GenreResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    Genre toGenre(GenreRequestDto dto);

    GenreResponseDto toGenreResponseDto(Genre genre);
}
