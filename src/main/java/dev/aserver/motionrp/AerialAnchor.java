package dev.aserver.motionrp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import java.io.*;
import java.util.*;

/** Return journal for a temporary pilot seat. Does not change player skin or scale. */
final class AerialAnchor {
    private final MotionPlugin plugin;private final Player player;
    private Location from,to,last;private boolean active;
    AerialAnchor(MotionPlugin plugin,Player player) {this.plugin=plugin;this.player=player;}
    private static NamespacedKey key(MotionPlugin plugin) {return new NamespacedKey(plugin,"aerial_return_v1");}
    boolean moveTo(Location seat,Location ground) {
        if(seat.getY()>seat.getWorld().getMaxHeight()-2 || !seat.getWorld().getWorldBorder().isInside(seat) || player.collidesAt(seat)) return false;
        if(!active) {
            try {
                var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
                out.writeUTF(ground.getWorld().getUID().toString());out.writeDouble(ground.getX());out.writeDouble(ground.getY());out.writeDouble(ground.getZ());
                out.writeFloat(ground.getYaw());out.writeFloat(ground.getPitch());out.writeBoolean(player.hasGravity());out.writeBoolean(player.getAllowFlight());out.writeBoolean(player.isFlying());
                player.getPersistentDataContainer().set(key(plugin),PersistentDataType.BYTE_ARRAY,bytes.toByteArray());player.saveData();active=true;
            } catch(IOException e) {throw new UncheckedIOException(e);}
        }
        player.setAllowFlight(true);player.setFlying(true);player.setGravity(false);from=player.getLocation();to=seat;
        try {boolean ok=player.teleport(seat,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);if(ok)last=seat.clone();return ok;}
        finally {from=null;to=null;player.setVelocity(new Vector());player.setFallDistance(0);}
    }
    boolean owns(Location a,Location b) {return from!=null && from.equals(a) && to.equals(b);}
    void close() {
        if(!active)return;
        boolean returnToGround=last==null || (player.getWorld()==last.getWorld() && player.getLocation().distanceSquared(last)<.04);
        restore(plugin,player,returnToGround,this);active=player.getPersistentDataContainer().has(key(plugin));
    }
    void finishAtCurrent() {if(active) {restore(plugin,player,false,this);active=player.getPersistentDataContainer().has(key(plugin));}}
    static void recover(MotionPlugin plugin,Player player) {restore(plugin,player,true,null);}
    private static void restore(MotionPlugin plugin,Player player,boolean returnToGround,AerialAnchor anchor) {
        byte[] bytes=player.getPersistentDataContainer().get(key(plugin),PersistentDataType.BYTE_ARRAY);if(bytes==null)return;
        try {
            var in=new DataInputStream(new ByteArrayInputStream(bytes));World world=Bukkit.getWorld(UUID.fromString(in.readUTF()));
            Location safe=new Location(world,in.readDouble(),in.readDouble(),in.readDouble(),in.readFloat(),in.readFloat());boolean gravity=in.readBoolean(),allowFlight=in.readBoolean(),flying=in.readBoolean();
            if(player.isDead())return;
            if(returnToGround && world!=null && player.getWorld()==world) {
                if(player.collidesAt(safe)) {
                    boolean found=false;
                    for(int dy=1;dy<=4;dy++) {var test=safe.clone().add(0,dy,0);if(!player.collidesAt(test)) {safe=test;found=true;break;}}
                    if(!found)return;
                }
                if(anchor!=null) {anchor.from=player.getLocation();anchor.to=safe;}
                try {if(!player.teleport(safe,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN))return;}
                finally {if(anchor!=null) {anchor.from=null;anchor.to=null;}}
            }
            player.setFlying(flying && allowFlight);player.setAllowFlight(allowFlight);player.setGravity(gravity);player.setFallDistance(0);player.setVelocity(new Vector());
            player.getPersistentDataContainer().remove(key(plugin));player.saveData();
        } catch(IOException | IllegalArgumentException e) {throw new IllegalStateException("Cannot recover aerial seat; journal retained",e);}
    }
}
