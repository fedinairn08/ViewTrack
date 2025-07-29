package com.viewTrack.controller;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.EmptyApiResponse;
import com.viewTrack.dto.request.GenreRequestDto;
import com.viewTrack.dto.response.GenreResponseDto;
import com.viewTrack.mapper.GenreMapper;
import com.viewTrack.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/genre")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    private final GenreMapper genreMapper;

    @PostMapping("/add")
    public ResponseEntity<BasicApiResponse<GenreResponseDto>> createGenre(@RequestBody GenreRequestDto genreRequestDto) {
        Genre genre = genreService.createGenre(genreRequestDto);
        GenreResponseDto genreResponseDto = genreMapper.toGenreResponseDto(genre);
        return ResponseEntity.ok(new BasicApiResponse<>(genreResponseDto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<EmptyApiResponse> deleteGenre(@RequestParam Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.ok(new EmptyApiResponse());
    }
}
