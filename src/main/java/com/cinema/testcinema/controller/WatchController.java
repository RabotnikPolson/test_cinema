package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.service.WatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class WatchController {
    private final WatchService watchService;

    public WatchController(WatchService watchService) {
        this.watchService = watchService;
    }

    // TODO: заменить заглушку userId на извлечение из security (JWT)
    private Long currentUserId(Principal p){
        // p.getName() -> username -> найти id в UserRepository
        // временно:
        return 1L;
    }

    @PostMapping("/watch-history/beat")
    public ResponseEntity<Void> beat(@RequestBody WatchBeatDto dto, Principal principal) {
        // получить User из репозитория по principal
        User u = new User(); u.setId(currentUserId(principal));
        watchService.beat(u, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics/me/summary")
    public ResponseEntity<AnalyticsSummaryDto> mySummary(Principal principal) {
        Long uid = currentUserId(principal);
        return ResponseEntity.ok(watchService.mySummary(uid));
    }
}
