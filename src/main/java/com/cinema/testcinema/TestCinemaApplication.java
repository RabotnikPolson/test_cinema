package com.cinema.testcinema;

import com.cinema.testcinema.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class TestCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCinemaApplication.class, args);
    }

}
