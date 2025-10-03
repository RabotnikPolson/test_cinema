package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    public Movie addMovie(MovieDto movieDto) {
        Movie movie = new Movie();
        movie.setTitle(movieDto.getTitle());
        movie.setYear((long) movieDto.getYear()); // преобразование int -> Long
        movie.setImdbId(movieDto.getImdbId());

        Optional<Genre> genreOpt = genreRepository.findById(movieDto.getGenreId());
        if (genreOpt.isPresent()) {
            movie.setGenre(genreOpt.get());
        } else {
            throw new RuntimeException("Genre not found with id: " + movieDto.getGenreId());
        }

        return movieRepository.save(movie);
    }
}
