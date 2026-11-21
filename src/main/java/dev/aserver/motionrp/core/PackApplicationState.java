package dev.aserver.motionrp.core;
import java.util.*;
/** Session-only evidence about packs offered by MotionRP, never guesses about local packs. */
public final class PackApplicationState {
 public enum Notice { NONE, NOT_APPLIED, OUTDATED }
 private enum State { PENDING, LOADED, FAILED }
 private record Entry(String hash,State state) {}
 private final Map<UUID,Entry> entries=new HashMap<>();
 public void offered(UUID player,String hash){entries.put(player,new Entry(Objects.requireNonNull(hash),State.PENDING));}
 public void status(UUID player,String status){var old=entries.get(player);if(old==null)return;State state=switch(status){case "SUCCESSFULLY_LOADED"->State.LOADED;case "ACCEPTED","DOWNLOADED"->State.PENDING;case "FAILED_DOWNLOAD","FAILED_RELOAD","INVALID_URL","DECLINED","DISCARDED"->State.FAILED;default->null;};if(state!=null)entries.put(player,new Entry(old.hash,state));}
 public Notice notice(UUID player,String currentHash){var e=entries.get(player);if(e==null||e.state==State.PENDING)return Notice.NONE;if(e.state==State.FAILED)return Notice.NOT_APPLIED;return currentHash!=null&&!currentHash.equals(e.hash)?Notice.OUTDATED:Notice.NONE;}
 public void forget(UUID player){entries.remove(player);}
}
