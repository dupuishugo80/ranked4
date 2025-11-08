package com.ranked4.userprofile.userprofile_service.dto;

import com.ranked4.userprofile.userprofile_service.model.UserProfile;

public class LeaderboardEntryDTO {

    private String displayName;
    private String avatarUrl;
    private int elo;
    private int wins;
    private int losses;
    private int draws;
    private int rank;

    public LeaderboardEntryDTO(UserProfile entity) {
        this.displayName = entity.getDisplayName();
        this.avatarUrl = entity.getAvatarUrl();
        this.elo = entity.getElo();
        this.wins = entity.getWins();
        this.losses = entity.getLosses();
        this.draws = entity.getDraws();
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

}