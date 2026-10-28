package com.bonker.stardewfishing.client;
import net.minecraft.sounds.SoundEvent;
public class FishingScreen {
    public boolean ended, won;
    public double accuracy;
    public int reelSoundTimer;
    public void stopReelingSounds() {}
    public void playSound(SoundEvent event) {}
    public void setResult(boolean won, double accuracy) {
        this.ended = true; this.won = won; this.accuracy = accuracy;
    }
}
