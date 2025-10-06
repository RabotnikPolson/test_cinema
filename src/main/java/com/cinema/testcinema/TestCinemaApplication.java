package com.cinema.testcinema;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootApplication
public class TestCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCinemaApplication.class, args);
    }

    @Bean
    CommandLineRunner initUsers(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            if (userRepo.count() == 0) {
                User admin = new User("admin", encoder.encode("admin"));
                admin.getRoles().add("ADMIN"); // Теперь это работает, так как Set изменяемый
                userRepo.save(admin);
                System.out.println("✅ Тестовый admin/admin вставлен");
            }
        };
    }

    @Bean
    CommandLineRunner initData(GenreRepository genreRepo, MovieRepository movieRepo) {
        return args -> {
            // Вставляем жанры
            if (genreRepo.count() == 0) {
                Genre drama = new Genre("Драма");
                Genre comedy = new Genre("Комедия");
                Genre sciFi = new Genre("Фантастика");
                Genre horror = new Genre("Ужасы");
                Genre romance = new Genre("Романтика");

                drama = genreRepo.save(drama);
                comedy = genreRepo.save(comedy);
                sciFi = genreRepo.save(sciFi);
                horror = genreRepo.save(horror);
                romance = genreRepo.save(romance);

                System.out.println("✅ Тестовые жанры вставлены: Драма, Комедия, Фантастика, Ужасы, Романтика");
            }

            if (movieRepo.count() == 0) {
                Optional<Genre> dramaOpt = genreRepo.findByName("Драма");
                Optional<Genre> sciFiOpt = genreRepo.findByName("Фантастика");

                if (dramaOpt.isPresent() && sciFiOpt.isPresent()) {
                    Genre dramaGenre = dramaOpt.get();
                    Genre sciFiGenre = sciFiOpt.get();


                    System.out.println("✅ Тестовые фильмы вставлены: Крёстный отец, Интерстеллар");
                } else {
                    System.err.println("❌ Жанры не найдены — фильмы не добавлены");
                }
            } else {
                System.out.println("✅ БД уже содержит данные (пропускаем вставку)");
            }
        };
    }
}