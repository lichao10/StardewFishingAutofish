package io.github.lichao10.stardewfishingautofish.compat;

import com.bonker.stardewfishing.SFConfig;
import com.bonker.stardewfishing.client.RodTooltipHandler;
import com.bonker.stardewfishing.proxy.ClientProxy;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ContainerTooltipBridge {
    private ContainerTooltipBridge() {}

    public static void render(ScreenEvent.Render.Foreground event) {
        if (!SFConfig.isInventoryEquippingEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        var graphics = event.getGuiGraphics();
        graphics.pose().pushMatrix();
        graphics.pose().translate(screen.getLeftPos(), screen.getTopPos());
        try {
            RodTooltipHandler.render(graphics, ClientProxy.getPartialTick(),
                    event.getMouseX() - screen.getLeftPos(), event.getMouseY() - screen.getTopPos());
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
