package com.cinema.testcinema.config;

import com.cinema.testcinema.model.Role;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class AdminSeeder {
    @Bean
    CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            String adminUser = "admin";
            repo.save(new User(adminUser, encoder.encode("ADMIN"), "ROLE_ADMIN"));
        };
    }
}
