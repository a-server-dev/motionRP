package dev.aserver.motionrp.core;

import java.util.*;
import static dev.aserver.motionrp.core.TreasuryTimeline.*;

/** Bounded branching paths which grow from a palm and collapse into a sword axis. */
public final class EaFormation {
    public record Point(double right,double up,double forward) {
        Point toward(Point b,double t) { return new Point(right+(b.right-right)*t,up+(b.up-up)*t,forward+(b.forward-forward)*t); }
    }
    private record Segment(Point from,Point to,int depth) {}
    private static final List<Segment> TREE=create();
    private EaFormation() {}
    private static List<Segment> create() {
        var result=new ArrayList<Segment>();
        for(int i=0;i<10;i++) {
            double angle=i*Math.PI*(3-Math.sqrt(5));
            branch(result,new Point(0,.04,0),angle,.8+(i%4)*.22,0,i);
        }
        return List.copyOf(result);
    }
    private static void branch(List<Segment> result,Point start,double angle,double rise,int depth,int seed) {
        double length=depth==0?1.15:depth==1?.85:.65;
        Point end=new Point(start.right+Math.cos(angle)*length,start.up+rise,start.forward+Math.sin(angle)*length);
        result.add(new Segment(start,end,depth));
        if(depth<2) for(int side:new int[]{-1,1}) branch(result,end,angle+side*(.65+(seed%3)*.12),rise*.55,depth+1,seed+side+3);
    }
    public static double growth(int tick) { return Math.clamp((tick-BRANCH)/(double)(CONVERGE-BRANCH),0,1); }
    public static double convergence(int tick) {
        double t=Math.clamp((tick-CONVERGE)/(double)(EA_READY-CONVERGE),0,1);return t*t*(3-2*t);
    }
    public static List<Point> points(int tick) {
        if(tick<BRANCH || tick>EA_READY) return List.of();
        double growth=growth(tick),collapse=convergence(tick);var points=new ArrayList<Point>();
        for(var segment:TREE) {
            double visible=Math.clamp(growth*3-segment.depth,0,1);if(visible==0) continue;
            Point end=segment.from.toward(segment.to,visible);
            double distance=Math.sqrt(Math.pow(end.right-segment.from.right,2)+Math.pow(end.up-segment.from.up,2)+Math.pow(end.forward-segment.from.forward,2));
            int samples=Math.max(1,(int)Math.ceil(distance/.22));
            for(int j=0;j<=samples;j++) {
                Point p=segment.from.toward(end,j/(double)samples);
                // Compress the entire branching structure onto the blade, never a sphere.
                points.add(p.toward(new Point(0,.12+Math.min(1.5,p.up*.48),0),collapse));
            }
        }
        return List.copyOf(points);
    }
    /** Each key band stays hidden until it has emerged above the horizontal gate. */
    public static boolean keyBandVisible(int band,int tick) {
        double rise=Math.clamp((tick-KEY)/(double)(KEY_READY-KEY),0,1);
        return (-5+(band+.5)*2.5)/16.0*.6 >= 1.3*(1-rise)-.06;
    }
}
