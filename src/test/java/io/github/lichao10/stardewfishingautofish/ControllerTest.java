package io.github.lichao10.stardewfishingautofish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    @Test void movesTowardFishAtOppositeEnds() {
        assertTrue(BarController.choose(0, 0, 36, 120, 0, 120, true, 1, .1, .1));
        assertFalse(BarController.choose(106, 0, 36, 5, 0, 5, true, 1, .1, .1));
    }

    @Test void chargedTreasureReturnsToNormalTracking() {
        TreasureController controller = new TreasureController();
        for (double position : new double[] {0, 40, 106}) {
            boolean expected = BarController.choose(position, 0, 36, 80, 0, 80, true, 1, .1, .1);
            assertEquals(expected, controller.choose(position, 0, 36, 80, 0, 80, true,
                    1, .1, .1, true, true, 5, 1, .5, 1, false));
        }
    }

    @Test void missingTreasureDoesNotChangeNormalTracking() {
        TreasureController controller = new TreasureController();
        for (double fish : new double[] {5, 60, 120}) {
            boolean expected = BarController.choose(40, 0, 36, fish, 0, (int) fish,
                    true, 1, .1, .1);
            assertEquals(expected, controller.choose(40, 0, 36, fish, 0, (int) fish,
                    true, 1, .1, .1, false, false, 5, 0, .5, 1, false));
        }
    }
}
