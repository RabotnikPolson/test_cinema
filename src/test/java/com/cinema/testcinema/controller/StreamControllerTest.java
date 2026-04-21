//package com.cinema.testcinema.controller;
//
//import com.cinema.testcinema.model.Movie;
//import com.cinema.testcinema.repository.MovieRepository;
//import com.cinema.testcinema.security.JwtAuthenticationFilter;
//import com.cinema.testcinema.security.JwtService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Optional;
//
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(controllers = StreamController.class, excludeAutoConfiguration = {
//                SecurityAutoConfiguration.class,
//                SecurityFilterAutoConfiguration.class
//})
//@AutoConfigureMockMvc(addFilters = false)
//@TestPropertySource(properties = "vidsrc.base-url=https://vidsrc-embed.ru")
//class StreamControllerTest {
//
//        @Autowired
//        private MockMvc mockMvc;
//
//        @MockBean
//        private MovieRepository movieRepository;
//
//        @MockBean
//        private JwtService jwtService;
//
//        @MockBean
//        private JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @Test
//    void streamReturns404WhenMovieNotFound() throws Exception {
//        when(movieRepository.findById(42L)).thenReturn(Optional.empty());
//
//        mockMvc.perform(get("/stream/42"))
//                .andExpect(status().isNotFound());
//    }
//
//        @Test
//        void streamReturns409WhenMovieHasNoImdbId() throws Exception {
//                Movie movie = new Movie();
//                movie.setId(2L);
//
//                when(movieRepository.findById(2L)).thenReturn(Optional.of(movie));
//
//                mockMvc.perform(get("/stream/2"))
//                                .andExpect(status().isConflict());
//        }
//
//        @Test
//        void streamReturns400ForInvalidAutoplay() throws Exception {
//                Movie movie = new Movie();
//                movie.setId(3L);
//                movie.setImdbId("tt1234567");
//
//                when(movieRepository.findById(3L)).thenReturn(Optional.of(movie));
//
//                mockMvc.perform(get("/stream/3").param("autoplay", "2"))
//                                .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        void streamReturns400ForInvalidSubUrl() throws Exception {
//                Movie movie = new Movie();
//                movie.setId(4L);
//                movie.setImdbId("tt1234567");
//
//                when(movieRepository.findById(4L)).thenReturn(Optional.of(movie));
//
//                mockMvc.perform(get("/stream/4").param("sub_url", "ftp://example.com/subs.srt"))
//                                .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        void streamReturns200AndEmbedUrlWhenMovieExists() throws Exception {
//                Movie movie = new Movie();
//                movie.setId(1L);
//                movie.setImdbId("tt5433140");
//
//                when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
//
//                mockMvc.perform(get("/stream/1")
//                                .param("ds_lang", "en")
//                                .param("autoplay", "1")
//                                .param("sub_url", "https://cdn.example.com/subs/test.vtt"))
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$.type").value("embed"))
//                                // Не привязываемся к порядку query params:
//                                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith(
//                                                "https://vidsrc-embed.ru/embed/movie?imdb=tt5433140")))
//                                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("ds_lang=en")))
//                                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("autoplay=1")))
//                                // закодированный sub_url должен быть в итоговой ссылке:
//                                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString(
//                                                "sub_url=https%3A%2F%2Fcdn.example.com%2Fsubs%2Ftest.vtt")));
//        }
//
//        @Test
//        void streamReturns200AndVbdkvUrlWhenKinopoiskIdIsPresent() throws Exception {
//                Movie movie = new Movie();
//                movie.setId(20L);
//                movie.setImdbId("tt5433140"); // should be ignored
//                movie.setKinopoiskId("5433140");
//
//                when(movieRepository.findById(20L)).thenReturn(Optional.of(movie));
//
//                mockMvc.perform(get("/stream/20"))
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$.type").value("embed"))
//                                .andExpect(jsonPath("$.url").value("https://vbdkv.com/api/short/5433140"));
//        }
//}
