package com.cinema.testcinema.security;

import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // одна роль вместо множества
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                u.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(u.getRole()))
        );
    }
}
