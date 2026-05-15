package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.security.JwtAuthenticationFilter;
import com.cinema.testcinema.security.JwtService;
import com.cinema.testcinema.service.KinopoiskSyncService;
import com.cinema.testcinema.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MovieController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class MovieControllerAddFromKinopoiskTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KinopoiskSyncService kinopoiskSyncService;

    @MockBean
    private MovieRepository movieRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private MovieService movieService;

    @Test
    void addFromKinopoiskReturns400ForBlankId() throws Exception {
        mockMvc.perform(post("/movies/addFromKinopoisk").param("kinopoiskId", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addFromKinopoiskReturns404WhenApiReturnsNotFound() throws Exception {
        when(kinopoiskSyncService.fetchAndSave("99999999"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден в Kinopoisk API"));

        mockMvc.perform(post("/movies/addFromKinopoisk").param("kinopoiskId", "99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFromKinopoiskReturns503WhenApiUnavailable() throws Exception {
        when(kinopoiskSyncService.fetchAndSave(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kinopoisk API unavailable"));

        mockMvc.perform(post("/movies/addFromKinopoisk").param("kinopoiskId", "301"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void addFromKinopoiskReturns200AndMovieJson() throws Exception {
        Movie savedMovie = new Movie();
        savedMovie.setKinopoiskId("301");
        savedMovie.setTitle("Матрица");
        savedMovie.setGenreText("Фантастика, Боевик");
        savedMovie.setGenres(new HashSet<>());
        savedMovie.setImdbRating("8.7");
        savedMovie.setYear(1999L);

        when(kinopoiskSyncService.fetchAndSave("301")).thenReturn(savedMovie);

        mockMvc.perform(post("/movies/addFromKinopoisk").param("kinopoiskId", "301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kinopoiskId").value("301"))
                .andExpect(jsonPath("$.title").value("Матрица"));
    }
}
