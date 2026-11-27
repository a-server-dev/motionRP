package dev.aserver.motionrp;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.manager.PlayerManager;
import org.bukkit.entity.Player;
/** Channel registration belongs to BetterModel's player lifecycle, not to a scene. */
final class ModelViewers {
 private ModelViewers(){}
 static boolean ready(Player player){
  // UUID lookup is read-only and safe for NPCs. player(PlatformPlayer) creates
  // a handler and can double-inject a shared or already registered Netty channel.
  return player.isOnline()&&BetterModel.platform().manager(PlayerManager.class).player(player.getUniqueId())!=null;
 }
}
