package dev.aserver.motionrp;
import dev.aserver.motionrp.core.PairSessions;
import static dev.aserver.motionrp.core.FusionTimeline.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Consensual two-player playback; either participant stopping releases both roles. */
final class FusionCoordinator implements AutoCloseable {
 private record Run(Player left,Player right,AerialAnchor leftAnchor,AerialAnchor rightAnchor,Location center,boolean battle,BrolyState broly){}
 private final MotionPlugin plugin;private final PairSessions sessions=new PairSessions();private final Map<UUID,Run> runs=new HashMap<>();private final Set<UUID> starting=new HashSet<>();
 FusionCoordinator(MotionPlugin plugin){this.plugin=plugin;}
 private MotionCatalog.Definition definition(boolean left,String kind){boolean battle=kind.equals("finalbattle"),broly=kind.equals("broly");String id=broly?(left?"broly_attacker":"broly_victim"):battle?(left?"final_battle_naruto":"final_battle_sasuke"):(left?"fusion_left":"fusion_right");return new MotionCatalog.Definition(id,broly?"ブロリー 岩盤":battle?"ナルト VS サスケ 最終決戦":"フュージョン", "motionrp_"+id,id,(broly?dev.aserver.motionrp.core.BrolyTimeline.END:battle?dev.aserver.motionrp.core.FinalBattleTimeline.END:END)+10,null,id);}
 boolean command(Player player,String[] args){return command(player,args,"fusion");}
 boolean finalBattle(Player player,String[] args){return command(player,args,"finalbattle");}
 boolean broly(Player player,String[] args){return command(player,args,"broly");}
 private boolean command(Player player,String[] args,String command){
  boolean battle=command.equals("finalbattle"),broly=command.equals("broly");String title=broly?"ブロリーの岩盤":battle?"最終決戦":"フュージョン";
  if(!player.hasPermission("motionrp.use")){plugin.tell(player,"権限がありません。");return true;}
  if(args.length==1||args.length>3){usage(player,command);return true;}
  if(args.length==2&&Set.of("cancel","stop").contains(args[1].toLowerCase(Locale.ROOT))){plugin.playback().stop(player.getUniqueId());sessions.cancel(player.getUniqueId());plugin.tell(player,title+"を中止しました。");return true;}
  boolean accept=args.length==3&&args[1].equalsIgnoreCase("accept");
  if(args.length!=2&&!accept){usage(player,command);return true;}
  String name=args[accept?2:1];Player other=Bukkit.getPlayerExact(name);
  if(other==null||other==player||!player.canSee(other)||!other.canSee(player)){plugin.tell(player,"近くにいる相手を指定してください。");return true;}
  if(!eligible(player,other)){plugin.tell(player,"二人とも同じワールドの8ブロック以内で、他のモーションを止めて地上に立ってください。");return true;}
  for(boolean left:new boolean[]{true,false}){String error=plugin.playback().unavailable(definition(left,command));if(error!=null){plugin.tell(player,error);return true;}}
  if(accept){var session=sessions.accept(player.getUniqueId(),other.getUniqueId(),System.currentTimeMillis(),command);if(session.isEmpty()){plugin.tell(player,"その相手からの有効な招待がありません。");return true;}start(other,player,command);}
  else if(sessions.invite(player.getUniqueId(),other.getUniqueId(),System.currentTimeMillis(),command)){plugin.tell(player,other.getName()+" に招待しました（30秒）。");plugin.tell(other,player.getName()+" と"+title+": /motionrp "+command+" accept "+player.getName()+"（30秒以内）"+(broly?" 招待者=ブロリー側、あなた=相手役":battle?" 招待者=ナルト、あなた=サスケ":""));}
  else plugin.tell(player,"二人のどちらかが招待・再生中です。");
  return true;
 }
 private boolean eligible(Player a,Player b){return a.getWorld()==b.getWorld()&&a.getLocation().distanceSquared(b.getLocation())<=64&&!plugin.playback().active(a.getUniqueId())&&!plugin.playback().active(b.getUniqueId())&&ready(a)&&ready(b);}
 private boolean ready(Player p){return p.isOnline()&&!p.isDead()&&!p.isGliding()&&!p.isInsideVehicle()&&!p.isSleeping()&&!p.getLocation().clone().subtract(0,.15,0).getBlock().isPassable();}
 private Location offset(Location center,double right){double yaw=Math.toRadians(center.getYaw());return center.clone().add(-Math.cos(yaw)*right,0,-Math.sin(yaw)*right);}
 private void start(Player left,Player right,String kind){
  boolean battle=kind.equals("finalbattle"),broly=kind.equals("broly");
  Location center=left.getLocation().clone();center.setPitch(0);Location lp=broly?center.clone():battle?battlePosition(center,true,0,58):offset(center,-HALF_SPACING),rp=broly?right.getLocation().clone():battle?battlePosition(center,false,0,58):offset(center,HALF_SPACING);
  var la=new AerialAnchor(plugin,left);var ra=new AerialAnchor(plugin,right);
  starting.add(left.getUniqueId());starting.add(right.getUniqueId());
  try{
   if(battle&&center.getY()+80>center.getWorld().getMaxHeight()-2)throw new IllegalStateException("上空80ブロックの高さが必要です。");
   if(left.collidesAt(lp)||right.collidesAt(rp)||!la.moveTo(lp,left.getLocation())||!ra.moveTo(rp,right.getLocation()))throw new IllegalStateException("並ぶ場所に障害物があります。");
   plugin.playback().play(left,definition(true,kind));plugin.playback().play(right,definition(false,kind));
   if(!plugin.playback().active(left.getUniqueId())||!plugin.playback().active(right.getUniqueId()))throw new IllegalStateException("二人のモデルを開始できませんでした。");
   var run=new Run(left,right,la,ra,center,battle,broly?new BrolyState(center,rp):null);runs.put(left.getUniqueId(),run);runs.put(right.getUniqueId(),run);plugin.tell(left,broly?"ブロリーの岩盤：攻撃側を開始しました。":battle?"最終決戦：ナルト役を開始しました。":"二人用フュージョンを開始しました。");plugin.tell(right,broly?"ブロリーの岩盤：相手役を開始しました。":battle?"最終決戦：サスケ役を開始しました。":"二人用フュージョンを開始しました。");
  }catch(RuntimeException e){sessions.cancel(left.getUniqueId());plugin.playback().stop(left.getUniqueId());plugin.playback().stop(right.getUniqueId());la.close();ra.close();plugin.tell(left,"開始できません: "+e.getMessage());plugin.tell(right,(broly?"ブロリーの岩盤":battle?"最終決戦":"フュージョン")+"を開始できませんでした。");}
  finally{starting.remove(left.getUniqueId());starting.remove(right.getUniqueId());}
 }
 void externalTeleport(UUID id){var run=runs.get(id);if(run!=null)(id.equals(run.left.getUniqueId())?run.leftAnchor:run.rightAnchor).finishAtCurrent();}
 void stopping(UUID id){
  if(starting.contains(id))return;
  var run=runs.remove(id);sessions.cancel(id);if(run==null)return;runs.remove(run.left.getUniqueId());runs.remove(run.right.getUniqueId());
  try{UUID other=id.equals(run.left.getUniqueId())?run.right.getUniqueId():run.left.getUniqueId();plugin.playback().stop(other);}
  finally{try{run.leftAnchor.close();}finally{run.rightAnchor.close();}}
 }
 UUID partner(UUID id){var run=runs.get(id);return run==null?null:(id.equals(run.left.getUniqueId())?run.right.getUniqueId():run.left.getUniqueId());}
 Location center(UUID participant){var run=runs.get(participant);return run==null?null:run.center.clone();}
 private void usage(Player player,String command){plugin.tell(player,"/motionrp "+command+" <相手> | "+command+" accept <招待者> | "+command+" cancel");}
 static Location battlePosition(Location center,boolean naruto,double height,double distance){double yaw=Math.toRadians(center.getYaw()),f=naruto?-distance:distance;var p=center.clone().add(-Math.sin(yaw)*f,height,Math.cos(yaw)*f);p.setYaw(center.getYaw()+(naruto?0:180));return p;}
 boolean ownsMove(UUID id,Location a,Location b){var run=runs.get(id);return run!=null&&(id.equals(run.left.getUniqueId())?run.leftAnchor:run.rightAnchor).owns(a,b);}
 boolean moveBattle(UUID id,Location seat){var run=runs.get(id);if(run==null||!run.battle)return false;return (id.equals(run.left.getUniqueId())?run.leftAnchor:run.rightAnchor).moveTo(seat,run.center);}

 BrolyState brolyState(UUID id){var run=runs.get(id);return run==null?null:run.broly;}
 boolean moveBroly(UUID id,Location attacker,Location victim){var run=runs.get(id);if(run==null||run.broly==null||!id.equals(run.left.getUniqueId()))return false;return run.rightAnchor.moveTo(victim,run.center)&&run.leftAnchor.moveTo(attacker,run.center);}

 @Override public void close(){for(UUID id:new ArrayList<>(runs.keySet()))plugin.playback().stop(id);sessions.clear();}
}
