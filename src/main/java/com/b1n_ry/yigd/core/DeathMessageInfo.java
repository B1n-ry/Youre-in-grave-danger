package com.b1n_ry.yigd.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public class DeathMessageInfo {
    private final String messageString;
    private final Text killedName;
    private final Text killerName;
    private final Text killedByItemName;
    private final Text primeAdversaryName;


    private DeathMessageInfo(String messageString, Text killedName, Text killerName, Text killedByItemName, Text primeAdversaryName) {
        this.messageString = messageString;
        this.killedName = killedName;
        this.killerName = killerName;
        this.killedByItemName = killedByItemName;
        this.primeAdversaryName = primeAdversaryName;
    }

    public String getMessageString() {
        return messageString;
    }

    public Text getKilledName() {
        return killedName;
    }

    public Text getKillerName() {
        return killerName;
    }

    public Text getKilledByItemName() {
        return killedByItemName;
    }

    public Text getPrimeAdversaryName() {
        return primeAdversaryName;
    }

    public static DeathMessageInfo of(DamageSource source, LivingEntity killed) {
        String string = "death.attack." + source.getName();
        Entity sourceEntity = source.getSource();
        Entity attacker = source.getAttacker();
        if (attacker != null || sourceEntity != null) {
            ItemStack itemStack;
            Text text = attacker == null ? sourceEntity.getDisplayName() : attacker.getDisplayName();
            if (attacker instanceof LivingEntity livingEntity) {
                itemStack = livingEntity.getMainHandStack();
            } else {
                itemStack = ItemStack.EMPTY;
            }
            if (!itemStack.isEmpty() && itemStack.hasCustomName()) {
                return new DeathMessageInfo(string, killed.getDisplayName(), text, itemStack.toHoverableText(), null);
            }
            return new DeathMessageInfo(string, killed.getDisplayName(), text, null, null);
        }
        LivingEntity primeAdversary = killed.getPrimeAdversary();
        if (primeAdversary != null) {
            return new DeathMessageInfo(string, killed.getDisplayName(), null, null, primeAdversary.getDisplayName());
        }
        return new DeathMessageInfo(string, killed.getDisplayName(), null, null, null);
    }

    public Text getMessage() {
        if (killerName != null) {
            if (killedByItemName != null) {
                return Text.translatable(messageString, killedName, killerName, killedByItemName);
            }
            return Text.translatable(messageString + ".item", killedName, killerName);
        }
        if (primeAdversaryName != null){
            return Text.translatable(messageString + ".player", killedName, primeAdversaryName);
        }

        return Text.translatable(messageString, killedName);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("message", this.messageString);
        nbt.putString("victim", this.killedName.getString());

        if (this.killerName != null) nbt.putString("killer", this.killerName.getString());
        if (this.killedByItemName != null) nbt.putString("killerItem", this.killedByItemName.getString());
        if (this.primeAdversaryName != null) nbt.putString("primeAdversary", this.primeAdversaryName.getString());

        return nbt;
    }

    public static DeathMessageInfo fromNbt(NbtCompound nbt) {
        String messageString = nbt.getString("message");
        String killedString = nbt.getString("victim");
        String killerString = nbt.contains("killer") ? nbt.getString("killer") : null;
        String killedByItemString = nbt.contains("killerItem") ? nbt.getString("killerItem") : null;
        String primeAdversaryString = nbt.contains("primeAdversary") ? nbt.getString("primeAdversary") : null;

        Text killedText = Text.of(killedString);
        Text killerText = killerString != null ? Text.of(killerString) : null;
        Text killedByItemText = killedByItemString != null ? Text.of(killedByItemString) : null;
        Text primeAdversaryText = primeAdversaryString != null ? Text.of(primeAdversaryString) : null;

        return new DeathMessageInfo(messageString, killedText, killerText, killedByItemText, primeAdversaryText);
    }
}
