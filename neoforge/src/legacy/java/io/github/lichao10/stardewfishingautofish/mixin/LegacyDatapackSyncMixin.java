package io.github.lichao10.stardewfishingautofish.mixin;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Resolve the same players using the older datapack-sync event API. */
@Mixin(targets = "com.bonker.stardewfishing.common.CommonEvents$ForgeBus", remap = false)
public abstract class LegacyDatapackSyncMixin {
    @Redirect(method = "onPlayerJoin", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/event/OnDatapackSyncEvent;getPlayers()Ljava/util/List;"), remap = false)
    private static List<ServerPlayer> stardew_fishing_autofish$players(OnDatapackSyncEvent event) {
        return event.getPlayer() == null ? event.getPlayerList().getPlayers() : List.of(event.getPlayer());
    }
}
