package dev.aserver.motionrp.core;

public final class TitanTimeline {
    public static final int BITE=8, FLASH=30, TRANSFORM=42, GROWN=60, SPRINT=120, TACKLE=140, DRIVE=140, EXIT=160, DIVE=170, LAND=208, STEAM=218, END=244;
    public static final double SCALE=10, NAPE_HEIGHT=14.4, SPRINT_DISTANCE=50, LANDING_FORWARD=45;
    private TitanTimeline() {}
    public static double scale(int tick,double original) {
        if(tick<TRANSFORM || tick>=END) return original;
        double p=Math.clamp((tick-TRANSFORM)/(double)(GROWN-TRANSFORM),0,1);
        p=p*p*(3-2*p);return original+(SCALE-original)*p;
    }
    public record ExitPose(double forward,double height) {}
    public static double sprintDistance(int tick){return SPRINT_DISTANCE*Math.clamp((tick-SPRINT)/(double)(TACKLE-SPRINT),0,1);}
    public static double tackleDistance(int tick){double p=Math.clamp((tick-DRIVE)/32.,0,1);return sprintDistance(tick)+14*p*p*(3-2*p);}
    public static ExitPose exitPose(int tick,double ground){
        var nape=TitanTacklePath.at(EXIT);double start=tackleDistance(EXIT)+nape.forward();
        if(tick<DIVE){double p=Math.clamp((tick-EXIT)/(double)(DIVE-EXIT),0,1);return new ExitPose(start-3*p,nape.height()+.7*Math.sin(p*Math.PI));}
        double p=Math.clamp((tick-DIVE)/(double)(LAND-DIVE),0,1);return new ExitPose((start-3)*(1-p)+LANDING_FORWARD*p,nape.height()+(ground-nape.height())*p*p);
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<FLASH ? "BITE" : tick<TRANSFORM ? "FLASH"
                : tick<GROWN ? "TRANSFORM" : tick<SPRINT ? "GIANT" : tick<TACKLE ? "SPRINT" : tick<EXIT ? "BODY_CHECK" : tick<DIVE ? "EXIT"
                : tick<LAND ? "DIVE" : tick<STEAM ? "LAND" : "STEAM";
    }
}
