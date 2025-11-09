package com.ranked4.matchmaking.matchmaking_service.controller;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.matchmaking.matchmaking_service.service.PrivateMatchService;

@RestController
@RequestMapping("/api/private-matches")
public class PrivateMatchController {

    private static final Logger log = LoggerFactory.getLogger(PrivateMatchController.class);
    private final PrivateMatchService privateMatchService;

    public PrivateMatchController(PrivateMatchService privateMatchService) {
        this.privateMatchService = privateMatchService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId = parseUserId(userIdHeader);
        Map<String, Object> lobby = privateMatchService.createPrivateLobby(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "code", lobby.get("code"),
                "expiresInSeconds", 1800
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestHeader("X-User-Id") String userIdHeader,
                                  @RequestBody Map<String, String> body) {
        UUID userId = parseUserId(userIdHeader);
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing code");
        }
        try {
            Map<String, Object> lobby = privateMatchService.joinPrivateLobby(userId, code.trim().toUpperCase());
            return ResponseEntity.ok(Map.of(
                    "code", lobby.get("code"),
                    "hostUserId", lobby.get("hostUserId"),
                    "guestUserId", lobby.get("guestUserId")
            ));
        } catch (IllegalStateException e) {
            log.warn("Join private lobby failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestHeader("X-User-Id") String userIdHeader,
                                   @RequestBody Map<String, String> body) {
        UUID userId = parseUserId(userIdHeader);
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing code");
        }
        try {
            UUID matchId = privateMatchService.startPrivateMatch(userId, code.trim().toUpperCase());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("matchId", matchId));
        } catch (IllegalStateException e) {
            log.warn("Start private match failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getLobby(@PathVariable String code,
                                        @RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid X-User-Id");
        }

        Map<String, Object> lobby = privateMatchService.getLobby(code);
        if (lobby == null) {
            return ResponseEntity.notFound().build();
        }

        String hostUserId = (String) lobby.get("hostUserId");
        String guestUserId = (String) lobby.get("guestUserId");

        if (!userId.toString().equals(hostUserId) && (guestUserId == null || !userId.toString().equals(guestUserId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("hostUserId", hostUserId);
        response.put("guestUserId", guestUserId);

        return ResponseEntity.ok(response);
    }

    private UUID parseUserId(String header) {
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid X-User-Id");
        }
    }
}
