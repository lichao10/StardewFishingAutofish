import com.bonker.stardewfishing.client.*;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import io.github.lichao10.stardewfishingautofish.BarController;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import sun.misc.Unsafe;

/** Runs the installed mod's unmodified FishingMinigame.class, with audio/UI/network-registration test doubles. */
public class ControllerIntegrationTest {
    static final Map<String, Field> FIELDS = new HashMap<>();
    static void set(Object game, String name, Object value) throws Exception { f(name).set(game, value); }
    static Field f(String name) throws Exception {
        if (!FIELDS.containsKey(name)) {
            Field field = FishingMinigame.class.getDeclaredField(name);
            field.setAccessible(true); FIELDS.put(name, field);
        }
        return FIELDS.get(name);
    }
    public static void main(String[] args) throws Exception {
        Field uf = Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true);
        Unsafe unsafe = (Unsafe) uf.get(null);
        int wins = 0, count = 0, timeouts = 0;
        double accuracy = 0;
        long start = System.nanoTime();
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            if (line.isBlank()) continue;
            String[] p = line.split(",");
            String name = p[0];
            int idle = Integer.parseInt(p[1]);
            float speed = Float.parseFloat(p[2]), up = Float.parseFloat(p[3]), down = Float.parseFloat(p[4]);
            int distance = Integer.parseInt(p[5]), variation = Integer.parseInt(p[6]);
            int localWins = 0, localCount = 0;
            for (int size : new int[]{24, 32, 48}) {
                for (int seed = 0; seed < 5; seed++) {
                    FishingScreen screen = new FishingScreen();
                    FishingMinigame game = (FishingMinigame) unsafe.allocateInstance(FishingMinigame.class);
                    set(game, "random", new Random(seed)); set(game, "screen", screen);
                    set(game, "packet", new S2CStartMinigamePacket(idle, speed, up, down,
                            distance, variation, null, false, false, 0, size));
                    set(game, "lineStrength", 0f); set(game, "barSize", size);
                    set(game, "maxBobberHeight", 142-size); set(game, "fishTarget", -1);
                    set(game, "points", 24f); set(game, "chestAppearTime", -1);
                    for (int tick = 0; tick < 1200 && !screen.ended; tick++) {
                        boolean input = BarController.choose(game.getBobberPos(), f("bobberVelocity").getDouble(game),
                            size, game.getFishPos(), f("fishVelocity").getDouble(game),
                            f("fishTarget").getInt(game), f("fishIsIdle").getBoolean(game), speed, up, down);
                        game.tick(input);
                        if (game.getBobberPos() < 0 || game.getBobberPos() > 142-size)
                            throw new AssertionError("bar out of bounds");
                    }
                    count++; localCount++;
                    if (screen.won) { wins++; localWins++; accuracy += screen.accuracy; }
                    if (!screen.ended) timeouts++;
                }
            }
            if (localWins != localCount) System.out.println(name + ": " + localWins + "/" + localCount);
        }
        System.out.printf(Locale.ROOT, "Actual Stardew Fishing 3.7 class: %d/%d wins, %d timeouts; mean winning accuracy %.2f%%; %.2fs%n",
                wins, count, timeouts, accuracy / Math.max(1,wins)*100, (System.nanoTime()-start)/1e9);
        if (count == 0 || wins < count * .95 || timeouts != 0) throw new AssertionError("controller regression");
    }
}
