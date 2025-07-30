package com.viewTrack.service.impl;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.repository.GenreRepository;
import com.viewTrack.dto.request.GenreRequestDto;
import com.viewTrack.mapper.GenreMapper;
import com.viewTrack.service.GenreService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    private final GenreMapper genreMapper;

    @Override
    public Genre createGenre(GenreRequestDto dto) {
        if (genreRepository.existsByGenreNameIgnoreCase(dto.getGenreName())) {
            throw new EntityExistsException("Жанр с таким названием уже существует");
        }

        Genre genre = genreMapper.toGenre(dto);
        return genreRepository.save(genre);
    }

    @Override
    public void deleteGenre(long id) {
        genreRepository.deleteById(id);
    }

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }
}
