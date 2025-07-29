package com.viewTrack.mapper;

import com.viewTrack.data.entity.Director;
import com.viewTrack.data.entity.Genre;
import com.viewTrack.data.entity.Movie;
import com.viewTrack.data.entity.Review;
import com.viewTrack.dto.response.MovieResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    @Mapping(target = "genres", source = "genres", qualifiedByName = "mapGenres")
    @Mapping(target = "directors", source = "directors", qualifiedByName = "mapDirectors")
    @Mapping(target = "reviews", source = "reviews", qualifiedByName = "mapReviews")
    MovieResponseDto toMovieResponseDto(Movie movie);

    @Named("mapGenres")
    default List<String> mapGenres(List<Genre> genres) {
        return genres.stream()
                .map(Genre::getGenreName)
                .collect(Collectors.toList());
    }

    @Named("mapDirectors")
    default List<String> mapDirectors(List<Director> directors) {
        return directors.stream()
                .map(Director::getFullName)
                .collect(Collectors.toList());
    }

    @Named("mapReviews")
    default List<String> mapReviews(List<Review> reviews) {
        return reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());
    }
}
