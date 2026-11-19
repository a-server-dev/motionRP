package dev.aserver.motionrp.core;

public final class BarrierSlashTimeline {
    public static final int RHO_END=180,GETSUGA_RELEASE=56,GETSUGA_IMPACT=96,GETSUGA_END=132;
    private BarrierSlashTimeline() {}
    public static int end(boolean shield){return shield?RHO_END:GETSUGA_END;}
    public static double layerScale(int layer){if(layer<0||layer>=7)throw new IllegalArgumentException("layer");return 1.89-layer*.14;}
    public static int layerStart(int layer){if(layer<0||layer>=7)throw new IllegalArgumentException("layer");return 16+layer*6;}
    public static double distance(int tick){return 32*Math.clamp((tick-GETSUGA_RELEASE)/(double)(GETSUGA_IMPACT-GETSUGA_RELEASE),0,1);}
}
