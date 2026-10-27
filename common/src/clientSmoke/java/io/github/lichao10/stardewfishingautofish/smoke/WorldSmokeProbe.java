package io.github.lichao10.stardewfishingautofish.smoke;

import com.bonker.stardewfishing.client.FishingScreen;
import com.mojang.logging.LogUtils;
import io.github.lichao10.stardewfishingautofish.ClientAutoFish;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Full local single-player flow, driven through game APIs instead of OS input. */
public final class WorldSmokeProbe {
    private static int stage, ticks, games;
    private static long started;
    private static String target;
    private static boolean originalPause, castSeen, minigameSeen;
    private static FishingScreen previousGame;
    private static Object menuHook;
    private static int loggedStage;

    public static boolean active() { return stage != 0; }
    public static void begin(Minecraft mc, String name) throws Exception {
        target = name; stage = 1; started = System.currentTimeMillis();
        originalPause = mc.options.pauseOnLostFocus;
        mc.options.pauseOnLostFocus = false;
        var settings = new LevelSettings("Autofish isolated smoke test", GameType.CREATIVE,
                false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        var options = new WorldOptions(20261027L, false, false);
        Function<RegistryAccess, WorldDimensions> dimensions = WorldPresets::createNormalWorldDimensions;
        var flows = mc.createWorldOpenFlows();
        var create = Arrays.stream(flows.getClass().getMethods())
                .filter(m -> m.getName().equals("createFreshLevel")).findFirst().orElseThrow();
        String folder = "autofish-smoke-" + started;
        if (create.getParameterCount() == 4) create.invoke(flows, folder, settings, options, dimensions);
        else create.invoke(flows, folder, settings, options, dimensions, new TitleScreen());
        LogUtils.getLogger().info("WORLD SMOKE started {} in {}", target, folder);
    }
    private static Field field(String name) throws Exception {
        Field f = ClientAutoFish.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static boolean enabled() throws Exception { return field("enabled").getBoolean(null); }
    private static void toggle(Minecraft mc) throws Exception {
        var method = ClientAutoFish.class.getDeclaredMethod("toggle", Minecraft.class);
        method.setAccessible(true); method.invoke(null, mc);
    }
    private static int rewards(Minecraft mc) {
        int total = 0;
        for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
            var item = mc.player.getInventory().getItem(slot);
            if (!item.isEmpty() && !item.is(Items.FISHING_ROD)) total += item.getCount();
        }
        return total;
    }
    private static void commands(Minecraft mc, String... commands) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var source = server.createCommandSourceStack().withSuppressedOutput();
            for (String command : commands) server.getCommands().performPrefixedCommand(source, command);
        });
    }
    public static void tick(Minecraft mc) {
        try {
            if (stage != loggedStage) {
                LogUtils.getLogger().info("WORLD SMOKE stage {}; games {}; rewards {}; screen {}", stage,
                        games, mc.player == null ? 0 : rewards(mc), mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
                loggedStage = stage;
            }
            if (System.currentTimeMillis() - started > 240_000) throw new AssertionError("World flow timeout at stage " + stage);
            if (mc.level == null || mc.player == null || mc.screen != null && stage != 3 && stage != 4) return;
            ticks++;
            if (stage == 1 && ticks > 20) {
                commands(mc, "fill -9 195 -3 9 195 19 minecraft:stone",
                        "fill -8 196 -2 8 199 18 minecraft:water", "setblock 0 199 -4 minecraft:stone",
                        "tp @a 0.5 200 -3.5 0 25", "clear @a", "give @a minecraft:fishing_rod",
                        "enchant @a minecraft:lure 3", "enchant @a minecraft:luck_of_the_sea 3",
                        "time set day", "weather clear");
                stage = 2; ticks = 0;
            } else if (stage == 2 && ticks > 80) {
                mc.options.pauseOnLostFocus = originalPause;
                toggle(mc);
                if (!enabled() || mc.options.pauseOnLostFocus) throw new AssertionError("Automation did not enable background operation");
                stage = 3; ticks = 0;
                LogUtils.getLogger().info("WORLD SMOKE automation enabled");
            } else if (stage == 3) {
                if (mc.player.fishing != null) castSeen = true;
                if (mc.screen instanceof FishingScreen screen) {
                    if (screen != previousGame) { previousGame = screen; games++; }
                    var status = FishingScreen.class.getDeclaredField("status"); status.setAccessible(true);
                    if (status.get(screen).toString().equals("MINIGAME")) minigameSeen = true;
                }
                if (games >= 2 && rewards(mc) >= 2 && !(mc.screen instanceof FishingScreen)) {
                    menuHook = mc.player.fishing;
                    mc.setScreen(new InventoryScreen(mc.player)); stage = 4; ticks = 0;
                }
            } else if (stage == 4 && ticks > 25) {
                if (mc.player.fishing != menuHook) throw new AssertionError("Rod action continued while inventory was open");
                mc.setScreen(null); toggle(mc);
                if (enabled() || mc.options.pauseOnLostFocus != originalPause) throw new AssertionError("Toggle did not restore pause setting");
                // Keep the unattended test running between disable/re-enable assertions.
                mc.options.pauseOnLostFocus = false;
                var server = mc.getSingleplayerServer(); var id = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    var rod = player.getMainHandItem(); rod.setDamageValue(rod.getMaxDamage() - 1);
                    player.containerMenu.broadcastChanges();
                });
                stage = 5; ticks = 0;
            } else if (stage == 5 && ticks > 40) {
                toggle(mc); stage = 6; ticks = 0;
            } else if (stage == 6 && ticks > 50) {
                if (enabled()) throw new AssertionError("Low durability did not stop automation");
                if (!castSeen || !minigameSeen || games < 2) throw new AssertionError("Incomplete real fishing cycle");
                String result = "PASS " + target + ": real single-player auto cast/bite/minigame/rewards; games=" + games
                        + "; rewards=" + rewards(mc) + "; inventory pause; background setting restored; low durability stop.\n";
                Files.writeString(Path.of("world-smoke-result.txt"), result);
                LogUtils.getLogger().info(result.strip());
                finish(mc);
            }
        } catch (Throwable failure) {
            LogUtils.getLogger().error("WORLD SMOKE FAILED at stage " + stage, failure);
            try { Files.writeString(Path.of("world-smoke-result.txt"), "FAIL " + failure + "\n"); } catch (Exception ignored) { }
            finish(mc);
        }
    }
    private static void finish(Minecraft mc) {
        stage = 0;
        try {
            if (enabled()) toggle(mc);
            mc.options.pauseOnLostFocus = originalPause;
            if (mc.level != null) mc.level.disconnect();
            java.lang.reflect.Method disconnect;
            try { disconnect = mc.getClass().getMethod("clearLevel", Screen.class); }
            catch (NoSuchMethodException missing) { disconnect = mc.getClass().getMethod("disconnect", Screen.class); }
            disconnect.invoke(mc, new TitleScreen());
        } catch (Exception error) { LogUtils.getLogger().warn("Smoke shutdown", error); }
        mc.stop();
    }
}
