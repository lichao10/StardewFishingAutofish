package com.bonker.stardewfishing.common.init;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
/** Test-only: no real audio registration. */
public final class SFSoundEvents {
    public static final DeferredHolder<SoundEvent, SoundEvent> DWOP = new DeferredHolder<>();
    public static final DeferredHolder<SoundEvent, SoundEvent> DWOP_REVERSE = new DeferredHolder<>();
}
