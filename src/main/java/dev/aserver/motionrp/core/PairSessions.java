package dev.aserver.motionrp.core;

import java.util.*;

/** Main-thread building block for future two-player scenes. No player is moved by an invitation. */
public final class PairSessions {
    public record Session(UUID id,UUID leader,UUID partner) {}
    private record Invite(UUID from,UUID to,long expires) {}
    private final Map<UUID,Invite> pending=new HashMap<>();
    private final Map<UUID,Session> active=new HashMap<>();
    public boolean invite(UUID from,UUID to,long now){
        expire(now);
        if(from.equals(to)||active.containsKey(from)||active.containsKey(to)||pending.containsKey(from)||pending.containsKey(to))return false;
        var invite=new Invite(from,to,now+30_000);pending.put(from,invite);pending.put(to,invite);return true;
    }
    public Optional<Session> accept(UUID recipient,UUID inviter,long now){
        expire(now);Invite invite=pending.get(recipient);
        if(invite==null || !invite.to.equals(recipient)||!invite.from.equals(inviter))return Optional.empty();
        pending.remove(invite.from);pending.remove(invite.to);
        var session=new Session(UUID.randomUUID(),invite.from,invite.to);active.put(invite.from,session);active.put(invite.to,session);return Optional.of(session);
    }
    /** Stop both roles on either player's damage, death, disconnect, teleport, or explicit cancel. */
    public Set<UUID> cancel(UUID participant){
        Invite invite=pending.remove(participant);if(invite!=null){pending.remove(invite.from);pending.remove(invite.to);}
        Session session=active.remove(participant);if(session==null)return Set.of();
        active.remove(session.leader);active.remove(session.partner);return Set.of(session.leader,session.partner);
    }
    public Optional<Session> session(UUID participant){return Optional.ofNullable(active.get(participant));}
    public void expire(long now){pending.values().removeIf(i->now>=i.expires);}
    public void clear(){pending.clear();active.clear();}
}
