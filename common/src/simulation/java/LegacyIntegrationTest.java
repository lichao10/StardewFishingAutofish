import com.bonker.stardewfishing.client.FishingMinigame;
import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.common.FishBehavior;
import io.github.lichao10.stardewfishingautofish.BarController;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public class LegacyIntegrationTest {
    private static Field field(String name) throws Exception {
        Field result = FishingMinigame.class.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
    public static void main(String[] args) throws Exception {
        var profiles = Files.readAllLines(Path.of(args[0]));
        Field velocity = field("bobberVelocity"), fishVelocity = field("fishVelocity"), target = field("fishTarget"), idle = field("fishIsIdle"), random = field("random");
        List<String> results = new ArrayList<>();
        results.add("case,profile,won,ticks,accuracy");
        int caught = 0, timeouts = 0;
        for (int i = 0; i < 1000; i++) {
            String[] row = profiles.get(i % profiles.size()).split(",");
            FishBehavior behavior = new FishBehavior(Integer.parseInt(row[1]), Float.parseFloat(row[2]), Float.parseFloat(row[3]), Float.parseFloat(row[4]), Integer.parseInt(row[5]), Integer.parseInt(row[6]));
            FishingScreen screen = new FishingScreen();
            FishingMinigame game = new FishingMinigame(screen, behavior);
            ((Random) random.get(game)).setSeed(20261027L + i);
            int ticks = 0;
            while (!screen.ended && ticks++ < 4000) {
                boolean input = BarController.choose(game.getBobberPos(), velocity.getDouble(game), 36,
                        game.getFishPos(), fishVelocity.getDouble(game), target.getInt(game), idle.getBoolean(game),
                        behavior.topSpeed(), behavior.upAcceleration(), behavior.downAcceleration());
                game.tick(input);
            }
            if (!screen.ended) timeouts++;
            if (screen.won) caught++;
            results.add(i + "," + row[0] + "," + screen.won + "," + ticks + "," + screen.accuracy);
        }
        Files.write(Path.of(args[1]), results);
        System.out.println("1000 SF 1.3 games: fish=" + caught + "/1000; timeouts=" + timeouts);
        if (caught < 980 || timeouts != 0) throw new AssertionError("Fishing regression: " + caught + " catches, " + timeouts + " timeouts");
    }
}
