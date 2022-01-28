package com.b1n_ry.yigd.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DebugHud.class)
public class DebugHudMixin extends DrawableHelper {
    @Shadow @Final private MinecraftClient client;

    @ModifyArg(method = "getLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1), index = 0)
    private Object renderWorldId(Object worldData) {
        if (client.world == null) return worldData;
        return worldData + "  ID: " + client.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(client.world.getDimension());
    }
}
