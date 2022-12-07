package com.b1n_ry.yigd.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(DebugHud.class)
public class DebugHudMixin extends DrawableHelper {
    @Shadow @Final private MinecraftClient client;

    @ModifyArg(method = "getLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1), index = 0)
    private Object renderWorldId(Object worldData) {
        if (client.world == null) return worldData;
        return worldData + "  ID: " + client.world.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getRawId(client.world.getDimension());
    }
}