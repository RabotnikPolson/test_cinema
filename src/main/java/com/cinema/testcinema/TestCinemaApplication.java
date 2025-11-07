package com.cinema.testcinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories("com.cinema.testcinema.repository")
@EntityScan("com.cinema.testcinema.model")

@SpringBootApplication
public class TestCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCinemaApplication.class, args);
    }

}
