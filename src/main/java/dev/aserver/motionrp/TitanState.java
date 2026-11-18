package dev.aserver.motionrp;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.net.URI;
import java.util.*;

/** Reversible server-local skin/scale change, with a player.dat recovery journal. */
final class TitanState {
    static final String DEFAULT_SKIN="https://textures.minecraft.net/texture/1c6cd7202c34e78f3073090349f7d973b288af5e5b7334dd249010b3f27078f9";
    private final MotionPlugin plugin;
    private final Player player;
    private final URI skin;
    private final double originalScale;
    private final boolean originalInvisible;
    private final List<ProfileProperty> originalTextures;
    private boolean began;
    private static final Properties PLACEHOLDER=placeholder();
    private static Properties placeholder() {
        try(var input=Objects.requireNonNull(TitanState.class.getResourceAsStream("/titan-placeholder.properties"))) {
            var properties=new Properties();properties.load(input);return properties;
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    TitanState(MotionPlugin plugin,Player player) {
        this.plugin=plugin;this.player=player;
        recover(plugin,player);
        originalScale=Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).getBaseValue();
        originalInvisible=player.isInvisible();
        originalTextures=player.getPlayerProfile().getProperties().stream().filter(p->p.getName().equals("textures")).toList();
        skin=URI.create(plugin.getConfig().getString("titan.skin-url",DEFAULT_SKIN));
        if(!"textures.minecraft.net".equalsIgnoreCase(skin.getHost()) || !Set.of("http","https").contains(skin.getScheme())
                || !skin.getPath().matches("/texture/[0-9a-fA-F]{64}")) throw new IllegalArgumentException("Invalid titan.skin-url");
    }
    URI skin() { return skin; }
    double originalScale() { return originalScale; }
    private static NamespacedKey key(MotionPlugin plugin,String name) { return new NamespacedKey(plugin,"titan_"+name+"_v1"); }
    void begin() {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeInt(originalTextures.size());
            for(var p:originalTextures) { out.writeUTF(p.getValue());out.writeBoolean(p.getSignature()!=null);if(p.getSignature()!=null) out.writeUTF(p.getSignature()); }
            var data=player.getPersistentDataContainer();
            data.set(key(plugin,"skin"),PersistentDataType.BYTE_ARRAY,bytes.toByteArray());
            data.set(key(plugin,"scale"),PersistentDataType.DOUBLE,originalScale);
            data.set(key(plugin,"invisible"),PersistentDataType.BYTE,(byte)(originalInvisible?1:0));
            player.saveData();began=true;
            player.setInvisible(true);
            var changed=player.getPlayerProfile().clone();
            if(skin.toString().equals(DEFAULT_SKIN)) {
                changed.removeProperty("textures");
                changed.setProperty(new ProfileProperty("textures",PLACEHOLDER.getProperty("value"),PLACEHOLDER.getProperty("signature")));
            } else {
                var texturesNew=changed.getTextures();texturesNew.setSkin(skin.toURL());changed.setTextures(texturesNew);
            }
            player.setPlayerProfile(changed);
        } catch(IOException failure) { throw new UncheckedIOException(failure); }
    }
    void scale(double value) { Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(value); }
    void prepareExit(Location safeGround) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            out.writeUTF(safeGround.getWorld().getUID().toString());
            out.writeDouble(safeGround.getX());out.writeDouble(safeGround.getY());out.writeDouble(safeGround.getZ());
            out.writeFloat(safeGround.getYaw());out.writeFloat(safeGround.getPitch());
            player.getPersistentDataContainer().set(key(plugin,"return"),PersistentDataType.BYTE_ARRAY,bytes.toByteArray());
            player.saveData();
        } catch(IOException failure) { throw new UncheckedIOException(failure); }
    }
    void landed() {
        player.getPersistentDataContainer().remove(key(plugin,"return"));player.saveData();
    }
    void restoreAppearance() { restoreState(plugin,player,false); }
    void restore() { if(began) { recover(plugin,player);began=false; } }
    static void recover(MotionPlugin plugin,Player player) { restoreState(plugin,player,true); }
    private static void restoreState(MotionPlugin plugin,Player player,boolean finish) {
        var data=player.getPersistentDataContainer();
        Double previous=data.get(key(plugin,"scale"),PersistentDataType.DOUBLE);
        if(previous==null) return;
        Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(previous);
        Byte invisible=data.get(key(plugin,"invisible"),PersistentDataType.BYTE);
        byte[] bytes=data.get(key(plugin,"skin"),PersistentDataType.BYTE_ARRAY);
        if(player.isDead()) { player.setInvisible(invisible!=null && invisible!=0);player.saveData();return; }
        try {
            if(bytes!=null) {
                var input=new DataInputStream(new ByteArrayInputStream(bytes));int count=input.readInt();
                if(count<0 || count>16) throw new IOException("Invalid titan recovery property count");
                var original=new ArrayList<ProfileProperty>();
                for(int i=0;i<count;i++) { String value=input.readUTF();String signature=input.readBoolean()?input.readUTF():null;original.add(new ProfileProperty("textures",value,signature)); }
                var profile=player.getPlayerProfile().clone();profile.removeProperty("textures");
                for(var p:original) profile.setProperty(p);
                var current=player.getPlayerProfile().getProperties().stream().filter(p->p.getName().equals("textures")).toList();
                if(!new HashSet<>(current).equals(new HashSet<>(original))) player.setPlayerProfile(profile);
            }
            player.setInvisible(invisible!=null && invisible!=0);
            if(finish) {
                byte[] returnTo=data.get(key(plugin,"return"),PersistentDataType.BYTE_ARRAY);
                if(returnTo!=null) {
                    var input=new DataInputStream(new ByteArrayInputStream(returnTo));
                    var world=Bukkit.getWorld(UUID.fromString(input.readUTF()));
                    var safe=new Location(world,input.readDouble(),input.readDouble(),input.readDouble(),input.readFloat(),input.readFloat());
                    if(world!=null && player.getWorld()==world && !player.collidesAt(safe)) {
                        player.teleport(safe,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                        player.setFallDistance(0);player.setVelocity(new org.bukkit.util.Vector());
                    }
                }
                data.remove(key(plugin,"scale"));data.remove(key(plugin,"skin"));data.remove(key(plugin,"invisible"));data.remove(key(plugin,"return"));
            }
            player.saveData();
        } catch(IOException failure) { throw new UncheckedIOException("Titan recovery journal is invalid",failure); }
        finally { player.setInvisible(invisible!=null && invisible!=0); }
    }
}
