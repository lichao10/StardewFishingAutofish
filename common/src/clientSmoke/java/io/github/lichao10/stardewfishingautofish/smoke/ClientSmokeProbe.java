package io.github.lichao10.stardewfishingautofish.smoke;

import com.mojang.logging.LogUtils;
import io.github.lichao10.stardewfishingautofish.StardewFishingAutofish;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

/** Client startup checks. */
public final class ClientSmokeProbe {
    private static int titleTicks;
    private static boolean finished;
    private static String observedScreen = "";
    public static void tick(boolean registered, String target) {
        Minecraft mc = Minecraft.getInstance();
        if (WorldSmokeProbe.active()) { WorldSmokeProbe.tick(mc); return; }
        String screen = mc.screen == null ? "null" : mc.screen.getClass().getName();
        if (!screen.equals(observedScreen)) {
            LogUtils.getLogger().info("CLIENT SMOKE screen: {}", screen);
            observedScreen = screen;
        }
        // The first-run accessibility tutorial blocks the menu in a fresh test profile.
        if (screen.endsWith("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
        if (finished || !(mc.screen instanceof TitleScreen) || ++titleTicks < 20) return;
        finished = true;
        try {
            if (!registered || !StardewFishingAutofish.ID.equals("stardew_fishing_autofish")) {
                throw new AssertionError("Runtime mod registration/identity mismatch");
            }
            var key = Arrays.stream(mc.options.keyMappings)
                    .filter(k -> k.getName().equals("key.stardew_fishing_autofish.toggle"))
                    .findFirst().orElseThrow(() -> new AssertionError("Toggle key not registered"));
            if (!key.getKey().getName().equals("key.keyboard.f8")) throw new AssertionError("Unexpected default key");
            var resource = mc.getResourceManager().getResource(
                    Identifier.tryParse("stardew_fishing_autofish:icon.png"))
                    .orElseThrow(() -> new AssertionError("Icon not available in active game resource manager"));
            try (var input = resource.open()) {
                byte[] png = input.readAllBytes();
                if (png.length < 1000 || png[0] != (byte) 137 || png[1] != 80) throw new AssertionError("Invalid icon PNG");
            }
            String result = "PASS " + target + ": real client reached TitleScreen; mod registered; F8 registered; icon loaded.\n";
            Files.writeString(Path.of("client-smoke-result.txt"), result);
            LogUtils.getLogger().info(result.strip());
            if (Boolean.getBoolean("autofish.smoke.world")) WorldSmokeProbe.begin(mc, target);
        } catch (Throwable failure) {
            LogUtils.getLogger().error("CLIENT SMOKE FAILED: " + target, failure);
            try { Files.writeString(Path.of("client-smoke-result.txt"), "FAIL " + failure + "\n"); }
            catch (Exception ignored) { }
        } finally {
            if (!WorldSmokeProbe.active()) mc.stop();
        }
    }
}
