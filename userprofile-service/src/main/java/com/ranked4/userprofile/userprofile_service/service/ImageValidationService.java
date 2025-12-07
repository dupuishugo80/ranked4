package com.ranked4.userprofile.userprofile_service.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class ImageValidationService {

    private static final long MIN_IMAGE_SIZE = 1024; // 1 KB
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MIN_DIMENSION = 50; // pixels
    private static final int MAX_DIMENSION = 4096; // pixels
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public ImageValidationService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB buffer
                .build();
    }

    public ValidationResult validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return new ValidationResult(false, "Image URL is required");
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return new ValidationResult(false, "Image URL must start with http:// or https://");
        }

        try {
            byte[] imageBytes = downloadImage(imageUrl);

            ValidationResult sizeValidation = validateSize(imageBytes);
            if (!sizeValidation.isValid()) {
                return sizeValidation;
            }

            ValidationResult dimensionsValidation = validateDimensions(imageBytes);
            if (!dimensionsValidation.isValid()) {
                return dimensionsValidation;
            }

            return new ValidationResult(true, "Image is valid");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return new ValidationResult(false,
                        "Image URL is not accessible (error " + e.getStatusCode().value() + ")");
            } else if (e.getStatusCode().is5xxServerError()) {
                return new ValidationResult(false, "Image server is unavailable");
            }
            return new ValidationResult(false, "Failed to download image: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, "Invalid URL: " + e.getMessage());
        } catch (Exception e) {
            return new ValidationResult(false, "Failed to validate image: " + e.getMessage());
        }
    }

    private byte[] downloadImage(String imageUrl) {
        return webClient.get()
                .uri(imageUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(TIMEOUT)
                .doOnNext(bytes -> {
                    String contentType = "unknown";
                })
                .block();
    }

    private ValidationResult validateSize(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new ValidationResult(false, "Image is empty");
        }

        long size = imageBytes.length;

        if (size < MIN_IMAGE_SIZE) {
            return new ValidationResult(false,
                    String.format("Image is too small (%d bytes). Minimum size: %d bytes",
                            size, MIN_IMAGE_SIZE));
        }

        if (size > MAX_IMAGE_SIZE) {
            return new ValidationResult(false,
                    String.format("Image is too large (%d bytes). Maximum size: %d bytes",
                            size, MAX_IMAGE_SIZE));
        }

        return new ValidationResult(true, "Image size is valid");
    }

    private ValidationResult validateDimensions(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (image == null) {
                return new ValidationResult(false, "The file is not a valid image or the format is not supported");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                return new ValidationResult(false,
                        String.format("Image dimensions are too small (%dx%d). Minimum dimensions: %dx%d",
                                width, height, MIN_DIMENSION, MIN_DIMENSION));
            }

            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                return new ValidationResult(false,
                        String.format("Image dimensions are too large (%dx%d). Maximum dimensions: %dx%d",
                                width, height, MAX_DIMENSION, MAX_DIMENSION));
            }

            return new ValidationResult(true, "Image dimensions are valid");

        } catch (IOException e) {
            return new ValidationResult(false, "Failed to read image: " + e.getMessage());
        }
    }

    public record ValidationResult(boolean isValid, String message) {
    }
}
