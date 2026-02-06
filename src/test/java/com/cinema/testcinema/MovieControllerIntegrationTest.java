//package com.cinema.testcinema;
//
//import com.cinema.testcinema.model.Genre;
//import com.cinema.testcinema.model.Movie;
//import com.cinema.testcinema.repository.GenreRepository;
//import com.cinema.testcinema.repository.MovieRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * Integration tests for the {@code /movies} endpoint.  These tests spin up the full
// * application context, insert sample data via repositories, and verify that the
// * controller returns arrays of movies consistent with the filtering and paging
// * rules defined in the requirements.  Each test runs in its own transaction to
// * ensure isolation.
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//class MovieControllerIntegrationTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    @Autowired
//    GenreRepository genreRepository;
//
//    @Autowired
//    MovieRepository movieRepository;
//
//    @Test
//    void getAllMovies_withoutParams_returnsAll() throws Exception {
//        Genre g = new Genre();
//        g.setName("TestGenre");
//        genreRepository.save(g);
//
//        Movie m1 = new Movie();
//        m1.setTitle("Movie One");
//        m1.setYear(2020L);
//        m1.getGenres().add(g);
//        movieRepository.save(m1);
//
//        Movie m2 = new Movie();
//        m2.setTitle("Second Movie");
//        m2.setYear(2021L);
//        m2.getGenres().add(g);
//        movieRepository.save(m2);
//
//        mockMvc.perform(get("/movies"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2));
//    }
//
//    @Test
//    void searchMovies_byTitle_findsMatch() throws Exception {
//        Genre g = new Genre();
//        g.setName("Sci-Fi");
//        genreRepository.save(g);
//
//        Movie inter = new Movie();
//        inter.setTitle("Interstellar");
//        inter.setYear(2014L);
//        inter.getGenres().add(g);
//        movieRepository.save(inter);
//
//        Movie star = new Movie();
//        star.setTitle("Star Wars");
//        star.setYear(1977L);
//        star.getGenres().add(g);
//        movieRepository.save(star);
//
//        mockMvc.perform(get("/movies").param("q", "stell"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].title").value("Interstellar"));
//    }
//
//    @Test
//    void filterMovies_byGenre_andPaginate() throws Exception {
//        Genre g1 = new Genre();
//        g1.setName("Comedy");
//        genreRepository.save(g1);
//
//        Genre g2 = new Genre();
//        g2.setName("Drama");
//        genreRepository.save(g2);
//
//        // create three comedies and one drama
//        for (int i = 0; i < 3; i++) {
//            Movie m = new Movie();
//            m.setTitle("Comedy " + i);
//            m.setYear(2000L + i);
//            m.getGenres().add(g1);
//            movieRepository.save(m);
//        }
//
//        Movie drama = new Movie();
//        drama.setTitle("Serious Movie");
//        drama.setYear(2015L);
//        drama.getGenres().add(g2);
//        movieRepository.save(drama);
//
//        mockMvc.perform(get("/movies")
//                        .param("genreId", String.valueOf(g1.getId()))
//                        .param("page", "0")
//                        .param("size", "2"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2))
//                // ensure each returned movie has our genre (first genre in list)
//                .andExpect(jsonPath("$[0].genres[0].id").value(g1.getId()))
//                .andExpect(jsonPath("$[1].genres[0].id").value(g1.getId()));
//    }
//}
