package com.ranked4.userprofile.userprofile_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public class UserProfileDTO {

    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private int elo;
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private Instant createdAt;
    private Instant updatedAt;

    public UserProfileDTO(UserProfile entity) {
        this.userId = entity.getUserId();
        this.displayName = entity.getDisplayName();
        this.avatarUrl = entity.getAvatarUrl();
        this.elo = entity.getElo();
        this.gamesPlayed = entity.getGamesPlayed();
        this.wins = entity.getWins();
        this.losses = entity.getLosses();
        this.draws = entity.getDraws();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }

    public static UserProfileDTO fromEntity(UserProfile entity) {
        if (entity == null) {
            return null;
        }
        return new UserProfileDTO(entity);
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}