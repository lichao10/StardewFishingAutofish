package io.github.lichao10.stardewfishingautofish.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stardew Fishing 3.7 uses a factory backported to newer Forge, absent in legacy NeoForge. */
@Mixin(targets = "com.bonker.stardewfishing.StardewFishing", remap = false)
public abstract class LegacyStardewResourceMixin {
    @Inject(method = "resource", at = @At("HEAD"), cancellable = true, remap = false)
    private static void stardew_fishing_autofish$resource(String path, CallbackInfoReturnable<ResourceLocation> callback) {
        callback.setReturnValue(new ResourceLocation("stardew_fishing", path));
    }
    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;getContainer()Lnet/minecraftforge/fml/javafmlmod/FMLModContainer;"), remap = false)
    private FMLModContainer stardew_fishing_autofish$container(FMLJavaModLoadingContext context) {
        return (FMLModContainer) ModLoadingContext.get().getActiveContainer();
    }
    @Redirect(method = "<init>", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;registerConfig(Lnet/minecraftforge/fml/config/ModConfig$Type;Lnet/minecraftforge/fml/config/IConfigSpec;)V"), remap = false)
    private void stardew_fishing_autofish$config(FMLJavaModLoadingContext context, ModConfig.Type type, IConfigSpec<?> spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }
}
