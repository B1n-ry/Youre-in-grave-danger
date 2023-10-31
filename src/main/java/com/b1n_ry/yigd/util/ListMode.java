package com.b1n_ry.yigd.util;

import net.minecraft.util.StringIdentifiable;

public enum ListMode implements StringIdentifiable {
    WHITELIST,
    BLACKLIST;

    public static final com.mojang.serialization.Codec<ListMode> CODEC = StringIdentifiable.createCodec(ListMode::values);

    @Override
    public String asString() {
        return this.name();
    }
}
