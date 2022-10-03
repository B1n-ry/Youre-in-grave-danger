package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.JsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @Inject(method = "getItem", at = @At(value = "INVOKE", target = "java/util/Optional.orElseThrow (Ljava/util/function/Supplier;)Ljava/lang/Object;"), cancellable = true)
    private static void throwError(JsonObject json, CallbackInfoReturnable<Item> cir) {
        String string = JsonHelper.getString(json, "item");
        if (string.equals("yigd:death_scroll") && YigdConfig.getConfig().utilitySettings.scrollItem.scrollType == ScrollTypeConfig.DISABLED) cir.setReturnValue(Items.AIR);
    }
}
