package io.github.lichao10.stardewfishingautofish.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StardewMixinPlugin implements IMixinConfigPlugin {
    private static final String TARGET = "com.bonker.stardewfishing.client.ClientEvents";
    private static final String OLD_EVENT = "net/neoforged/neoforge/client/event/ContainerScreenEvent$Render$Foreground";
    private static final String EVENT = "net/neoforged/neoforge/client/event/ScreenEvent$Render$Foreground";
    private static final String BRIDGE = "io/github/lichao10/stardewfishingautofish/compat/ContainerTooltipBridge";

    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    public List<String> getMixins() { return null; }
    public void preApply(String targetClassName, ClassNode node, String mixinClassName, IMixinInfo info) {}

    public void postApply(String targetClassName, ClassNode node, String mixinClassName, IMixinInfo info) {
        if (!TARGET.equals(targetClassName)) return;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("onScreenRendered") || !method.desc.equals("(L" + OLD_EVENT + ";)V")) continue;
            method.desc = "(L" + EVENT + ";)V";
            method.signature = null;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "render", method.desc, false));
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = 1;
            method.maxLocals = 1;
            return;
        }
        throw new IllegalStateException("Stardew Fishing container overlay handler was not found");
    }
}
