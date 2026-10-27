package io.github.lichao10.stardewfishingautofish.mixin;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Reads the client-synchronized bite flag. */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("biting")
    boolean stardew_fishing_autofish$isBiting();
}
