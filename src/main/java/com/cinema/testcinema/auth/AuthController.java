package com.cinema.testcinema.auth;

import com.cinema.testcinema.auth.dto.AuthResponse;
import com.cinema.testcinema.auth.dto.LoginRequest;
import com.cinema.testcinema.auth.dto.RefreshRequest;
import com.cinema.testcinema.auth.dto.RegisterRequest;
import com.cinema.testcinema.model.RefreshToken;
import com.cinema.testcinema.model.Role;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.RoleRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.security.JwtService;
import com.cinema.testcinema.security.RefreshTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticatedUserService authenticatedUserService;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          AuthenticatedUserService authenticatedUserService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register attempt for email={} username={}", request.email(), request.username());
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Имя пользователя уже занято");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        Role roleUser = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Роль ROLE_USER не найдена"));
        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        refreshTokenService.revokeAllForUser(savedUser.getId());
        refreshTokenService.purgeExpired();
        RefreshToken refreshToken = refreshTokenService.create(savedUser);
        String accessToken = jwtService.generateAccessToken(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.of(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email={}", request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials for email={}", request.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь отключен");
        }

        refreshTokenService.revokeAllForUser(user.getId());
        refreshTokenService.purgeExpired();
        RefreshToken refreshToken = refreshTokenService.create(user);
        String accessToken = jwtService.generateAccessToken(user);

        return ResponseEntity.ok(AuthResponse.of(accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.purgeExpired();
        RefreshToken stored = refreshTokenService.validate(request.refreshToken());
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Пользователь отключен");
        }

        RefreshToken rotated = refreshTokenService.rotate(stored);
        String accessToken = jwtService.generateAccessToken(user);

        return ResponseEntity.ok(AuthResponse.of(accessToken, rotated.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        refreshTokenService.revokeAllForUser(userId);
        return ResponseEntity.noContent().build();
    }
}
