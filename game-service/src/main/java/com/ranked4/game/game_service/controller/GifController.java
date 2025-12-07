package com.ranked4.game.game_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.game.game_service.dto.CreateGifRequestDTO;
import com.ranked4.game.game_service.dto.GifDTO;
import com.ranked4.game.game_service.dto.UpdateGifRequestDTO;
import com.ranked4.game.game_service.model.Gif;
import com.ranked4.game.game_service.service.GifService;

@RestController
@RequestMapping("/api/gifs")
public class GifController {

    private final GifService gifService;

    public GifController(GifService gifService) {
        this.gifService = gifService;
    }

    @GetMapping
    public List<GifDTO> getGifs(@RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        List<Gif> gifs = includeInactive ? gifService.getAllGifs() : gifService.getActiveGifs();
        return gifs.stream()
            .map(gif -> new GifDTO(
                gif.getId(),
                gif.getCode(),
                gif.getAssetPath(),
                gif.isActive()
            ))
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GifDTO> getGifById(@PathVariable Long id) {
        return gifService.getById(id)
            .map(gif -> ResponseEntity.ok(new GifDTO(
                gif.getId(),
                gif.getCode(),
                gif.getAssetPath(),
                gif.isActive()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGif(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestBody CreateGifRequestDTO request) {

        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Only admins can create GIFs");
        }

        try {
            Gif gif = gifService.createGif(
                request.code(),
                request.assetPath(),
                request.active() != null ? request.active() : true
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GifDTO(gif.getId(), gif.getCode(), gif.getAssetPath(), gif.isActive()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating GIF: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGif(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable Long id,
            @RequestBody UpdateGifRequestDTO request) {

        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Only admins can update GIFs");
        }

        try {
            Gif gif = gifService.updateGif(id, request.code(), request.assetPath(), request.active());
            return ResponseEntity.ok(new GifDTO(gif.getId(), gif.getCode(), gif.getAssetPath(), gif.isActive()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating GIF: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGif(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable Long id) {

        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Only admins can delete GIFs");
        }

        try {
            gifService.deleteGif(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting GIF: " + e.getMessage());
        }
    }
}