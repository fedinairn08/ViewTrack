package com.viewTrack.controller;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.dto.BasicApiResponse;
import com.viewTrack.dto.request.MovieRequestDto;
import com.viewTrack.dto.response.MovieResponseDto;
import com.viewTrack.mapper.MovieMapper;
import com.viewTrack.service.GenreService;
import com.viewTrack.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    private final MovieMapper movieMapper;

    private final GenreService genreService;

    @PostMapping("/add")
    public ResponseEntity<BasicApiResponse<MovieResponseDto>> createMovie(@RequestBody MovieRequestDto movieDto) {
        Movie movie = movieService.createMovie(movieDto);
        MovieResponseDto movieResponseDto = movieMapper.toMovieResponseDto(movie);
        return ResponseEntity.ok(new BasicApiResponse<>(movieResponseDto));
    }

    @GetMapping("/all")
    public String showAllMoviesForm(@RequestParam(required = false) String sort,
                                    @RequestParam(required = false) String genre,
                                    @RequestParam(required = false) String year,
                                    Model model) {
        List<Genre> allGenres = genreService.findAll();
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentYear", year);

        List<Movie> movies = movieService.getMovies(sort, genre, year);
        model.addAttribute("movies", movies);
        model.addAttribute("title", "Фильмы");
        return "allFilms";
    }
}
