package io.github.lichao10.stardewfishingautofish;

import com.bonker.stardewfishing.client.FishingMinigame;
import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import com.bonker.stardewfishing.proxy.ItemUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/** Client fishing automation. */
@EventBusSubscriber(modid = StardewFishingAutofish.ID, value = Dist.CLIENT)
public final class ClientAutoFish {
    private static KeyMapping toggle;
    private static boolean enabled;
    private static Boolean savedPauseOnLostFocus;
    private static int cooldown;
    private static FishingScreen controlledScreen;
    private static FishingHook observedHook;
    private static boolean reeledThisBite;
    private static TreasureController treasureController = new TreasureController();
    private static Field minigameField, statusField, velocityField, fishVelocityField,
            targetField, idleField, packetField, treasureBobberField;

    @EventBusSubscriber(modid = StardewFishingAutofish.ID, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            toggle = new KeyMapping("key.stardew_fishing_autofish.toggle", KeyConflictContext.UNIVERSAL,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.stardew_fishing_autofish");
            event.register(toggle);
        }
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && event.getAction() == GLFW.GLFW_PRESS
                && toggle != null && toggle.matches(event.getKey(), event.getScanCode())) {
            toggle(mc);
        }
    }

    @SubscribeEvent
    public static void screenKey(ScreenEvent.KeyPressed.Pre event) {
        // Screen keys can be consumed before the global InputEvent.Key event.
        if (event.getScreen() instanceof FishingScreen && toggle != null
                && toggle.matches(event.getKeyCode(), event.getScanCode())) {
            toggle(Minecraft.getInstance());
            event.setCanceled(true);
        }
    }

    private static void toggle(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        if (enabled) { stop(mc, "message.stardew_fishing_autofish.off"); return; }
        if (ItemUtils.getRodHand(mc.player) == null) {
            message(mc, "message.stardew_fishing_autofish.need_rod");
            return;
        }
        try {
            initializeFields();
            savedPauseOnLostFocus = mc.options.pauseOnLostFocus;
            mc.options.pauseOnLostFocus = false;
            enabled = true;
            cooldown = 10;
            observedHook = null;
            reeledThisBite = false;
            treasureController = new TreasureController();
            message(mc, "message.stardew_fishing_autofish.on", toggle.getTranslatedKeyMessage());
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail(mc, error);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            stop(mc, null);
            return;
        }
        if (!mc.player.isAlive() || mc.player.isSpectator()) {
            stop(mc, "message.stardew_fishing_autofish.off");
            return;
        }
        try {
            if (mc.screen instanceof FishingScreen screen) {
                if (controlledScreen != screen) treasureController = new TreasureController();
                controlledScreen = screen;
                cooldown = 20;
                if (statusField.get(screen) == FishingScreen.Status.MINIGAME) {
                    FishingMinigame game = (FishingMinigame) minigameField.get(screen);
                    S2CStartMinigamePacket packet = (S2CStartMinigamePacket) packetField.get(game);
                    screen.setInputDown(treasureController.choose(game.getBobberPos(),
                            velocityField.getDouble(game), game.getBarSize(), game.getFishPos(),
                            fishVelocityField.getDouble(game), targetField.getInt(game),
                            idleField.getBoolean(game), packet.topSpeed(),
                            packet.upAcceleration(), packet.downAcceleration(),
                            game.isChestVisible(), game.gotChest(), game.getChestPos(),
                            game.getChestProgress(), game.getProgress(), packet.lineStrength(),
                            treasureBobberField.getBoolean(game)));
                } else screen.setInputDown(false);
                return;
            }
            releaseInput();
            // Pause while another screen is open.
            if (mc.screen != null || mc.isPaused()) return;
            if (cooldown > 0) { cooldown--; return; }
            InteractionHand hand = ItemUtils.getRodHand(mc.player);
            if (hand == null) { stop(mc, "message.stardew_fishing_autofish.need_rod"); return; }
            ItemStack rod = mc.player.getItemInHand(hand);
            if (rod.isDamageableItem() && rod.getMaxDamage() - rod.getDamageValue() <= 1) {
                stop(mc, "message.stardew_fishing_autofish.low_durability");
                return;
            }
            FishingHook hook = mc.player.fishing;
            // Ignore stale references to another player's bobber.
            if (hook != null && hook.getPlayerOwner() != mc.player) return;
            if (hook != observedHook) {
                observedHook = hook;
                reeledThisBite = false;
            }
            if (hook == null) {
                useRod(mc, hand);
                cooldown = 30; // allow the cast packet and bobber entity to arrive
            } else if (!BiteDetector.isBiting(hook)) {
                reeledThisBite = false;
            } else if (!reeledThisBite) {
                reeledThisBite = true;
                useRod(mc, hand);
                cooldown = 10;
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail(mc, error);
        }
    }

    private static void useRod(Minecraft mc, InteractionHand hand) {
        // Use the rod through the vanilla interaction API.
        mc.gameMode.useItem(mc.player, hand);
        mc.player.swing(hand);
    }

    private static void initializeFields() throws ReflectiveOperationException {
        if (minigameField != null) return;
        Field game = field(FishingScreen.class, "minigame");
        statusField = field(FishingScreen.class, "status");
        velocityField = field(FishingMinigame.class, "bobberVelocity");
        fishVelocityField = field(FishingMinigame.class, "fishVelocity");
        targetField = field(FishingMinigame.class, "fishTarget");
        idleField = field(FishingMinigame.class, "fishIsIdle");
        packetField = field(FishingMinigame.class, "packet");
        treasureBobberField = field(FishingMinigame.class, "hasTreasureBobber");
        minigameField = game;
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void releaseInput() {
        if (controlledScreen != null) {
            controlledScreen.setInputDown(false);
            controlledScreen = null;
        }
    }

    private static void stop(Minecraft mc, String reason) {
        enabled = false;
        if (savedPauseOnLostFocus != null) {
            mc.options.pauseOnLostFocus = savedPauseOnLostFocus;
            savedPauseOnLostFocus = null;
        }
        releaseInput();
        cooldown = 0;
        observedHook = null;
        reeledThisBite = false;
        if (reason != null) message(mc, reason);
    }

    private static void fail(Minecraft mc, Exception error) {
        stop(mc, "message.stardew_fishing_autofish.error");
        LogUtils.getLogger().error("Stardew Fishing Autofish disabled after compatibility error", error);
    }

    private static void message(Minecraft mc, String key, Object... args) {
        if (mc.player != null) mc.player.displayClientMessage(Component.translatable(key, args), true);
    }

    private static void overlay(GuiGraphics graphics) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(mc.font, Component.translatable("hud.stardew_fishing_autofish.on",
                toggle.getTranslatedKeyMessage()), 6, 6, 0x66FF99);
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) { overlay(event.getGuiGraphics()); }

    @SubscribeEvent
    public static void fishingHud(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof FishingScreen) overlay(event.getGuiGraphics());
    }
}
