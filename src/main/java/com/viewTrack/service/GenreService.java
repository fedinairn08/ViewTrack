package com.viewTrack.service;

import com.viewTrack.data.entity.Genre;

import java.util.List;

public interface GenreService {
    Genre createGenre(String genreName);

    void deleteGenre(long id);

    List<Genre> findAll();

    boolean existsByGenreName(String genreName);
}
