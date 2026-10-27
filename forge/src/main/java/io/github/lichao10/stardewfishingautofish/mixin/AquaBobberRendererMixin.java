package io.github.lichao10.stardewfishingautofish.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/** Match Oculus 1.8's vanilla fishing-line delimiter for Aquaculture's renderer. */
@Pseudo
@Mixin(targets = "com.teammetallurgy.aquaculture.client.renderer.entity.AquaBobberRenderer", remap = false)
public abstract class AquaBobberRendererMixin {
    @Inject(method = "render(Lcom/teammetallurgy/aquaculture/entity/AquaFishingBobberEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 1, remap = true),
            require = 1, remap = false)
    private void stardew_fishing_autofish$endFishingLine(@Coerce Object bobber, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!ModList.get().isLoaded("oculus")) return;
        // Zero-normal separator is also used by Oculus's MixinFishingHookRenderer.
        // It terminates this line in the shared batch without changing its owner.
        VertexConsumer consumer = buffers.getBuffer(RenderType.lineStrip());
        consumer.vertex(0.0, 0.0, 0.0).color(0, 0, 0, 255)
                .normal(0.0f, 0.0f, 0.0f).endVertex();
    }
}
