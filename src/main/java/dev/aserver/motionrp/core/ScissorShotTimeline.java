package dev.aserver.motionrp.core;
/** Shared scoop, opposing-foot brush and rightward landing turn for the two aerial shots. */
public final class ScissorShotTimeline {
 private ScissorShotTimeline(){}
 public static int at(boolean blizzard,int clock){return blizzard?clock:(int)Math.round(clock*132.0/211);}
 public static double clock(boolean blizzard,int tick){return blizzard?tick:tick*211.0/132;}
 public static double height(boolean blizzard,int tick,double jump){double c=clock(blizzard,tick),r=BlizzardTimeline.reference(c);return .85*BlizzardTimeline.smooth(c,2,12)*(1-BlizzardTimeline.smooth(c,24,39))+jump*BlizzardTimeline.smooth(r,154,192)*(1-BlizzardTimeline.smooth(r,243,271));}
 public static String phase(boolean blizzard,int tick){double c=clock(blizzard,tick);return c<8?"CHARGE":c<18?"FOOT_BRUSH":c<39?"RIGHT_TURN":c<69?"FROZEN_CROUCH":c<127?"DOUBLE_TURN":c<173?"WINDUP":c<203?"SHOT":"RECOVER";}
}
