package dev.aserver.motionrp;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.BrolyTimeline.*;
/** The recipient starts at the actual acceptance position, not a staged teleport. */
final class BrolyState {
 final Location origin,victimStart;final Vector direction;Location attacker,victim,impact,impactVictim;int impactTick=-1;boolean realBlock,fleeBlocked;double distance,flightY=Double.NaN;
 String failure="進行先に障害物があるため演出を終了しました。";
 BrolyState(Location center,Location recipient){
  origin=center.clone();victimStart=recipient.clone();direction=recipient.toVector().subtract(center.toVector()).setY(0);
  if(direction.lengthSquared()<.25)direction.setX(-Math.sin(Math.toRadians(center.getYaw()))).setZ(Math.cos(Math.toRadians(center.getYaw())));direction.normalize();
  origin.setDirection(direction);origin.setPitch(0);origin.setYaw(Location.normalizeYaw(origin.getYaw()));attacker=origin.clone();victim=recipient.clone();victim.setYaw(origin.getYaw());victim.setPitch(0);
 }
 record Contact(double travel,Location point){}
 Contact contact(Location feet,double travel){
  Contact nearest=null;Vector right=new Vector(direction.getZ(),0,-direction.getX());
  for(double side:new double[]{-BODY_MARGIN,0,BODY_MARGIN})for(double height:new double[]{.01,.9,1.79}){
   var start=feet.clone().add(right.clone().multiply(side)).add(0,height,0);
   for(double t=0;t<=travel+.1;t+=.25){var check=start.clone().add(direction.clone().multiply(t));if(!check.getWorld().isChunkLoaded(check.getBlockX()>>4,check.getBlockZ()>>4))return new Contact(-1,start);}
   var hit=start.getWorld().rayTraceBlocks(start,direction,travel+.08,FluidCollisionMode.NEVER,true);
   if(hit!=null){double d=start.toVector().distance(hit.getHitPosition());if(nearest==null||d<nearest.travel)nearest=new Contact(d,hit.getHitPosition().toLocation(start.getWorld()));}
  }return nearest;
 }
 record Clear(Location point,boolean blocked){}
 private Clear clear(Player player,Location from,Location to){
  var delta=to.toVector().subtract(from.toVector());int steps=Math.max(1,(int)Math.ceil(delta.length()/Math.max(.02,Math.min(.15,player.getBoundingBox().getWidthX()/4))));Location last=from.clone();
  for(int i=1;i<=steps;i++){var p=from.clone().add(delta.clone().multiply(i/(double)steps));p.setYaw(to.getYaw());p.setPitch(to.getPitch());
   if(!p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4)){failure="読み込み済みの範囲を超えるため演出を終了しました。";return null;}
   if(player.collidesAt(p))return new Clear(last,true);last=p;
  }return new Clear(to,false);
 }
 boolean advance(int tick,Player source,Player target){
  if(tick<CHARGE){var desired=origin.clone().add(0,lift(tick),0);var safe=clear(source,attacker,desired);if(safe==null||safe.blocked)return false;attacker=safe.point;}
  if(tick<TURN){
   if(!fleeBlocked){var desired=victimStart.clone().add(direction.clone().multiply(fleeDistance(tick+1))).add(0,lift(tick),0);desired.setYaw(origin.getYaw());desired.setPitch(0);var safe=clear(target,victim,desired);if(safe==null)return false;victim=safe.point;fleeBlocked=safe.blocked;}
   return true;
  }
  if(tick<CHARGE){var raised=victim.clone();raised.setY(victimStart.getY()+HOVER_HEIGHT);var safe=clear(target,victim,raised);if(safe==null||safe.blocked)return false;victim=safe.point;victim.setYaw(Location.normalizeYaw((float)(origin.getYaw()+180*turnProgress(tick))));return true;}
  victim.setYaw(Location.normalizeYaw(origin.getYaw()+180));
  if(tick<GRAB){var end=victim.clone().subtract(direction.clone().multiply(.9));var base=origin.clone().add(0,HOVER_HEIGHT,0);var desired=base.clone().add(end.toVector().subtract(base.toVector()).multiply(chargeProgress(tick)));desired.setYaw(origin.getYaw());var safe=clear(source,attacker,desired);if(safe==null||safe.blocked)return false;attacker=safe.point;return true;}
  if(tick<LAUNCH){double p=Math.clamp((tick-GRAB)/(double)(LAUNCH-GRAB),0,1);var desired=victim.clone();desired.setY(victimStart.getY()+HOVER_HEIGHT+.1*p);var safe=clear(target,victim,desired);if(safe==null||safe.blocked)return false;victim=safe.point;var end=victim.clone().subtract(direction.clone().multiply(.9));end.setYaw(origin.getYaw());safe=clear(source,attacker,end);if(safe==null||safe.blocked)return false;attacker=safe.point;return true;}
  if(impactTick>=0){
   double p=dentProgress(tick-impactTick);var desired=impactVictim.clone().add(direction.clone().multiply(realBlock?0:DENT_DEPTH*p));desired.setY(impactVictim.getY()+(victimStart.getY()+IMPACT_HEIGHT-impactVictim.getY())*p);var safe=clear(target,victim,desired);if(safe==null||safe.blocked)return false;victim=safe.point;var chase=victim.clone().subtract(direction.clone().multiply(1.05));chase.setYaw(origin.getYaw());safe=clear(source,attacker,chase);if(safe==null||safe.blocked)return false;attacker=safe.point;
   return true;
  }
  if(Double.isNaN(flightY))flightY=victim.getY();
  double next=nextDistance(distance),step=next-distance;var hit=contact(victim,step);if(hit!=null&&hit.travel<0){failure="読み込み済みの範囲を超えるため演出を終了しました。";return false;}double travel=hit==null?step:safeTravel(step,hit.travel);
  var before=victim.clone();var proposed=victim.clone().add(direction.clone().multiply(travel));proposed.setY(flightY+(victimStart.getY()+IMPACT_HEIGHT-flightY)*Math.clamp((distance+travel)/MAX_DISTANCE,0,1));var safe=clear(target,before,proposed);if(safe==null)return false;
  if(safe.blocked){proposed=safe.point;travel=proposed.toVector().subtract(before.toVector()).dot(direction);if(hit==null)hit=new Contact(travel,proposed.clone().add(direction.clone().multiply(.5)).add(0,1,0));}
  victim=proposed;distance+=travel;var chase=victim.clone().subtract(direction.clone().multiply(1.05));chase.setYaw(origin.getYaw());safe=clear(source,attacker,chase);if(safe==null||safe.blocked)return false;attacker=safe.point;
  if(hit!=null||next>=MAX_DISTANCE){impactTick=tick;realBlock=hit!=null;impact=hit==null?victim.clone().add(direction.clone().multiply(.55)).add(0,1.3,0):hit.point.clone();impact.setY(victim.getY()+1.3);impactVictim=victim.clone();}
  return true;
 }
}
