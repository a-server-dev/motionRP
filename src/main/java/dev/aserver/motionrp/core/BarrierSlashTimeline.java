package dev.aserver.motionrp.core;

public final class BarrierSlashTimeline {
    public static final int RHO_END=180,MASK_START=12,MASK_COMPLETE=26,GETSUGA_PREFIX=32,GETSUGA_CHARGE=60,GETSUGA_RELEASE=88,GETSUGA_IMPACT=128,GETSUGA_FADE=144,GETSUGA_END=164;
    private BarrierSlashTimeline() {}
    public static int end(boolean shield){return shield?RHO_END:GETSUGA_END;}
    public static double layerScale(int layer){if(layer<0||layer>=7)throw new IllegalArgumentException("layer");return (1.89-layer*.14)*(layer==0?1.5:2);}
    public static double layerForward(int layer){layerScale(layer);return 2.1+layer*.8;}
    public static int layerStart(int layer){if(layer<0||layer>=7)throw new IllegalArgumentException("layer");return 16+layer*6;}
    public static double distance(int tick){return 32*Math.clamp((tick-GETSUGA_RELEASE)/(double)(GETSUGA_IMPACT-GETSUGA_RELEASE),0,1);}
}
