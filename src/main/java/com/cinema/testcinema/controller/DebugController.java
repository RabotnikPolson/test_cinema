package com.cinema.testcinema.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/internal/debug")
public class DebugController {

    @GetMapping("/token")
    public Map<String, Object> token(HttpServletRequest req, Authentication auth) {
        return Map.of(
                "authPresent", auth != null,
                "principal", auth != null ? auth.getPrincipal() : null,
                "authorities", auth != null ? auth.getAuthorities() : null,
                "authorizationHeader", req.getHeader("Authorization")
        );
    }
}
