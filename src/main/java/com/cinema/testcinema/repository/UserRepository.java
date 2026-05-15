package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles") // <---- ЛОВИ
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles") // <---- Тоже
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
