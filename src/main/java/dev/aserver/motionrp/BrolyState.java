package dev.aserver.motionrp;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.BrolyTimeline.*;
/** Shared by the two consenting roles. The attacker's scene advances it once per tick. */
final class BrolyState {
 final Location origin;final Vector direction;Location attacker,victim,impact;int impactTick=-1;boolean realBlock;double distance;
 BrolyState(Location center){origin=center.clone();direction=new Vector(-Math.sin(Math.toRadians(center.getYaw())),0,Math.cos(Math.toRadians(center.getYaw())));attacker=origin.clone();victim=ahead(origin,1.35,0,true);}
 static Location ahead(Location center,double forward,double up,boolean faceBack){double a=Math.toRadians(center.getYaw());var p=center.clone().add(-Math.sin(a)*forward,up,Math.cos(a)*forward);p.setPitch(0);if(faceBack)p.setYaw(center.getYaw()+180);return p;}
 record Contact(double travel,Location point){}
 Contact contact(Location feet,double travel){
  Contact nearest=null;
  // Swept volume: sides, centre and both head/torso levels. Never loads chunks.
  Vector right=new Vector(direction.getZ(),0,-direction.getX());
  for(double side:new double[]{-BODY_MARGIN,0,BODY_MARGIN})for(double height:new double[]{.15,.9,1.7}){
   var start=feet.clone().add(right.clone().multiply(side)).add(0,height,0);
   for(double t=0;t<=travel+.1;t+=.25){var check=start.clone().add(direction.clone().multiply(t));if(!check.getWorld().isChunkLoaded(check.getBlockX()>>4,check.getBlockZ()>>4))return new Contact(-1,start);}
   var hit=start.getWorld().rayTraceBlocks(start,direction,travel+.08,FluidCollisionMode.NEVER,true);
   if(hit!=null){double d=start.toVector().distance(hit.getHitPosition());if(nearest==null||d<nearest.travel)nearest=new Contact(d,hit.getHitPosition().toLocation(start.getWorld()));}
  }return nearest;
 }
 boolean advance(int tick,Player target){
  if(tick<LAUNCH){double p=Math.clamp((tick-12)/20.0,0,1);attacker=ahead(origin,.5*p,0,false);victim=ahead(origin,1.35,.55*p,true);return true;}
  if(impactTick>=0)return true;
  double next=nextDistance(distance),step=next-distance;var hit=contact(victim,step);if(hit!=null&&hit.travel<0)return false;double travel=hit==null?step:safeTravel(step,hit.travel);
  var before=victim.clone();var proposed=victim.clone().add(direction.clone().multiply(travel));
  if(target.collidesAt(proposed)){double lo=0,hi=travel;for(int i=0;i<12;i++){double mid=(lo+hi)/2;var p=before.clone().add(direction.clone().multiply(mid));if(target.collidesAt(p))hi=mid;else lo=mid;}travel=lo;proposed=before.clone().add(direction.clone().multiply(travel));if(hit==null)hit=new Contact(travel,proposed.clone().add(direction.clone().multiply(.5)).add(0,1,0));}
  victim=proposed;distance+=travel;attacker=victim.clone().subtract(direction.clone().multiply(1.05));attacker.setYaw(origin.getYaw());
  if(hit!=null||next>=MAX_DISTANCE){impactTick=tick;realBlock=hit!=null;impact=hit==null?victim.clone().add(direction.clone().multiply(.55)).add(0,1.3,0):hit.point.clone();impact.setY(victim.getY()+1.3);}
  return true;
 }
}
