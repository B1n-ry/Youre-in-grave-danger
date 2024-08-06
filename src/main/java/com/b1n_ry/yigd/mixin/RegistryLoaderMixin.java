package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.google.gson.JsonElement;
import com.mojang.serialization.Decoder;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin is present so that the configs can still toggle if the enchantments are loaded (without a bunch of fuzz)
 */
@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
    @Inject(method = "parseAndAdd", at = @At("HEAD"), cancellable = true)
    private static <E> void maybeCancelResourceLoad(MutableRegistry<E> registry, Decoder<E> decoder, RegistryOps<JsonElement> ops, RegistryKey<E> key, Resource resource, RegistryEntryInfo entryInfo, CallbackInfo ci) {
        YigdConfig.ExtraFeatures extraFeaturesConfig = YigdConfig.getConfig().extraFeatures;
        if (key.equals(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Yigd.MOD_ID, "soulbound"))) && !extraFeaturesConfig.enableSoulbound) {
            ci.cancel();
        }
        if (key.equals(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Yigd.MOD_ID, "death_sight"))) && !extraFeaturesConfig.deathSightEnchant.enabled) {
            ci.cancel();
        }
    }
}
