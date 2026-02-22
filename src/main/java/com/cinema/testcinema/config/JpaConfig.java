package com.cinema.testcinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.boot.autoconfigure.domain.EntityScan;

@Configuration
@EnableJpaRepositories("com.cinema.testcinema.repository")
@EntityScan("com.cinema.testcinema.model")
public class JpaConfig {
}
