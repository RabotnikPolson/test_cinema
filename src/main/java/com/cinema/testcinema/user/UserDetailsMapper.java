package com.cinema.testcinema.user;

import com.cinema.testcinema.model.User;
import com.cinema.testcinema.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserDetailsMapper {

    public UserPrincipal toPrincipal(User user) {
        return UserPrincipal.from(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getRoles().stream().map(role -> role.getRoleName()).collect(Collectors.toList())
        );
    }
}
