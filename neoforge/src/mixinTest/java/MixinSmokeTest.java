import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Applies the accessor to the runtime fishing hook class. */
public class MixinSmokeTest {
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("stardew_fishing_autofish.mixins.json");
        HeadlessMixinService service = (HeadlessMixinService) MixinService.getService();
        String target = "net.minecraft.world.entity.projectile.FishingHook";
        ClassNode node = service.getClassNode(target);
        var transformer = service.transformer();
        if (!transformer.transformClass(MixinEnvironment.getDefaultEnvironment(),target,node))
            throw new AssertionError("No accessor applied");
        if (!node.interfaces.contains("io/github/lichao10/stardewfishingautofish/mixin/FishingHookAccessor"))
            throw new AssertionError("Accessor interface missing");
        int reads = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals("stardew_fishing_autofish$isBiting") && method.desc.equals("()Z")) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETFIELD
                            && field.name.equals("biting") && field.desc.equals("Z")) reads++;
                }
            }
        }
        if (reads != 1) throw new AssertionError("Expected exactly one read of synchronized biting; got " + reads);
        String upstreamTarget = "com.bonker.stardewfishing.client.ClientEvents";
        ClassNode upstream = service.getClassNode(upstreamTarget);
        if (!transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), upstreamTarget, upstream))
            throw new AssertionError("Container event bridge was not applied");
        MethodNode bridge = upstream.methods.stream().filter(m -> m.name.equals("onScreenRendered")).findFirst().orElseThrow();
        if (!bridge.desc.equals("(Lnet/neoforged/neoforge/client/event/ScreenEvent$Render$Foreground;)V"))
            throw new AssertionError("Container event descriptor was not updated");
        boolean delegate = false;
        for (AbstractInsnNode instruction : bridge.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("io/github/lichao10/stardewfishingautofish/compat/ContainerTooltipBridge")) delegate = true;
        }
        if (!delegate) throw new AssertionError("Container tooltip delegate is missing");
        System.out.println("PASS: Stardew Fishing container overlay uses the current NeoForge event.");
        System.out.println("PASS: Minecraft " + System.getProperty("autofish.test.minecraft") + " FishingHook accessor Mixin applied; reads biting exactly once.");
    }
}
