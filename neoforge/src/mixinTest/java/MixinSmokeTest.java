import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;

/** Runs the actual NeoForge Mixin implementation on the actual 1.21.1 hook class. */
public class MixinSmokeTest {
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("stardewfishingautofish.mixins.json");
        HeadlessMixinService service = (HeadlessMixinService) MixinService.getService();
        String target = "net.minecraft.world.entity.projectile.FishingHook";
        ClassNode node = service.getClassNode(target);
        if (!service.transformer().transformClass(MixinEnvironment.getDefaultEnvironment(),target,node))
            throw new AssertionError("No accessor applied");
        if (!node.interfaces.contains("io/github/lichao10/stardewfishingautofish/mixin/FishingHookAccessor"))
            throw new AssertionError("Accessor interface missing");
        int reads = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals("stardewfishingautofish$isBiting") && method.desc.equals("()Z")) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETFIELD
                            && field.name.equals("biting") && field.desc.equals("Z")) reads++;
                }
            }
        }
        if (reads != 1) throw new AssertionError("Expected exactly one read of synchronized biting; got " + reads);
        System.out.println("PASS: actual Minecraft 1.21.1 FishingHook accessor Mixin applied; reads biting exactly once.");
    }
}
