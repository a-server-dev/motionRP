package dev.aserver.motionrp;
import com.google.gson.*;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Small bounded timeline interpreter for the editor's validated recipes. */
final class StudioScene extends SummonScene {
 private final JsonObject project;private final java.util.List<JsonObject> events=new java.util.ArrayList<>();private final Map<String,DummyTracker> models=new HashMap<>();private final AerialAnchor flight;
 StudioScene(MotionPlugin plugin,Player player,EntityTracker body,JsonObject project){super(plugin,player,"Studio-"+project.get("id").getAsString(),project.get("duration").getAsInt(),64,true);this.project=project;for(var raw:project.getAsJsonArray("events"))events.add(raw.getAsJsonObject());events.sort(java.util.Comparator.comparing(e->!e.get("type").getAsString().equals("move_player")));flight=new AerialAnchor(plugin,player);body.update(TrackerUpdateAction.moveDuration(1));}
 private static int n(JsonObject e,String k,int d){return e.has(k)?e.get(k).getAsInt():d;}
 private static double v(JsonObject e,String k,double d){return e.has(k)?e.get(k).getAsDouble():d;}
 private Location point(JsonObject e){var from=e.has("from")?e.getAsJsonArray("from"):JsonParser.parseString("[0,0,0]").getAsJsonArray();var to=e.has("to")?e.getAsJsonArray("to"):from;double t=Math.clamp((tick-n(e,"tick",0))/(double)Math.max(1,n(e,"endTick",tick+1)-n(e,"tick",0)-1),0,1);double[] p=new double[3];for(int i=0;i<3;i++)p[i]=from.get(i).getAsDouble()+(to.get(i).getAsDouble()-from.get(i).getAsDouble())*t;
  Location base=e.has("anchor")&&e.get("anchor").getAsString().equals("player")&&!e.get("type").getAsString().equals("move_player")?owner.getLocation():origin;double a=Math.toRadians(base.getYaw());return base.clone().add(-Math.cos(a)*p[0]-Math.sin(a)*p[2],p[1],-Math.sin(a)*p[0]+Math.cos(a)*p[2]);}
 @Override String phase(){return "TIMELINE";}
 @Override public boolean ownsMove(Location a,Location b){return flight.owns(a,b);}
 @Override public void externalTeleport(){flight.finishAtCurrent();}
 @Override void onClose(){flight.close();}
 @Override void update(){for(var e:events){String id=e.get("id").getAsString(),type=e.get("type").getAsString();int start=n(e,"tick",0),end=n(e,"endTick",start+1);
  if(tick<start)continue;
  if(type.equals("model")){
   if(tick==start){var t=model(e.get("asset").getAsString(),point(e),(float)v(e,"scale",1),id);models.put(id,t);t.rotator(ModelRotator.DEFAULT);t.update(TrackerUpdateAction.moveDuration(1));if(e.has("animation")&&!e.get("animation").getAsString().isBlank()){if(e.has("loop")&&e.get("loop").getAsBoolean())loop(t,e.get("animation").getAsString());else t.animate(e.get("animation").getAsString(),AnimationModifier.DEFAULT_WITH_PLAY_ONCE);}}
   var t=models.get(id);if(t!=null){if(tick>=end){audience.hide(t);models.remove(id);}else{var p=point(e);t.location(BukkitAdapter.adapt(p));float pitch=(float)v(e,"pitch",0),yaw=p.getYaw()+(float)v(e,"yaw",0);t.rotation(()->new ModelRotation(pitch,yaw));}}}
  else if(type.equals("particle")&&tick<end&&(tick-start)%n(e,"every",4)==0)particle(point(e),Particle.valueOf(e.get("particle").getAsString()),n(e,"count",8),v(e,"spread",.3),.02);
  else if(type.equals("sound")&&tick==start)sound(point(e),Sound.valueOf(e.get("sound").getAsString()),(float)v(e,"volume",1),(float)v(e,"pitch",1));
  else if(type.equals("move_player")&&tick<end){var target=point(e);target.setYaw(owner.getYaw());target.setPitch(owner.getPitch());var delta=target.toVector().subtract(owner.getLocation().toVector());boolean safe=delta.length()<=2&&target.getY()>=target.getWorld().getMinHeight()+2;int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));var from=owner.getLocation();for(int i=1;safe&&i<=steps;i++){var p=from.clone().add(delta.clone().multiply(i/(double)steps));safe=p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4)&&!owner.collidesAt(p);}if(!safe||!flight.moveTo(target,origin)){plugin.tell(owner,"Studioの移動経路に障害物があるため終了しました。");plugin.playback().stop(owner.getUniqueId());return;}}
 }}
}
