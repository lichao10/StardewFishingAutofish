package net.minecraft.world.entity.projectile;
import io.github.lichao10.stardewfishingautofish.mixin.FishingHookAccessor;
/** Headless fixture, never included in the release. */
public class FishingHook implements FishingHookAccessor {
    private boolean biting;
    public void setVanilla(boolean value) { biting = value; }
    public boolean stardewfishingautofish$isBiting() { return biting; }
}
