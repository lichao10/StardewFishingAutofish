package io.github.lichao10.stardewfishingautofish;

/** Banks fish progress, visits the stationary chest, and returns to the fish if needed. */
public final class TreasureController {
    private boolean recovering;
    private boolean delayingFinish;

    public boolean choose(double pos, double velocity, int barSize,
            double fish, double fishVelocity, int fishTarget, boolean idle,
            double topSpeed, double upAcceleration, double downAcceleration,
            boolean chestVisible, boolean gotChest, int chestPos, double chestProgress,
            double fishProgress, double lineStrength, boolean treasureBobber) {
        boolean pending = chestVisible && !gotChest;
        if (!pending) {
            recovering = false;
            delayingFinish = false;
            return BarController.choose(pos, velocity, barSize, fish, fishVelocity,
                    fishTarget, idle, topSpeed, upAcceleration, downAcceleration);
        }
        double points = fishProgress * 120;
        // The game increments by 1 and decays by 1/4; undo float getter rounding.
        double chestTicks = Math.round(chestProgress * 120) / 4.0;
        double loss = Math.max(0, 2 - lineStrength);
        double center = pos + (barSize - 14) / 2.0;
        double futureFish = Math.max(0, Math.min(127, fish + fishVelocity * 6));
        double distance = Math.max(0, Math.abs(futureFish - center) - (barSize - 14) / 2.0);
        double returnTime = Math.sqrt(2 * distance / .7) + Math.abs(velocity) / .7 + 8;
        double reserve = Math.min(78, Math.max(18, loss * returnTime + 10));
        if (points <= reserve) recovering = true;
        if (recovering && points >= Math.min(106, Math.max(90, reserve + 32))) recovering = false;
        // A treasure bobber freezes fish-progress loss while touching the chest.
        if (treasureBobber && chestTicks > 0 && points > reserve + 8) recovering = false;
        boolean onFish = fish >= Math.floor(pos) - 2 && fish <= Math.ceil(pos) + barSize - 12;
        if (points >= 100 && 30 - chestTicks >= 118 - points && onFish)
            delayingFinish = true;
        if (points + 30 - chestTicks <= 112 || points <= reserve + 12
                || Math.abs(fish - chestPos) > barSize + 8) delayingFinish = false;
        return BarController.chooseGoal(pos, velocity, barSize, fish, fishVelocity,
                fishTarget, idle, topSpeed, upAcceleration, downAcceleration,
                true, !recovering, delayingFinish, chestPos, chestTicks, points, lineStrength, treasureBobber);
    }
}
