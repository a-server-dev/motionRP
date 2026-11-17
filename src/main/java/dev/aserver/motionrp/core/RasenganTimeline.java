package dev.aserver.motionrp.core;

/** One clock for body animation, clone lifetime and the hand-held effect. */
public final class RasenganTimeline {
    public static final int CLONE_START = 8;
    public static final int ORB_START = 20;
    public static final int CLONE_END = 60;
    public static final int THRUST_START = 72;
    public static final int BURST = 82;
    public static final int ORB_END = 94;
    public static final int END = 96;
    public static final double DASH_DISTANCE = 4.0;
    public enum Phase { SIGNS, ASSIST, CHARGE, WINDUP, THRUST, DISSIPATE, FINISHED }
    public record Frame(Phase phase, boolean cloneVisible, boolean orbVisible, double scale, double forward) {}

    private RasenganTimeline() {}

    /** Cumulative horizontal travel; zero before the thrust, no post-cast drift. */
    public static double dashDistance(int tick) {
        return DASH_DISTANCE * smooth((tick - THRUST_START + 1) / (double) (BURST - THRUST_START));
    }

    public static double orbHeight(int tick) {
        return RasenganHandPath.at(tick).height();
    }

    public static Frame at(int tick) {
        if (tick < 0 || tick >= END) return new Frame(Phase.FINISHED, false, false, 0, .64);
        Phase phase = tick < CLONE_START ? Phase.SIGNS : tick < ORB_START ? Phase.ASSIST
                : tick < CLONE_END ? Phase.CHARGE : tick < THRUST_START ? Phase.WINDUP
                : tick < BURST ? Phase.THRUST : Phase.DISSIPATE;
        double scale = .12 + .88 * smooth((tick - ORB_START) / 32.0);
        if (tick >= BURST) scale = Math.max(.02, 1 - smooth((tick - BURST) / 12.0));
        double forward = RasenganHandPath.at(tick).forward();
        return new Frame(phase, tick >= CLONE_START && tick < CLONE_END,
                tick >= ORB_START && tick < ORB_END, scale, forward);
    }

    private static double smooth(double value) {
        double x = Math.clamp(value, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
