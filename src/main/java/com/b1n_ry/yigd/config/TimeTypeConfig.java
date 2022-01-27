package com.b1n_ry.yigd.config;

public enum TimeTypeConfig {
    SECONDS,
    MINUTES,
    HOURS;
    public int tickFactor() {
        switch (this) {
            case SECONDS -> {
                return 20;
            }
            case MINUTES -> {
                return 20 * 60;
            }
            case HOURS -> {
                return 20 * 60 * 60;
            }
            default -> {
                return 1;
            }
        }
    }
}
