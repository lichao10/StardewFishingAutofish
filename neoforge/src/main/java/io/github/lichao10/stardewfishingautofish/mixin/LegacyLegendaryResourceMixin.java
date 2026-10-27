package io.github.lichao10.stardewfishingautofish.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Resource location construction for NeoForge 1.20.1. */
@Mixin(targets = "com.bonker.stardewfishing.server.data.LegendaryFishModifier", remap = false)
public abstract class LegacyLegendaryResourceMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), remap = false)
    private static ResourceLocation stardew_fishing_autofish$resource(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
