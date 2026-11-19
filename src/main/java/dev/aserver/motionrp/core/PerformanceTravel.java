package dev.aserver.motionrp.core;

/** Distances and jump height in blocks; the model supplies limbs, not duplicate travel. */
public final class PerformanceTravel {
    private PerformanceTravel() {}
    public static int end(Performance p){return p==Performance.RONALDO?46:p==Performance.KNEESLIDE?50:0;}
    public static double distance(Performance p,int tick){
        double t=Math.max(0,tick);
        if(p==Performance.RONALDO)return t<=24?4*t/24:4+2*Math.min(1,(t-24)/22);
        if(p==Performance.KNEESLIDE){if(t<=20)return 3*t/20;double f=Math.min(1,(t-20)/30);return 3+4*(2*f-f*f);}
        return 0;
    }
    public static double height(Performance p,int tick){return p==Performance.RONALDO && tick>24 && tick<46?1.2*Math.sin(Math.PI*(tick-24)/22):0;}
    public static double yaw(Performance p,int tick){return p==Performance.RONALDO?180*Math.clamp((tick-24)/22.0,0,1):0;}
}
