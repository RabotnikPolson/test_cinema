package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.WatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class WatchControllerSecurityTest {

    private final WatchService watchService = Mockito.mock(WatchService.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AuthenticatedUserService authenticatedUserService = new AuthenticatedUserService();
    private final WatchController controller = new WatchController(watchService, userRepository, authenticatedUserService);

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void beatUsesAuthenticatedUserId() {
        User user = new User();
        user.setId(1L);
        user.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        WatchBeatDto dto = new WatchBeatDto(42L, UUID.randomUUID(), 5, Instant.now(), false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        controller.beat(dto, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(watchService).beat(userCaptor.capture(), eq(dto));
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
    }
}
