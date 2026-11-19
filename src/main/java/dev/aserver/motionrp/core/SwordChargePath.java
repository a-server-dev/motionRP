package dev.aserver.motionrp.core;

import java.util.*;

/** The source light blade rotates at the palm; particles use that same axis and current length. */
public final class SwordChargePath {
    public record Point(double right,double forward,double height){}
    private SwordChargePath(){}
    public static double length(int tick){return tick<NobleTimeline.LIGHT_BLADE?1.6:40.1/16*(.1+7.9*Math.clamp((tick-NobleTimeline.LIGHT_BLADE)/16.,0,1));}
    public static List<Point> samples(int tick){
        var hand=NobleHandPath.at(true,tick);double length=length(tick),angle=Math.toRadians(95*Math.clamp((tick-NobleTimeline.CUT_START)/2.5,0,1));
        List<Point> points=new ArrayList<>();
        for(int j=0;j<20;j++){
            double d=(tick*.8+j*length/20)%length,r=.08+.15*Math.sin(j*.7+tick*.2);
            points.add(new Point(hand.right()+r*Math.cos(j+tick*.4),hand.forward()+d*Math.sin(angle),hand.height()+d*Math.cos(angle)));
        }
        return points;
    }
}
