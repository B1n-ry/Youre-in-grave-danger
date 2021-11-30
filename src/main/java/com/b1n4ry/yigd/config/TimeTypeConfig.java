package com.b1n4ry.yigd.config;

public enum TimeTypeConfig {
    SECONDS,
    MINUTES,
    HOURS;
    public int tickFactor() {
        switch (this) {
            case SECONDS -> {
                return 1000;
            }
            case MINUTES -> {
                return 1000 * 60;
            }
            case HOURS -> {
                return 1000 * 60 * 60;
            }
            default -> {
                return 1;
            }
        }
    }
}
