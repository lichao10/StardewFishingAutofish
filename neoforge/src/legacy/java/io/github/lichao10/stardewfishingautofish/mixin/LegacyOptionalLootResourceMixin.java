package io.github.lichao10.stardewfishingautofish.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use the equivalent legacy parser for the upstream optional-item loot codec. */
@Mixin(targets = "com.bonker.stardewfishing.server.data.OptionalLootItem$Serializer", remap = false)
public abstract class LegacyOptionalLootResourceMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/resources/ResourceLocation;parse(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), remap = false)
    private ResourceLocation stardew_fishing_autofish$parse(String location) {
        return new ResourceLocation(location);
    }
}
