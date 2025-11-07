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
        try {
            String header = req.getHeader("Authorization");
            String token = null;
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }

            if (token != null && jwt.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwt.getUsername(token);
                if (username != null) {
                    UserDetails details = uds.loadUserByUsername(username); // может кинуть UsernameNotFoundException
                    var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("JWT authenticated user={} remoteAddr={}", username, req.getRemoteAddr());
                } else {
                    log.debug("JWT valid but username claim is null");
                }
            }
        } catch (UsernameNotFoundException e) {
            log.warn("User not found for JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in JwtAuthFilter", e);
        }

        chain.doFilter(req, res);
    }
}
