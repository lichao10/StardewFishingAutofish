package io.github.lichao10.stardewfishingautofish;

import com.bonker.stardewfishing.client.FishingMinigame;
import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.common.FishBehavior;
import com.bonker.stardewfishing.common.FishingHookLogic;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client fishing automation. */
@Mod.EventBusSubscriber(modid = StardewFishingAutofish.ID, value = Dist.CLIENT)
public final class ClientAutoFish {
    private static KeyMapping toggle;
    private static boolean enabled;
    private static Boolean savedPauseOnLostFocus;
    private static int cooldown;
    private static FishingScreen controlledScreen;
    private static FishingHook observedHook;
    private static boolean reeledThisBite;
    private static Field minigameField, statusField, velocityField, fishVelocityField,
            targetField, idleField, behaviorField, inputField;

    @Mod.EventBusSubscriber(modid = StardewFishingAutofish.ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
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
        if (getRodHand(mc.player) == null) {
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
            message(mc, "message.stardew_fishing_autofish.on", toggle.getTranslatedKeyMessage());
        } catch (ReflectiveOperationException | RuntimeException error) {
            fail(mc, error);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !enabled) return;
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
                controlledScreen = screen;
                cooldown = 20;
                if (statusField.get(screen) == FishingScreen.Status.MINIGAME) {
                    FishingMinigame game = (FishingMinigame) minigameField.get(screen);
                    FishBehavior behavior = (FishBehavior) behaviorField.get(game);
                    inputField.setBoolean(screen, BarController.choose(game.getBobberPos(),
                            velocityField.getDouble(game), 36, game.getFishPos(),
                            fishVelocityField.getDouble(game), targetField.getInt(game),
                            idleField.getBoolean(game), behavior.topSpeed(),
                            behavior.upAcceleration(), behavior.downAcceleration()));
                } else inputField.setBoolean(screen, false);
                return;
            }
            releaseInput();
            // Pause while another screen is open.
            if (mc.screen != null || mc.isPaused()) return;
            if (cooldown > 0) { cooldown--; return; }
            InteractionHand hand = getRodHand(mc.player);
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

    private static InteractionHand getRodHand(net.minecraft.world.entity.player.Player player) {
        return FishingHookLogic.getRodHand(player);
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
        behaviorField = field(FishingMinigame.class, "behavior");
        inputField = field(FishingScreen.class, "mouseDown");
        minigameField = game;
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void releaseInput() {
        if (controlledScreen != null) {
            try { inputField.setBoolean(controlledScreen, false); }
            catch (IllegalAccessException error) { LogUtils.getLogger().error("Could not release fishing input", error); }
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

    private static void overlay(PoseStack graphics) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        mc.font.drawShadow(graphics, Component.translatable("hud.stardew_fishing_autofish.on",
                toggle.getTranslatedKeyMessage()), 6, 6, 0x66FF99);
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) { overlay(event.getPoseStack()); }

    @SubscribeEvent
    public static void fishingHud(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof FishingScreen) overlay(event.getPoseStack());
    }
}
