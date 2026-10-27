package com.bonker.stardewfishing.common.networking;
import net.minecraft.world.item.ItemStack;
/** Test-only data record: avoids registry/network registration in a headless JVM. */
public record S2CStartMinigamePacket(int idleTime, float topSpeed, float upAcceleration,
        float downAcceleration, int avgDistance, int moveVariation, ItemStack fish,
        boolean treasureChest, boolean goldenChest, float lineStrength, int barSize) {}
