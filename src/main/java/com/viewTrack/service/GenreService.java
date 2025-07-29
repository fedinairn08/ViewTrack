package com.viewTrack.service;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.dto.request.GenreRequestDto;

public interface GenreService {
    Genre createGenre(GenreRequestDto dto);

    void deleteGenre(long id);
}
