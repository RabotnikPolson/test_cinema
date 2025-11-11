package com.cinema.testcinema.security;

import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.user.UserDetailsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final UserDetailsMapper userDetailsMapper;

    public CustomUserDetailsService(UserRepository userRepository, UserDetailsMapper userDetailsMapper) {
        this.userRepository = userRepository;
        this.userDetailsMapper = userDetailsMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (!user.isEnabled()) {
                        log.debug("Attempt to authenticate disabled user: {}", email);
                        throw new UsernameNotFoundException("User is disabled");
                    }
                    return userDetailsMapper.toPrincipal(user);
                })
                .orElseThrow(() -> new UsernameNotFoundException("User with email %s not found".formatted(email)));
    }
}
