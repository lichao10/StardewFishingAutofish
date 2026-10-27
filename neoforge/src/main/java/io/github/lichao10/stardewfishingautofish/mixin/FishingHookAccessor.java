package io.github.lichao10.stardewfishingautofish.mixin;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Reads the client-synchronized vanilla bite flag in the 1.21.1 named runtime. */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("biting")
    boolean stardewfishingautofish$isBiting();
}
