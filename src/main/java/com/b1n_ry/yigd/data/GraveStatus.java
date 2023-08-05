package com.b1n_ry.yigd.data;

public enum GraveStatus {
    UNCLAIMED,
    CLAIMED,
    DESTROYED;

    public int getTransparentColor() {
        return switch (this) {
            case CLAIMED -> 0x2600FF00;
            case DESTROYED -> 0x26FF0000;
            case UNCLAIMED -> 0x26FFFF00;
        };
    }
}
