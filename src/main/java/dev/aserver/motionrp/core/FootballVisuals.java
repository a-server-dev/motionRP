package dev.aserver.motionrp.core;
import static dev.aserver.motionrp.core.FootballFootPath.*;
/** Body-local flame ribbons and the native (-Z) facing of the authored penguin mesh. */
public final class FootballVisuals {
 public static final int FLAME_SAMPLES=12;
 public record Facing(float pitch,float yaw){}
 private FootballVisuals(){}
 public static Facing penguinFacing(double x,double y,double z){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||x*x+y*y+z*z<1e-12)throw new IllegalArgumentException("Invalid penguin target");
  // BetterModel converts authored mesh coordinates by (-X,+Y,-Z). Its +Z beak
  // therefore faces native -Z. Vanilla fixed displays apply Ry(-yaw)*Rx(pitch).
  return new Facing((float)Math.toDegrees(Math.atan2(y,Math.hypot(x,z))),(float)(Math.toDegrees(Math.atan2(-x,z))+180));
 }
 /** Preserve the calf-facing pose on release, then turn along the shot without a yaw wrap snap. */
 public static Facing launchFacing(Facing attached,Facing flight,double progress){
  double p=Math.clamp(progress,0,1),delta=((flight.yaw()-attached.yaw()+540)%360)-180;
  return new Facing((float)(attached.pitch()+(flight.pitch()-attached.pitch())*p),(float)(attached.yaw()+delta*p));
 }
 private static Point sub(Point a,Point b){return new Point(a.right()-b.right(),a.forward()-b.forward(),a.height()-b.height());}
 private static Point unit(Point p){double n=Math.sqrt(p.right()*p.right()+p.forward()*p.forward()+p.height()*p.height());if(n<1e-8)throw new IllegalArgumentException("Degenerate body axis");return new Point(p.right()/n,p.forward()/n,p.height()/n);}
 public static Point flame(Frame frame,int tick,int strand,int sample){
  if(strand<0||strand>1||sample<0||sample>=FLAME_SAMPLES)throw new IllegalArgumentException("Invalid flame sample");
  Point center=frame.torso(),axis=unit(sub(frame.upper(),center)),u=unit(sub(frame.side(),center));
  Point v=unit(new Point(axis.forward()*u.height()-axis.height()*u.forward(),axis.height()*u.right()-axis.right()*u.height(),axis.right()*u.forward()-axis.forward()*u.right()));
  double q=tick*.6+strand*Math.PI+sample*.4,along=-.6+1.2*sample/(FLAME_SAMPLES-1.0),radius=.57+.07*Math.sin(tick*.13+sample*.5);
  double a=Math.cos(q)*radius,b=Math.sin(q)*radius;
  return new Point(center.right()+axis.right()*along+u.right()*a+v.right()*b,center.forward()+axis.forward()*along+u.forward()*a+v.forward()*b,center.height()+axis.height()*along+u.height()*a+v.height()*b);
 }
}
