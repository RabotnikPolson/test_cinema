package com.cinema.testcinema;

import com.cinema.testcinema.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableCaching
public class TestCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCinemaApplication.class, args);
    }

}
//
//public void deleteMovie(Long id) {
//
//    if (!movieRepository.existsById(id)) {
//
//        throw new RuntimeException("Фильм с ID " + id + " не найден");
//
//    }
//
//    movieRepository.deleteById(id);
//
//}
//
//
//
//@DeleteMapping("/{id}")
//
//@PreAuthorize("hasRole('ADMIN')")
//
//@Operation(summary = "Удалить фильм по ID (ADMIN). CASCADE удалит субтитры, рейтинги и историю.")
//
//public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
//
//    movieService.deleteMovie(id);
//
//    return ResponseEntity.noContent().build();
//
//}
