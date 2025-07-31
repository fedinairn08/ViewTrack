package com.viewTrack.controller;

import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.data.entity.User;
import com.viewTrack.service.GenreService;
import com.viewTrack.service.MovieService;
import com.viewTrack.service.UserMovieService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    private final GenreService genreService;

    private final AuthUtils authUtils;

    private final UserMovieService userMovieService;

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

    @GetMapping("/to-watch")
    public String showToWatchList(@RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String genre,
                                  @RequestParam(required = false) String year,
                                  Model model) {
        User currentUser = authUtils.getUserEntity();

        List<Genre> allGenres = genreService.findAll();
        model.addAttribute("allGenres", allGenres);

        // Получаем фильмы из списка "Буду смотреть"
        List<Movie> movies = userMovieService.getToWatchList(currentUser, sort, genre, year);

        model.addAttribute("movies", movies);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentYear", year);
        model.addAttribute("title", "Буду смотреть");
        model.addAttribute("active", "to-watch");

        return "to-watch";
    }

    @GetMapping("/{id}")
    public String getMovieDetails(@PathVariable Long id, Model model) {
        Movie movie = movieService.getMovieById(id);
        User currentUser = authUtils.getUserEntity();

        Integer userRating = null;
        boolean inToWatchList = false;
        if (currentUser != null) {
            Optional<Review> userReview = movie.getReviews().stream()
                    .filter(review -> review.getUser().getId().equals(currentUser.getId()))
                    .findFirst();

            if (userReview.isPresent()) {
                userRating = userReview.get().getRating();
            }
        }

        inToWatchList = userMovieService.isInToWatchList(movie.getId());

        model.addAttribute("movie", movie);
        model.addAttribute("userRating", userRating);
        model.addAttribute("inToWatchList", inToWatchList);
        model.addAttribute("title", movie.getTitle());

        return "movie-detail";
    }
}
