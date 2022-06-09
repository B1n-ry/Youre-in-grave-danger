package com.b1n_ry.yigd.core;

import net.minecraft.util.Identifier;

public class PacketIdentifiers {
    public static final Identifier CONFIG_UPDATE = new Identifier("yigd", "config_update");
    public static final Identifier RESTORE_INVENTORY = new Identifier("yigd", "restore_inventory");
    public static final Identifier DELETE_GRAVE = new Identifier("yigd", "delete_grave");
    public static final Identifier ROB_GRAVE = new Identifier("yigd", "rob_grave");
    public static final Identifier GIVE_KEY_ITEM = new Identifier("yigd", "give_key_item");
    public static final Identifier SET_GRAVE_LOCK = new Identifier("yigd", "set_grave_lock");

    public static final Identifier SINGLE_GRAVE_GUI = new Identifier("yigd", "single_grave");
    public static final Identifier PLAYER_GRAVES_GUI = new Identifier("yigd", "single_dead_guy");
    public static final Identifier ALL_PLAYER_GRAVES = new Identifier("yigd", "all_dead_people");
    public static final Identifier KEY_AND_UNLOCK_CONFIG = new Identifier("yigd", "key_and_unlockable");
}
