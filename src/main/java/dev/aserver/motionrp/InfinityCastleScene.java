package dev.aserver.motionrp;

import dev.aserver.motionrp.core.InfinityCastleTimeline;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import static dev.aserver.motionrp.core.InfinityCastleTimeline.*;

/** A cosmetic all-around chamber in a checked aerial seat; physical terrain is untouched. */
final class InfinityCastleScene extends SummonScene {
 private final EntityTracker body;private final AerialAnchor flight;
 private final Location home,stage;private DummyTracker entrance,exit,castle;
 private final DummyTracker[] platforms=new DummyTracker[12],doors=new DummyTracker[8];
 InfinityCastleScene(MotionPlugin plugin,Player player,EntityTracker body){
  super(plugin,player,"InfinityCastle",END,64,true);this.body=body;flight=new AerialAnchor(plugin,player);home=player.getLocation().clone();
  if(player.getLocation().clone().subtract(0,.15,0).getBlock().isPassable())throw new IllegalArgumentException("地上に立って使用してください。");
  stage=home.clone();stage.setPitch(0);stage.setY(Math.min(home.getWorld().getMaxHeight()-20,Math.max(128,home.getY()+108)));
  if(stage.getY()-DROP<home.getY()+8)throw new IllegalArgumentException("無限城の演出に必要な上空の空間がありません。");
  for(int i=0;i<=DROP;i++)if(player.collidesAt(stage.clone().subtract(0,i,0)))throw new IllegalArgumentException("上空の落下経路に障害物があります。");
  for(double y=0;y<=3;y+=.25)if(player.collidesAt(home.clone().add(0,y,0)))throw new IllegalArgumentException("帰還口を出す3ブロック上までの空間が必要です。");
  body.update(TrackerUpdateAction.moveDuration(1));
 }
 @Override String phase(){return InfinityCastleTimeline.phase(tick);}
 @Override public boolean ownsMove(Location a,Location b){return flight.owns(a,b);}
 @Override public void externalTeleport(){flight.finishAtCurrent();}
 @Override void onClose(){flight.close();owner.setFallDistance(0);}
 private Location seat(Location p){p.setYaw(owner.getYaw());p.setPitch(owner.getPitch()==0?0:owner.getPitch());return p;}
 private boolean move(Location p){if(flight.moveTo(seat(p),home))return true;plugin.tell(owner,"進行先に障害物があるため無限城を終了しました。");plugin.playback().stop(owner.getUniqueId());return false;}
 private DummyTracker hatch(Location p,String kind){var t=model("motionrp_castle_fusuma",p,1,kind);t.rotation(()->new ModelRotation(-90,home.getYaw()));t.animate("open",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);return t;}
 private void position(DummyTracker t,Location p){t.location(BukkitAdapter.adapt(p));}
 @Override void update(){
  if(tick==0){entrance=hatch(home.clone().add(0,.04,0),"entrance");sound(home,Sound.BLOCK_WOODEN_DOOR_OPEN,1,.65f);}
  if(tick==FALL){
   if(!move(stage.clone()))return;
   castle=model("motionrp_castle_chamber",stage.clone().subtract(0,36,0),1,"all_around_castle");castle.rotation(()->new ModelRotation(0,home.getYaw()));
   for(int i=0;i<platforms.length;i++){platforms[i]=model("motionrp_castle_platform",stage,1,"moving_platform"+i);platforms[i].update(TrackerUpdateAction.moveDuration(1));}
   for(int i=0;i<doors.length;i++){doors[i]=model("motionrp_castle_fusuma",stage,1,"moving_fusuma"+i);doors[i].update(TrackerUpdateAction.moveDuration(1));loop(doors[i],"slide");}
   sound(stage,Sound.BLOCK_NOTE_BLOCK_BANJO,2,.5f);trace("fall begins returnTick="+RETURN);
  }
  if(tick>FALL&&tick<RETURN){if(!move(stage.clone().subtract(0,depth(tick),0)))return;}
  if(tick>=FALL&&tick<RETURN){
   double age=tick-FALL;
   for(int i=0;i<platforms.length;i++){double a=i*Math.PI/6+.12*Math.sin(age*.018+i),r=9+(i%3)*3+1.5*Math.sin(age*.025+i),y=-8-i*5.5+3*Math.sin(age*.035+i);
    var p=stage.clone().add(r*Math.cos(a),y,r*Math.sin(a));p.setYaw((float)(-Math.toDegrees(a)+90+12*Math.sin(age*.02+i)));position(platforms[i],p);final float yaw=p.getYaw();platforms[i].rotation(()->new ModelRotation(0,yaw));}
   for(int i=0;i<doors.length;i++){double a=i*Math.PI/4,rad=13+3*Math.sin(age*.04+i);var p=stage.clone().add(rad*Math.cos(a),-5-i*8+2*Math.sin(age*.04+i),rad*Math.sin(a));position(doors[i],p);final float yaw=(float)(-Math.toDegrees(a)+90);doors[i].rotation(()->new ModelRotation(0,yaw));}
   if(tick%8==0){sound(owner.getLocation(),Sound.ENTITY_BREEZE_IDLE_AIR,.3f,.6f);particle(owner.getLocation().add(0,2,0),Particle.CLOUD,5,.9,.015);}
   if(tick%24==0)sound(owner.getLocation(),Sound.BLOCK_NOTE_BLOCK_BANJO,.7f,(tick%48==0)?.5f:.8f);
   if(tick==RETURN-12){exit=hatch(home.clone().add(0,EXIT_HEIGHT,0),"return_portal");sound(home.clone().add(0,3,0),Sound.BLOCK_WOODEN_DOOR_OPEN,1,.65f);}
  }
  if(tick==RETURN){
   if(!move(home.clone().add(0,EXIT_HEIGHT,0)))return;
   audience.hide(castle);for(var t:platforms)audience.hide(t);for(var t:doors)audience.hide(t);audience.hide(entrance);
   particle(owner.getLocation(),Particle.POOF,12,.45,.04);sound(owner.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.7f,.8f);trace("return height=3 elapsedFallTicks=160");
  }
  if(tick>RETURN&&tick<LAND)if(!move(home.clone().add(0,exitHeight(tick),0)))return;
  if(tick==LAND){flight.close();if(exit!=null)exit.animate("close",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(home,Sound.BLOCK_WOODEN_DOOR_CLOSE,1,.7f);}
 }
}
