package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.MovieService;
import com.cinema.testcinema.service.OmdbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class MovieControllerAddFromImdbTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OmdbService omdbService;

    @MockBean
    private MovieRepository movieRepository;

    @MockBean
    private GenreRepository genreRepository;

    @MockBean
    private MovieService movieService;

    @Test
    void addFromImdbReturns400ForInvalidImdbId() throws Exception {
        mockMvc.perform(post("/movies/addFromImdb").param("imdbId", "123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addFromImdbReturns404ForOmdbNotFound() throws Exception {
        when(omdbService.fetchMovieOrThrow("tt1234567"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден в OMDb API"));

        mockMvc.perform(post("/movies/addFromImdb").param("imdbId", "tt1234567"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFromImdbReturns503ForOmdbTimeout() throws Exception {
        when(omdbService.fetchMovieOrThrow("tt1234567"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OMDb timeout/unavailable"));

        mockMvc.perform(post("/movies/addFromImdb").param("imdbId", "tt1234567"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void addFromImdbReturns502ForOmdbNetworkError() throws Exception {
        when(omdbService.fetchMovieOrThrow("tt1234567"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb gateway error"));

        mockMvc.perform(post("/movies/addFromImdb").param("imdbId", "tt1234567"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void addFromImdbReturns200ForSuccess() throws Exception {
        Movie movie = new Movie();
        movie.setImdbId("tt1234567");
        movie.setTitle("Test movie");
        movie.setGenreText("Action, Drama");

        Genre genre = new Genre();
        genre.setId(10L);
        genre.setName("Action");

        when(omdbService.fetchMovieOrThrow("tt1234567")).thenReturn(movie);
        when(genreRepository.findByName("Action")).thenReturn(genre);
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/movies/addFromImdb").param("imdbId", "tt1234567"))
                .andExpect(status().isOk());
    }
}
