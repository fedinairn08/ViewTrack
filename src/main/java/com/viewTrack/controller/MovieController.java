package com.viewTrack.controller;

import com.viewTrack.data.entity.*;
import com.viewTrack.data.repository.ReviewRepository;
import com.viewTrack.exeption.ResourceNotFoundException;
import com.viewTrack.service.*;
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

    private final ReviewRepository reviewRepository;

    private final DirectorService directorService;
    
    private final AiReviewService aiReviewService;

    @GetMapping("/all")
    public String showAllMoviesForm(@RequestParam(required = false) String sort,
                                    @RequestParam(required = false) String genre,
                                    @RequestParam(required = false) String year,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) Long director,
                                    Model model) {
        List<Genre> allGenres = genreService.findAll();
        List<Director> allDirectors = directorService.findAll();
        User currentUser = authUtils.getUserEntity();

        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allDirectors", allDirectors);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentYear", year);
        model.addAttribute("currentDirector", director);
        model.addAttribute("user", currentUser);

        List<Movie> movies = movieService.getMovies(sort, genre, year, search, director);
        model.addAttribute("movies", movies);
        model.addAttribute("searchTerm", search);
        model.addAttribute("title", "Фильмы");
        return "allFilms";
    }

    @GetMapping("/to-watch")
    public String showToWatchList(@RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String genre,
                                  @RequestParam(required = false) String year,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(required = false) Long director,
                                  Model model) {
        User currentUser = authUtils.getUserEntity();

        List<Genre> allGenres = genreService.findAll();
        List<Director> allDirectors = directorService.findAll();
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allDirectors", allDirectors);

        List<Movie> movies = userMovieService.getToWatchList(currentUser, sort, genre, year, search, director);

        model.addAttribute("movies", movies);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentYear", year);
        model.addAttribute("currentDirector", director);
        model.addAttribute("user", currentUser);
        model.addAttribute("searchTerm", search);
        model.addAttribute("title", "Буду смотреть");
        model.addAttribute("active", "to-watch");

        return "to-watch";
    }

    @GetMapping("/watched")
    public String showWatchedList(@RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String genre,
                                  @RequestParam(required = false) String year,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(required = false) Long director,
                                  Model model) {
        User currentUser = authUtils.getUserEntity();

        List<Genre> allGenres = genreService.findAll();
        List<Director> allDirectors = directorService.findAll();
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allDirectors", allDirectors);

        List<Movie> movies = userMovieService.getWatchedList(currentUser, sort, genre, year, search, director);

        model.addAttribute("movies", movies);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentYear", year);
        model.addAttribute("currentDirector", director);
        model.addAttribute("user", currentUser);
        model.addAttribute("searchTerm", search);
        model.addAttribute("title", "Просмотренное");
        model.addAttribute("active", "watched");

        return "watched";
    }

    @GetMapping("/{id}")
    public String getMovieDetails(@PathVariable Long id, Model model) {
        Movie movie = movieService.getMovieById(id);
        User currentUser = authUtils.getUserEntity();

        Integer userRating = null;
        String userReviewContent = null;
        boolean inToWatchList;
        boolean inWatched;

        List<Review> movieReviews = reviewRepository.findByMovieId(id);

        Long ratingsCount = reviewRepository.countByMovie(movie);

        if (currentUser != null) {
            Optional<Review> userReview = reviewRepository.findByUserAndMovie(currentUser, movie);

            if (userReview.isPresent()) {
                userRating = userReview.get().getRating();
                userReviewContent = userReview.get().getContent();
            }
        }

        inToWatchList = userMovieService.isInToWatchList(movie.getId());
        inWatched = userMovieService.isInWatchedList(movie.getId());
        
        model.addAttribute("movie", movie);
        model.addAttribute("userRating", userRating);
        model.addAttribute("userReviewContent", userReviewContent);
        model.addAttribute("inToWatchList", inToWatchList);
        model.addAttribute("user", currentUser);
        model.addAttribute("inWatched", inWatched);
        model.addAttribute("title", movie.getTitle());
        model.addAttribute("movieReviews", movieReviews);
        model.addAttribute("ratingsCount", ratingsCount);

        AiReview aiReview = aiReviewService.getOrGenerateReviewForMovie(id);
        model.addAttribute("aiReview", aiReview);

        return "movie-detail";
    }

    @GetMapping("/directors")
    public String directorsPage(@RequestParam(required = false) String sort,
                               @RequestParam(required = false) String search,
                               Model model) {
        User currentUser = authUtils.getUserEntity();
        List<Director> directors = directorService.getDirectors(sort, search);

        model.addAttribute("directors", directors);
        model.addAttribute("user", currentUser);
        model.addAttribute("active", "directors");
        model.addAttribute("currentSort", sort);
        model.addAttribute("searchTerm", search);

        return "directors";
    }

    @GetMapping("/director/{id}")
    public String directorDetailPage(@PathVariable Long id, Model model) {
        User currentUser = authUtils.getUserEntity();
        Director director = directorService.getDirectorById(id);
        
        if (director == null) {
            throw new ResourceNotFoundException("Режиссер не найден");
        }

        List<Movie> movies = movieService.getMoviesByDirectorOrdered(id);

        model.addAttribute("director", director);
        model.addAttribute("movies", movies);
        model.addAttribute("user", currentUser);

        return "director-detail";
    }
}
