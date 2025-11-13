package com.ranked4.userprofile.userprofile_service.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_userprofile_userid", columnList = "userId", unique = true)
})

public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    private int elo = 1200;

    @Column(nullable = false)
    private int gamesPlayed = 0;

    @Column(nullable = false)
    private int wins = 0;

    @Column(nullable = false)
    private int losses = 0;

    @Column(nullable = false)
    private int draws = 0;

    @Column(nullable = false)
    private int gold = 0;

    @ManyToOne
    @JoinColumn(name = "equipped_disc_id", nullable = true)
    private DiscCustomization equippedDisc;

    @ManyToMany
    @JoinTable(
        name = "user_owned_discs",
        joinColumns = @JoinColumn(name = "user_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "disc_customization_id")
    )
    private Set<DiscCustomization> ownedDiscs = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public UserProfile(Long id, UUID userId, String displayName, String avatarUrl, int elo, int gamesPlayed, int wins,
            int losses, int draws, int gold, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.elo = elo;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.gold = gold;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UserProfile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public DiscCustomization getEquippedDisc() {
        return equippedDisc;
    }

    public void setEquippedDisc(DiscCustomization equippedDisc) {
        this.equippedDisc = equippedDisc;
    }

    public Set<DiscCustomization> getOwnedDiscs() {
        return ownedDiscs;
    }

    public void setOwnedDiscs(Set<DiscCustomization> ownedDiscs) {
        this.ownedDiscs = ownedDiscs;
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