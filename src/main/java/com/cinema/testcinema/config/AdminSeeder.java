package com.cinema.testcinema.config;

import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            String adminUser = "admin";
            if (repo.existsByUsername(adminUser)) {
                return;
            }

            try {
                User admin = new User(adminUser, encoder.encode("ADMIN"), "ROLE_ADMIN");
                repo.save(admin);
            } catch (DataIntegrityViolationException ex) {
                System.out.println("Admin user already exists, skipping creation.");
            }
        };
    }
}
