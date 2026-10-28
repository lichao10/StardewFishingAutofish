package io.github.lichao10.stardewfishingautofish.smoke;

import io.github.lichao10.stardewfishingautofish.StardewFishingAutofish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod("autofish_smoke")
public final class ClientSmokeMod {
    @Mod.EventBusSubscriber(modid = "autofish_smoke", value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ClientSmokeProbe.tick(ModList.get().isLoaded(StardewFishingAutofish.ID), System.getProperty("autofish.smoke.target", "Forge 1.19.2"));
            }
        }
    }
}
