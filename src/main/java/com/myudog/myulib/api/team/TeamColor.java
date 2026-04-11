package com.myudog.myulib.api.team;

public enum TeamColor {
    DEFAULT,
    RED,
    BLUE,
    GREEN,
    YELLOW,
    AQUA,
    WHITE,
    BLACK,
    GRAY,
    GOLD,
    LIGHT_PURPLE,
    DARK_PURPLE,
    DARK_RED,
    DARK_GREEN,
    DARK_AQUA,
    DARK_BLUE,
    DARK_GRAY;

    public boolean isRed() {
        return this == RED || this == DARK_RED;
    }
}

