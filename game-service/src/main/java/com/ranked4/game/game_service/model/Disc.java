package com.ranked4.game.game_service.model;

public enum Disc {
    EMPTY(0),
    PLAYER_ONE(1),
    PLAYER_TWO(2);

    private final int value;

    Disc(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Disc fromInt(int i) {
        for (Disc d : values()) {
            if (d.getValue() == i) {
                return d;
            }
        }
        return EMPTY;
    }
}
