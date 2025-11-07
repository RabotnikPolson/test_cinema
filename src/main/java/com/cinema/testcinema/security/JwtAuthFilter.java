package com.cinema.testcinema.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwt;
    private final UserDetailsService uds;

    public JwtAuthFilter(JwtService jwt, UserDetailsService uds) {
        this.jwt = jwt;
        this.uds = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        String token = null;

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        log.debug("[JwtAuthFilter] {} {} tokenPresent={} authNull={}",
                req.getMethod(), req.getRequestURI(), token != null,
                SecurityContextHolder.getContext().getAuthentication() == null);

        try {
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean valid = jwt.isValid(token);
                log.debug("[JwtAuthFilter] token valid={}", valid);

                if (valid) {
                    String username = jwt.getUsername(token);
                    log.debug("[JwtAuthFilter] extracted username='{}'", username);

                    if (username != null) {
                        try {
                            UserDetails details = uds.loadUserByUsername(username);
                            log.debug("[JwtAuthFilter] loaded user={} authorities={}", username, details.getAuthorities());
                            var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.info("[JwtAuthFilter] authenticated user={} from {}", username, req.getRemoteAddr());
                        } catch (UsernameNotFoundException ex) {
                            log.warn("[JwtAuthFilter] user not found for username={}", username);
                        }
                    } else {
                        log.warn("[JwtAuthFilter] username null inside valid token");
                    }
                } else {
                    log.warn("[JwtAuthFilter] invalid token for {}", req.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.error("[JwtAuthFilter] unexpected error", e);
        }

        chain.doFilter(req, res);
    }
}
