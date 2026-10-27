package io.github.lichao10.stardewfishingautofish.smoke;

import io.github.lichao10.stardewfishingautofish.StardewFishingAutofish;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod("autofish_smoke")
public final class ClientSmokeMod {
    @EventBusSubscriber(modid = "autofish_smoke", value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            ClientSmokeProbe.tick(ModList.get().isLoaded(StardewFishingAutofish.ID), "NeoForge 1.21.1");
        }
    }
}
