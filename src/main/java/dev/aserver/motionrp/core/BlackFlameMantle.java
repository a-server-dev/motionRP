package dev.aserver.motionrp.core;
import java.util.*;
/** Uneven rising black fire, with a tall crown and torn side plumes; never feather ribs. */
public final class BlackFlameMantle {
 public record Point(double right,double forward,double height){}
 private BlackFlameMantle(){}
 public static List<Point> samples(int tick){
  var points=new ArrayList<Point>(246);
  for(int lobe=0;lobe<3;lobe++)for(int tongue=0;tongue<7;tongue++)for(int j=0;j<10;j++){
   double p=j/9.,seed=lobe*13+tongue*2.39996;
   double flicker=Math.sin(tick*.19+seed),length=.88+.19*Math.sin(seed*1.7)+.07*flicker;
   double center=lobe==0?0:lobe==1?-.65:.65;
   double drift=lobe==0?.45*Math.sin(seed):lobe==1?-3.0:3.65;
   double x=center+drift*p*length+(tongue-3)*.15*Math.sin(Math.PI*p)+.22*Math.sin(p*7+seed+tick*.08)*p;
   double y=(lobe==0?2.05:1.45)+p*(lobe==0?3.7:2.5)*length+.24*Math.sin(p*5+seed)*p;
   points.add(new Point(x,-.65-.35*p+.1*Math.sin(seed),y));
  }
  for(int i=0;i<12;i++)for(int j=0;j<3;j++){
   double side=i%2==0?1:-1,p=j/2.,age=((tick+i*7)%37)/37.;
   points.add(new Point(side*(1.05+(i%5)*.37+.2*Math.sin(i+age*5)), -.7-.2*Math.cos(i),.2+(i%4)*.55+age*.9+p*.4));
  }
  return points;
 }
}
