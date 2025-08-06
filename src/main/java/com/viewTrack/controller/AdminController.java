package com.viewTrack.controller;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.User;
import com.viewTrack.service.DirectorService;
import com.viewTrack.service.GenreService;
import com.viewTrack.service.MovieService;
import com.viewTrack.service.RoleService;
import com.viewTrack.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MovieService movieService;

    private final GenreService genreService;

    private final DirectorService directorService;

    private final AuthUtils authUtils;

    private final RoleService roleService;

    @GetMapping("/movies")
    public String adminMoviesPage(@RequestParam(required = false) String sort,
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

        return "admin/movies";
    }

    @GetMapping("/movies/add")
    public String addMovieForm(Model model) {
        User currentUser = authUtils.getUserEntity();

        List<Genre> genres = genreService.findAll();
        List<Director> directors = directorService.findAll();

        model.addAttribute("genres", genres);
        model.addAttribute("directors", directors);
        model.addAttribute("user", currentUser);
        model.addAttribute("title", "Добавить новый фильм");

        return "admin/add-movie";
    }
}
