package com.scouter.netherdepthsupgrade.entity.entities;
import net.minecraft.world.entity.projectile.FishingHook;
/** Models the separate DATA_BITING -> bitingFish contract observed in NDU 3.3. */
public class LavaFishingBobberEntity extends FishingHook {
    private boolean bitingFish;
    public void setLava(boolean value) { bitingFish = value; }
}
