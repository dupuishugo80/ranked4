package com.ranked4.game.game_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranked4.game.game_service.model.Gif;
import com.ranked4.game.game_service.repository.GifRepository;

@Service
public class GifService {

    private final GifRepository gifRepository;

    public GifService(GifRepository gifRepository) {
        this.gifRepository = gifRepository;
    }

    public List<Gif> getActiveGifs() {
        return gifRepository.findAllByActiveTrue();
    }

    public Optional<Gif> getByCode(String code) {
        return gifRepository.findByCodeAndActiveTrue(code);
    }
}