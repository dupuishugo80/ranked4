package com.ranked4.matchmaking.matchmaking_service.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.matchmaking.matchmaking_service.service.MatchmakingService;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingController.class);
    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/join")
    public ResponseEntity<Void> joinQueue(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            log.warn("Attempting to access /join with an invalid X-User-Id: {}", userIdHeader);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        log.info("Request /join received for user {}", userId);
        matchmakingService.joinQueue(userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveQueue(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            log.warn("Attempting to access /leave with an invalid X-User-Id: {}", userIdHeader);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        log.info("Request /leave received for user {}", userId);
        matchmakingService.leaveQueue(userId);

        return ResponseEntity.ok().build();
    }
}
