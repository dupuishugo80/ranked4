package com.ranked4.game.game_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranked4.game.game_service.dto.GifDTO;
import com.ranked4.game.game_service.service.GifService;

@RestController
@RequestMapping("/api/gifs")
public class GifController {

    private final GifService gifService;

    public GifController(GifService gifService) {
        this.gifService = gifService;
    }

    @GetMapping
    public List<GifDTO> getGifs() {
        return gifService.getActiveGifs().stream()
            .map(gif -> new GifDTO(
                gif.getId(),
                gif.getCode(),
                gif.getAssetPath()
            ))
            .toList();
    }
}