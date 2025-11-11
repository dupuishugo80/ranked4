package com.ranked4.game.game_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ranked4.game.game_service.model.Gif;

public interface GifRepository extends JpaRepository<Gif, Long> {
      Optional<Gif> findByCodeAndActiveTrue(String code);
      List<Gif> findAllByActiveTrue();
}