package com.viewTrack.controller;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.dto.response.MovieResponseDto;
import com.viewTrack.mapper.MovieMapper;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    private final MovieMapper movieMapper;

    @PostMapping("/add")
    public ResponseEntity<BasicApiResponse<MovieResponseDto>> createMovie(@RequestBody MovieRequestDto movieDto) {
        Movie movie = movieService.createMovie(movieDto);
        MovieResponseDto movieResponseDto = movieMapper.toMovieResponseDto(movie);
        return ResponseEntity.ok(new BasicApiResponse<>(movieResponseDto));
    }

}
