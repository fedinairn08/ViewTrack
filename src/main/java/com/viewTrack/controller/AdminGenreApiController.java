package com.viewTrack.controller;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/genres")
@RequiredArgsConstructor
public class AdminGenreApiController {

    private final GenreService genreService;

    @PostMapping
    public ResponseEntity<Genre> addGenre(@RequestParam String genreName) {
        if (genreService.existsByGenreName(genreName)) {
            throw new IllegalArgumentException("Жанр с таким названием уже существует");
        }

        Genre genre = genreService.createGenre(genreName);
        return ResponseEntity.ok(genre);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}
