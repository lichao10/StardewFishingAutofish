package com.bonker.stardewfishing.client;
import net.minecraft.sounds.SoundEvent;
public class FishingScreen {
    public boolean ended, won, chest, golden;
    public double accuracy;
    public void stopReelingSounds() {}
    public void playSound(SoundEvent event) {}
    public void setResult(boolean won, double accuracy, boolean chest, boolean golden) {
        this.ended = true; this.won = won; this.accuracy = accuracy;
        this.chest = chest; this.golden = golden;
    }
}
