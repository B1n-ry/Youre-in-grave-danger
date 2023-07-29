package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.util.Identifier;

public interface PacketIdentifiers {
    Identifier GRAVE_OVERVIEW_S2C = idFor("grave_overview_s2c");
    Identifier GRAVE_SELECTION_S2C = idFor("grave_selection_s2c");
    Identifier PLAYER_SELECTION_S2C = idFor("player_selection_s2c");
    Identifier GRAVE_LOCKING_C2S = idFor("grave_locking_c2s");
    Identifier GRAVE_RESTORE_C2S = idFor("grave_restore_c2s");
    Identifier GRAVE_ROBBING_C2S = idFor("grave_robbing_c2s");
    Identifier GRAVE_DELETE_C2S = idFor("grave_delete_c2s");

    static Identifier idFor(String path) {
        return new Identifier(Yigd.MOD_ID, path);
    }
}
