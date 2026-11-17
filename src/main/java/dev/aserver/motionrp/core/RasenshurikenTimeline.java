package dev.aserver.motionrp.core;

public final class RasenshurikenTimeline {
    public static final int CLONES = 8, ORB = 20, RAISE = 70, STEP = 86, JUMP = 90,
            THROW = 114, IMPACT = 130, LAND = 150, END = 160;
    public static final double HEIGHT = 5, RANGE = 5;
    public enum Phase { SIGNS, ASSIST, CHARGE, RAISE, STEP, JUMP, THROW, IMPACT, LAND, FINISHED }
    private RasenshurikenTimeline() {}
    public static Phase phase(int tick) {
        return tick < 0 || tick >= END ? Phase.FINISHED : tick < CLONES ? Phase.SIGNS
                : tick < ORB ? Phase.ASSIST : tick < RAISE ? Phase.CHARGE : tick < STEP ? Phase.RAISE
                : tick < JUMP ? Phase.STEP : tick < THROW ? Phase.JUMP : tick < IMPACT ? Phase.THROW
                : tick < LAND ? Phase.IMPACT : Phase.LAND;
    }
    public static double height(int tick) {
        if (tick < JUMP || tick >= LAND) return 0;
        return tick <= THROW ? HEIGHT * Math.sin((tick - JUMP) / (double) (THROW - JUMP) * Math.PI / 2)
                : HEIGHT * Math.cos((tick - THROW) / (double) (LAND - THROW) * Math.PI / 2);
    }
}
