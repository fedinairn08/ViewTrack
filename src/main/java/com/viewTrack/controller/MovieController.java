package com.viewTrack.controller;

import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.dto.response.MovieResponseDto;
import com.viewTrack.mapper.MovieMapper;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
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

    @GetMapping("/all")
    public String showSignInForm(Model model) {
        List<Movie> movies = movieService.getAllMovies();
        model.addAttribute("movies", movies);
        model.addAttribute("title", "Фильмы");
        return "allFilms";
    }
}
