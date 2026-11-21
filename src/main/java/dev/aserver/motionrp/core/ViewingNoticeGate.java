package dev.aserver.motionrp.core;
import java.util.*;
/** One combined reminder per viewer, even when several actors start together. */
public final class ViewingNoticeGate {
 private final Map<UUID,Long> last=new HashMap<>();
 public boolean allow(UUID viewer,long now){var before=last.get(viewer);if(before!=null&&now>=before&&now-before<30_000)return false;last.put(viewer,now);return true;}
 public void forget(UUID viewer){last.remove(viewer);}
}
