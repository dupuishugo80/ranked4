package com.ranked4.game.game_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ranked4.game.game_service.model.Gif;
import com.ranked4.game.game_service.repository.GifRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final GifRepository gifRepository;

    public DataInitializer(GifRepository gifRepository) {
        this.gifRepository = gifRepository;
    }

    @Override
    public void run(String... args) {
        initializeGifs();
    }

    private void initializeGifs() {
        long count = gifRepository.count();
        if (count > 0) {
            logger.info("GIFs already initialized. Found {} GIFs.", count);
            return;
        }

        logger.info("Initializing default GIFs...");

        createGif("thumbs_up", "https://media.giphy.com/media/111ebonMs90YLu/giphy.gif");
        createGif("clap", "https://media.giphy.com/media/fnK0jeA8vIh2QLq3IZ/giphy.gif");
        createGif("fire", "https://media.giphy.com/media/l0IyhLWvcoVWzIM5W/giphy.gif");
        createGif("laugh", "https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif");
        createGif("shocked", "https://media.giphy.com/media/3o6Zt6KHxJTbXCnSvu/giphy.gif");
        createGif("thinking", "https://media.giphy.com/media/3o7btPCcdNniyf0ArS/giphy.gif");
        createGif("victory", "https://media.giphy.com/media/g9582DNuQppxC/giphy.gif");
        createGif("facepalm", "https://media.giphy.com/media/XsUtdIeJ0MWMo/giphy.gif");

        logger.info("Default GIFs initialized successfully. Total: {}", gifRepository.count());
    }

    private void createGif(String code, String assetPath) {
        Gif gif = new Gif();
        gif.setCode(code);
        gif.setAssetPath(assetPath);
        gif.setActive(true);
        gifRepository.save(gif);
        logger.debug("Created GIF: {}", code);
    }
}
