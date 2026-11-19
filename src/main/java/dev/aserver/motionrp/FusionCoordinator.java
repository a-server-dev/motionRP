package dev.aserver.motionrp;
import dev.aserver.motionrp.core.PairSessions;
import static dev.aserver.motionrp.core.FusionTimeline.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Consensual two-player playback; either participant stopping releases both roles. */
final class FusionCoordinator implements AutoCloseable {
 private record Run(Player left,Player right,AerialAnchor leftAnchor,AerialAnchor rightAnchor,Location center){}
 private final MotionPlugin plugin;private final PairSessions sessions=new PairSessions();private final Map<UUID,Run> runs=new HashMap<>();private final Set<UUID> starting=new HashSet<>();
 FusionCoordinator(MotionPlugin plugin){this.plugin=plugin;}
 private MotionCatalog.Definition definition(boolean left){String id=left?"fusion_left":"fusion_right";return new MotionCatalog.Definition(id,"フュージョン", "motionrp_"+id,id,END+10,null,id);}
 boolean command(Player player,String[] args){
  if(!player.hasPermission("motionrp.use")){plugin.tell(player,"権限がありません。");return true;}
  if(args.length==1||args.length>3){usage(player);return true;}
  if(args.length==2&&Set.of("cancel","stop").contains(args[1].toLowerCase(Locale.ROOT))){plugin.playback().stop(player.getUniqueId());sessions.cancel(player.getUniqueId());plugin.tell(player,"フュージョンを中止しました。");return true;}
  boolean accept=args.length==3&&args[1].equalsIgnoreCase("accept");
  if(args.length!=2&&!accept){usage(player);return true;}
  String name=args[accept?2:1];Player other=Bukkit.getPlayerExact(name);
  if(other==null||other==player||!player.canSee(other)||!other.canSee(player)){plugin.tell(player,"近くにいる相手を指定してください。");return true;}
  if(!eligible(player,other)){plugin.tell(player,"二人とも同じワールドの8ブロック以内で、他のモーションを止めて地上に立ってください。");return true;}
  for(boolean left:new boolean[]{true,false}){String error=plugin.playback().unavailable(definition(left));if(error!=null){plugin.tell(player,error);return true;}}
  if(accept){var session=sessions.accept(player.getUniqueId(),other.getUniqueId(),System.currentTimeMillis());if(session.isEmpty()){plugin.tell(player,"その相手からの有効な招待がありません。");return true;}start(other,player);}
  else if(sessions.invite(player.getUniqueId(),other.getUniqueId(),System.currentTimeMillis())){plugin.tell(player,other.getName()+" に招待しました（30秒）。");plugin.tell(other,player.getName()+" とフュージョン: /motionrp fusion accept "+player.getName()+"（30秒以内）");}
  else plugin.tell(player,"二人のどちらかが招待・再生中です。");
  return true;
 }
 private boolean eligible(Player a,Player b){return a.getWorld()==b.getWorld()&&a.getLocation().distanceSquared(b.getLocation())<=64&&!plugin.playback().active(a.getUniqueId())&&!plugin.playback().active(b.getUniqueId())&&ready(a)&&ready(b);}
 private boolean ready(Player p){return p.isOnline()&&!p.isDead()&&!p.isGliding()&&!p.isInsideVehicle()&&!p.isSleeping()&&!p.getLocation().clone().subtract(0,.15,0).getBlock().isPassable();}
 private Location offset(Location center,double right){double yaw=Math.toRadians(center.getYaw());return center.clone().add(-Math.cos(yaw)*right,0,-Math.sin(yaw)*right);}
 private void start(Player left,Player right){
  Location center=left.getLocation().clone();center.setPitch(0);Location lp=offset(center,-HALF_SPACING),rp=offset(center,HALF_SPACING);
  var la=new AerialAnchor(plugin,left);var ra=new AerialAnchor(plugin,right);
  starting.add(left.getUniqueId());starting.add(right.getUniqueId());
  try{
   if(left.collidesAt(lp)||right.collidesAt(rp)||!la.moveTo(lp,left.getLocation())||!ra.moveTo(rp,right.getLocation()))throw new IllegalStateException("並ぶ場所に障害物があります。");
   plugin.playback().play(left,definition(true));plugin.playback().play(right,definition(false));
   if(!plugin.playback().active(left.getUniqueId())||!plugin.playback().active(right.getUniqueId()))throw new IllegalStateException("二人のモデルを開始できませんでした。");
   var run=new Run(left,right,la,ra,center);runs.put(left.getUniqueId(),run);runs.put(right.getUniqueId(),run);plugin.tell(left,"二人用フュージョンを開始しました。");plugin.tell(right,"二人用フュージョンを開始しました。");
  }catch(RuntimeException e){sessions.cancel(left.getUniqueId());plugin.playback().stop(left.getUniqueId());plugin.playback().stop(right.getUniqueId());la.close();ra.close();plugin.tell(left,"開始できません: "+e.getMessage());plugin.tell(right,"フュージョンを開始できませんでした。");}
  finally{starting.remove(left.getUniqueId());starting.remove(right.getUniqueId());}
 }
 void externalTeleport(UUID id){var run=runs.get(id);if(run!=null)(id.equals(run.left.getUniqueId())?run.leftAnchor:run.rightAnchor).finishAtCurrent();}
 void stopping(UUID id){
  if(starting.contains(id))return;
  var run=runs.remove(id);sessions.cancel(id);if(run==null)return;runs.remove(run.left.getUniqueId());runs.remove(run.right.getUniqueId());
  try{UUID other=id.equals(run.left.getUniqueId())?run.right.getUniqueId():run.left.getUniqueId();plugin.playback().stop(other);}
  finally{try{run.leftAnchor.close();}finally{run.rightAnchor.close();}}
 }
 Location center(UUID participant){var run=runs.get(participant);return run==null?null:run.center.clone();}
 private void usage(Player player){plugin.tell(player,"/motionrp fusion <相手> | fusion accept <招待者> | fusion cancel");}
 @Override public void close(){for(UUID id:new ArrayList<>(runs.keySet()))plugin.playback().stop(id);sessions.clear();}
}
