import io.github.lichao10.stardewfishingautofish.BiteDetector;
import net.minecraft.world.entity.projectile.FishingHook;
import com.scouter.netherdepthsupgrade.entity.entities.LavaFishingBobberEntity;
public class BiteRegressionTest {
    static class AquaHook extends FishingHook {}
    static class LavaSubclass extends LavaFishingBobberEntity {}
    static int checks;
    static void check(FishingHook hook, boolean expected) throws Exception {
        if (BiteDetector.isBiting(hook) != expected) throw new AssertionError(hook.getClass());
        checks++;
    }
    public static void main(String[] args) throws Exception {
        for (FishingHook hook : new FishingHook[] {new FishingHook(), new AquaHook()}) {
            check(hook,false);hook.setVanilla(true);check(hook,true);
            hook.setVanilla(false);check(hook,false);
        }
        for (LavaFishingBobberEntity hook : new LavaFishingBobberEntity[] {
                new LavaFishingBobberEntity(),new LavaSubclass()}) {
            check(hook,false);hook.setLava(true);check(hook,true);
            hook.setLava(false);check(hook,false);hook.setVanilla(true);check(hook,false);
            hook.setLava(true);check(hook,true);
        }
        System.out.println("PASS: " + checks + " bite checks (vanilla, Aquaculture fixture, lava, subclass, reset).");
    }
}
