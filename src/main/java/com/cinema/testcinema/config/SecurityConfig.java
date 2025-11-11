package com.cinema.testcinema.config;

import com.cinema.testcinema.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS первым, потом CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))

                // правила доступа
                .authorizeHttpRequests(auth -> auth
                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // публичные GET-запросы
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/api/movies/**",
                                "/api/reviews/**",
                                "/api/comments/**",
                                "/genres/**",
                                "/movies/**",
                                "/static/**",
                                "/public/**"
                        ).permitAll()

                        // регистрация, логин, swagger и внутренние проверки
                        .requestMatchers(
                                "/api/auth/**",
                                "/internal/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // админ-зона
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // всё остальное требует токен
                        .anyRequest().authenticated()
                )

                // JWT фильтр перед UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://*.ngrok-free.app",
                "https://*.devtunnels.ms"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        cfg.setAllowCredentials(true); // нужно true, иначе браузер не пошлёт Authorization
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (req, res, ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            try {
                res.getWriter().write("{\"error\":\"Unauthorized\"}");
            } catch (Exception ignored) {}
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
