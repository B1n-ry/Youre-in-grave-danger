package com.b1n4ry.yigd.mixin;

import com.mojang.datafixers.DataFixUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;

@Mixin(DebugHud.class)
@Environment(EnvType.CLIENT)
public class DebugHudMixin extends DrawableHelper {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "getLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean renderWorldId(List list, Object e) {
        World world = this.getWorld();
        LongSet longSet = world instanceof ServerWorld ? ((ServerWorld)world).getForcedChunks() : LongSets.EMPTY_SET;

        if (e instanceof String) {
            list.add(this.client.world.getRegistryKey().getValue() + " FC: " + ((LongSet)longSet).size() + "  ID: " + world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(world.getDimension()));
        }
        return false;
    }

    // Copy of the DebugHud class method
    private World getWorld() {
        return (World) DataFixUtils.orElse(Optional.ofNullable(this.client.getServer()).flatMap((integratedServer) -> {
            return Optional.ofNullable(integratedServer.getWorld(this.client.world.getRegistryKey()));
        }), this.client.world);
    }
}
