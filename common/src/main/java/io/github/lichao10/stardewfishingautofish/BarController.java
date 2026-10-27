package io.github.lichao10.stardewfishingautofish;

import java.util.Arrays;
import java.util.Comparator;

/** Chooses only the next held/released input; never changes minigame state. */
public final class BarController {
    private static final double ACCELERATION = (double) 0.7f;
    private static final int HORIZON = 12;
    private static final int BEAM = 24;
    private record Node(double pos, double velocity, double cost, boolean first,
            double points, double chestTicks) {}

    public static boolean choose(double pos, double velocity, int barSize,
            double fish, double fishVelocity, int fishTarget, boolean idle,
            double topSpeed, double upAcceleration, double downAcceleration) {
        return chooseGoal(pos, velocity, barSize, fish, fishVelocity, fishTarget, idle,
                topSpeed, upAcceleration, downAcceleration, false, false, false, 0, 0, 0, 1, false);
    }

    static boolean chooseGoal(double pos, double velocity, int barSize,
            double fish, double fishVelocity, int fishTarget, boolean idle,
            double topSpeed, double upAcceleration, double downAcceleration,
            boolean treasurePending, boolean seekChest, boolean avoidFish, double chestPos,
            double chestTicks, double points, double lineStrength, boolean treasureBobber) {
        Node[] beam = {new Node(pos, velocity, 0, false, points, chestTicks)};
        int maxHeight = 142 - barSize;
        for (int step = 0; step < HORIZON; step++) {
            // Predict the public game's fish movement until its next random decision.
            if (idle) {
                double oldVelocity = fishVelocity;
                fishVelocity -= (fishVelocity > 0 ? upAcceleration : downAcceleration)
                        * Math.signum(fishVelocity);
                if (oldVelocity * fishVelocity <= 0) fishVelocity = 0;
            } else {
                double delta = fishTarget - fish;
                fishVelocity = clamp(fishVelocity + (delta > 0 ? upAcceleration : downAcceleration)
                        * Math.signum(delta), -topSpeed, topSpeed);
            }
            fish += fishVelocity;
            if (Math.abs(fishTarget - fish) < fishVelocity) idle = true;
            if (fish > 127 || fish < 0) {
                fish = clamp(fish, 0, 127);
                fishVelocity = 0;
                idle = true;
            }
            Node[] next = new Node[beam.length * 2];
            int index = 0;
            for (Node node : beam) {
                for (int input = 0; input < 2; input++) {
                    boolean held = input == 1;
                    double v = node.velocity;
                    if (held) {
                        if (v < 0) v *= 0.9;
                        v += ACCELERATION;
                    } else if (node.pos > 0) v -= ACCELERATION;
                    double p = node.pos + v;
                    if (p > maxHeight) { p = maxHeight; v = 0; }
                    else if (p <= 0) { p = 0; v = v < -(double) 1.4f ? v * -0.4 : 0; }
                    double min = Math.floor(p) - 2;
                    double max = Math.ceil(p) + barSize - 12;
                    boolean onFish = fish >= min && fish <= max;
                    boolean onChest = treasurePending && chestPos >= min && chestPos <= max;
                    double newChest = node.chestTicks;
                    if (newChest < 30) newChest = onChest ? Math.min(30, newChest + 1)
                            : Math.max(0, newChest - 0.25);
                    double newPoints = node.points + (onFish ? 1
                            : treasureBobber && onChest ? 0 : -2 + lineStrength);
                    boolean targetChest = seekChest && newChest < 30;
                    double target = targetChest ? chestPos : fish;
                    double miss = Math.max(0, Math.max(min - target, target - max));
                    double desired = clamp(target - (barSize - 14) / 2.0, 0, maxHeight);
                    if (avoidFish && newChest < 30) {
                        double below = fish - (barSize - 12) - 5;
                        double above = fish + 5;
                        boolean goBelow = below >= 0 && (above > maxHeight
                                || Math.abs(pos - below) < Math.abs(pos - above));
                        desired = clamp(goBelow ? below : above, 0, maxHeight);
                        // Use a convex distance to the escape position: overlap depth has a
                        // local minimum at a boundary and can trap a wide bar on the fish.
                        miss = Math.abs(p - desired);
                    }
                    double error = p - desired;
                    double relativeSpeed = v - (targetChest ? 0 : fishVelocity);
                    double cost = node.cost + Math.pow(0.93, step)
                            * (4 * miss * miss + 0.035 * error * error
                            + 0.12 * relativeSpeed * relativeSpeed);
                    if (treasurePending) {
                        // Fish must survive, and may only finish after the chest is charged.
                        if (newPoints <= 0) cost += 10_000_000;
                        if (newPoints >= 120 && newChest < 30) cost += 10_000_000;
                        if (targetChest) cost -= 12 * (newChest - node.chestTicks);
                        // Finishing must dominate the subsequent change of tracking target;
                        // otherwise receding-horizon planning can hover just short of full.
                        if (seekChest && node.chestTicks < 30 && newChest >= 30) cost -= 1_000_000;
                    }
                    next[index++] = new Node(p, v, cost, step == 0 ? held : node.first,
                            newPoints, newChest);
                }
            }
            Arrays.sort(next, Comparator.comparingDouble(Node::cost));
            beam = Arrays.copyOf(next, Math.min(BEAM, next.length));
        }
        return beam[0].first;
    }

    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }
}
