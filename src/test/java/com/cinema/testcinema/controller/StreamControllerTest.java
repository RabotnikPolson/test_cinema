package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StreamController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "vidsrc.base-url=https://vidsrc-embed.ru")
class StreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieRepository movieRepository;

    @Test
    void streamReturnsEmbedUrlWithOptionalParams() throws Exception {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setImdbId("tt5433140");

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        mockMvc.perform(get("/stream/1")
                        .param("ds_lang", "en")
                        .param("autoplay", "1")
                        .param("sub_url", "https://cdn.example.com/subs/test.vtt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("embed"))
                .andExpect(jsonPath("$.url").value("https://vidsrc-embed.ru/embed/movie?imdb=tt5433140&ds_lang=en&autoplay=1&sub_url=https%3A%2F%2Fcdn.example.com%2Fsubs%2Ftest.vtt"));
    }

    @Test
    void streamReturns404WhenMovieNotFound() throws Exception {
        when(movieRepository.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/stream/42"))
                .andExpect(status().isNotFound());
    }

    @Test
    void streamReturns409WhenMovieHasNoImdbId() throws Exception {
        Movie movie = new Movie();
        movie.setId(2L);

        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie));

        mockMvc.perform(get("/stream/2"))
                .andExpect(status().isConflict());
    }
}
