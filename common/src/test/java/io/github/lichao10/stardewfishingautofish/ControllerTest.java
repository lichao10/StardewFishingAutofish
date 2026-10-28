package io.github.lichao10.stardewfishingautofish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {
    @Test void movesTowardFishAtOppositeEnds() {
        assertTrue(BarController.choose(0, 0, 36, 120, 0, 120, true, 1, .1, .1));
        assertFalse(BarController.choose(106, 0, 36, 5, 0, 5, true, 1, .1, .1));
    }

}
