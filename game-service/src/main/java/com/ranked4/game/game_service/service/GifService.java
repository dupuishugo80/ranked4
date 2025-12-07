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

    public List<Gif> getAllGifs() {
        return gifRepository.findAll();
    }

    public Optional<Gif> getById(Long id) {
        return gifRepository.findById(id);
    }

    public Optional<Gif> getByCode(String code) {
        return gifRepository.findByCodeAndActiveTrue(code);
    }

    public Gif createGif(String code, String assetPath, boolean active) {
        Gif gif = new Gif();
        gif.setCode(code);
        gif.setAssetPath(assetPath);
        gif.setActive(active);
        return gifRepository.save(gif);
    }

    public Gif updateGif(Long id, String code, String assetPath, Boolean active) {
        Gif gif = gifRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("GIF not found with id: " + id));

        if (code != null && !code.isBlank()) {
            gif.setCode(code);
        }
        if (assetPath != null && !assetPath.isBlank()) {
            gif.setAssetPath(assetPath);
        }
        if (active != null) {
            gif.setActive(active);
        }

        return gifRepository.save(gif);
    }

    public void deleteGif(Long id) {
        if (!gifRepository.existsById(id)) {
            throw new IllegalArgumentException("GIF not found with id: " + id);
        }
        gifRepository.deleteById(id);
    }
}