package dev.aserver.motionrp;

import dev.aserver.motionrp.core.NightGuyTimeline;
import static dev.aserver.motionrp.core.NightGuyTimeline.*;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Red steam and a particle-only dragon accompany the owner's full-body flying kick. */
final class NightGuyScene extends SummonScene {
 private final AerialAnchor pilot;
 private double reach=28,runReach=RUN_DISTANCE;
 private Location landing;
 private Location impact;
 private static final Particle.DustOptions RED=new Particle.DustOptions(Color.fromRGB(235,24,35),2.2f),DARK=new Particle.DustOptions(Color.fromRGB(72,3,8),2.6f),HOT=new Particle.DustOptions(Color.fromRGB(255,155,105),1.2f);
 NightGuyScene(MotionPlugin plugin,Player owner,EntityTracker body){
  super(plugin,owner,"NightGuy",END,144,true);pilot=new AerialAnchor(plugin,owner);body.rotation(()->new ModelRotation(0,origin.getYaw()));
 }
 @Override String phase(){return NightGuyTimeline.phase(tick);}
 @Override public boolean ownsMove(Location a,Location b){return pilot.owns(a,b);}
 @Override void onClose(){pilot.close();}
 private Location relative(double right,double forward,double height){return at(right,forward,height).add(owner.getLocation().toVector().subtract(origin.toVector()));}
 private boolean safe(Location p){
  for(double x:new double[]{-.7,.7})for(double z:new double[]{-.7,.7})if(!p.getWorld().isChunkLoaded((int)Math.floor(p.getX()+x)>>4,(int)Math.floor(p.getZ()+z)>>4))return false;
  return p.getY()<p.getWorld().getMaxHeight()-2&&p.getWorld().getWorldBorder().isInside(p)&&!owner.collidesAt(p);
 }
 private boolean move(Location target){
  Location from=owner.getLocation();Vector delta=target.toVector().subtract(from.toVector());int steps=Math.max(1,(int)Math.ceil(delta.length()/.18));
  for(int i=1;i<=steps;i++)if(!safe(from.clone().add(delta.clone().multiply(i/(double)steps))))return false;
  target.setYaw(origin.getYaw());target.setPitch(owner.getPitch());return pilot.moveTo(target,origin);
 }
 @Override void update(){
  if(tick==STEAM){sound(origin,Sound.ENTITY_WARDEN_HEARTBEAT,1.4f,.65f);trace("red steam ignites");}
  if(tick>=STEAM&&tick<LANDED){
   double fade=tick<RECOVER?1:1-ease((tick-RECOVER)/(double)(LANDED-RECOVER));
   for(int i=0;i<18;i++){
    double p=((tick+i*7)%28)/28.,a=i*2.39996+tick*.12,r=(.4+p*.75)*fade;
    Location q=relative(Math.cos(a)*r,Math.sin(a)*r,.1+3.4*p*fade);
    dust(q,RED,3,.1);dust(q,DARK,2,.12);if(i%6==0)particle(q,Particle.SMOKE,1,.06,.025);
   }
  }
  if(tick>=DRAGON&&tick<RECOVER){dragon();if(tick==DRAGON){trace("red particle dragon forms around caster");sound(origin,Sound.ENTITY_ENDER_DRAGON_GROWL,1.4f,.7f);}}
  if(tick==RUN){
   for(double d=.2;d<=RUN_DISTANCE;d+=.2){Location p=at(0,d,0);Location floor=safe(p)?ground(p):null;
    if(floor==null||Math.abs(floor.getY()-.12-origin.getY())>.25){runReach=Math.max(0,d-.4);break;}
   }
   trace("sprint begins distance="+runReach+" duration="+(LEAP-RUN));
   sound(origin,Sound.ENTITY_BREEZE_SHOOT,1.7f,.7f);
  }
  if(tick==DASH){
   for(double d=.2;d<=reach+3;d+=.2)if(!safe(at(0,runReach+d,4))){reach=Math.max(0,d-3.0);break;}
   for(int step=0;step<=(int)Math.ceil(reach/.25);step++){double d=Math.max(0,reach-step*.25);
    Location floor=ground(at(0,runReach+d,0));
    if(floor!=null){floor.subtract(0,.12,0);if(safe(floor)){landing=floor;reach=d;break;}}
   }
   if(landing==null){plugin.playback().stop(owner.getUniqueId(),"着地できる地面がありません");return;}
   trace("kick dash reach="+reach);sound(owner.getLocation(),Sound.ENTITY_BREEZE_SHOOT,2,.55f);
  }
  if(tick>=RUN&&tick<=LANDED){
   double h=height(tick);
   if(landing!=null&&tick>=RECOVER)h+=(landing.getY()-origin.getY())*ease((tick-RECOVER)/(double)(LANDED-RECOVER));
   if(!move(at(0,forward(tick,runReach,reach),h))){plugin.playback().stop(owner.getUniqueId(),"突進経路が塞がれています");return;}
   if(tick<LEAP&&tick%2==0){particle(owner.getLocation(),Particle.DUST_PLUME,5,.25,.025);sound(owner.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.3f,1.8f);}
   if(tick==LEAP)trace("sprint complete distance="+runReach+" duration="+(LEAP-RUN));
   if(tick==LANDED){pilot.finishAtCurrent();trace("landed at kick endpoint; no return");sound(owner.getLocation(),Sound.ENTITY_GENERIC_SMALL_FALL,.8f,.9f);}
  }
  if(tick==IMPACT){
   impact=relative(0,2.7,1.1);for(Player p:audience.players())p.spawnParticle(Particle.FLASH,impact,1,0,0,0,0,Color.fromRGB(255,92,65));
   particle(impact,Particle.GUST,8,2.3,.06);dust(impact,RED,180,3);dust(impact,DARK,100,2.6);
   sound(impact,Sound.ENTITY_GENERIC_EXPLODE,3,.6f);sound(impact,Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,2,.5f);trace("flying kick impact; no terrain or entity damage");
  }
  if(impact!=null&&tick<RECOVER+16){
   double age=tick-IMPACT,r=1+age*.27;
   for(int i=0;i<28;i++){double a=i*Math.PI/14;dust(impact.clone().add(at(Math.cos(a)*r,0,Math.sin(a)*r*.7).toVector().subtract(origin.toVector())),RED,2,.12);}
  }

 }
 private void dragon(){
  double grow=ease((tick-DRAGON)/20.),fade=tick<IMPACT?1:1-ease((tick-IMPACT)/(double)(RECOVER-IMPACT));
  // Tapering coils stretch behind the kick, with a broad jaw, paired eyes and swept horns ahead.
  for(int j=0;j<18;j++){
   double z=3.8-j*.85,r=(1.65-j*.065)*grow*fade;
   for(int k=0;k<10;k++){double a=k*Math.PI/5+tick*.12+j*.32;Location p=relative(Math.cos(a)*r,z,1.1+Math.sin(a)*r);
    dust(p,k%3==0?DARK:RED,2,.14);
   }
  }
  for(int side:new int[]{-1,1}){
   for(int j=0;j<12;j++){
    double p=j/11.;dust(relative(side*(.7+p*1.9)*grow,3.5-p*3,1.7+p*1.6),RED,3,.1);
    dust(relative(side*(.35+p*.8)*grow,4.7-p*1.3,.5-p*.25),DARK,3,.12);
   }
   dust(relative(side*.75*grow,4.25,1.65),HOT,3,.04);
  }
 }
}
