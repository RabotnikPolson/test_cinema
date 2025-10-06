package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final OmdbService omdbService;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository, OmdbService omdbService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.omdbService = omdbService;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findByType("movie");
    }

    public List<Movie> getAllSeries() {
        return movieRepository.findByType("series");
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Movie addMovie(MovieDto movieDto) {
        if (movieDto.getImdbId() == null || movieDto.getImdbId().isEmpty()) {
            throw new RuntimeException("IMDB ID is required");
        }

        Movie movie = omdbService.getMovieFromOmdb(movieDto.getImdbId());
        if (movie == null) throw new RuntimeException("Movie not found in OMDB");

        if (movieDto.getTitle() != null) movie.setTitle(movieDto.getTitle());
        if (movieDto.getYear() != null) movie.setYear(movieDto.getYear());
        if (movieDto.getType() != null) movie.setType(movieDto.getType()); // movie или series

        Optional<Genre> genreOpt = genreRepository.findById(movieDto.getGenreId());
        genreOpt.ifPresent(movie::setGenre);

        return movieRepository.save(movie);
    }
}
