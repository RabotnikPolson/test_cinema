package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.*;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User u = new User(req.username(), encoder.encode(req.password()), "ROLE_USER");
        users.save(u);
        String token = jwt.generate(u.getUsername(), Map.of("role", u.getRole()));
        return new AuthResponse(token, u.getUsername());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User u = users.findByUsername(req.username()).orElseThrow();
        String token = jwt.generate(u.getUsername(), Map.of("role", u.getRole()));
        return new AuthResponse(token, u.getUsername());
    }
}
