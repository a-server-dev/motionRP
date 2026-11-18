package dev.aserver.motionrp;

import dev.aserver.motionrp.core.TitanTimeline;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.profile.*;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.LightningStrike;
import org.bukkit.util.Vector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static dev.aserver.motionrp.core.TitanTimeline.*;

final class TitanScene extends SummonScene {
    private final EntityTracker body;
    private final String playerModel;
    private final TitanState state;
    private DummyTracker giant;
    private Location landing,movingFrom,movingTo,lastOwned;
    private final List<LightningStrike> lightning=new ArrayList<>();
    private record Bolt(DummyTracker tracker,int ends) {}
    private final List<Bolt> bolts=new ArrayList<>();
    TitanScene(MotionPlugin plugin,Player owner,String playerModel,EntityTracker body,TitanState state) {
        super(plugin,owner,"Titan",END);this.body=body;this.playerModel=playerModel;this.state=state;
    }
    @Override String phase() { return TitanTimeline.phase(tick); }
    @Override void update() {
        if(tick==BITE) sound(at(.3,.5,1.5),Sound.ENTITY_PLAYER_ATTACK_CRIT,.55f,.8f);
        if(tick==FLASH) {
            sound(origin,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.3f,.7f);
            sound(origin,Sound.ENTITY_LIGHTNING_BOLT_IMPACT,1,.85f);
        }
        if(tick==TRANSFORM) {
            body.update(TrackerUpdateAction.togglePart(false));
            state.begin();
            if(state.bundledSkin()) {
                giant=model(SummonAssets.TITAN_BODY,origin,(float)state.originalScale(),"giant");
                loop(giant,"titan_idle");
            } else {
            String url=state.skin().toString();
            String raw=Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\""+url+"\"}}}").getBytes(StandardCharsets.UTF_8));
            var profile=ModelProfile.of(new ModelProfileInfo(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)),"MotionRP_Titan"),
                    new ModelProfileSkin(state.skin(),null,false,raw));
            giant=Objects.requireNonNull(BetterModel.limbOrNull(playerModel)).create(BukkitAdapter.adapt(origin),profile,MODIFIER,
                    t->t.scaler(ModelScaler.value((float)state.originalScale())));
            giant.animate("titan_idle",AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build());
            audience.show(giant,plugin,prefix,"giant");
            }
            sound(origin,Sound.ENTITY_GENERIC_EXPLODE,1.5f,.6f);
        }
        if(giant!=null && tick<EXIT) {
            body.update(TrackerUpdateAction.togglePart(false));
            double value=TitanTimeline.scale(tick,state.originalScale());state.scale(value);
            giant.scaler(ModelScaler.value((float)Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.SCALE)).getValue()));
            Location current=owner.getLocation();current.setPitch(0);giant.location(BukkitAdapter.adapt(current));
            if(tick==GROWN) trace("scale="+owner.getAttribute(org.bukkit.attribute.Attribute.SCALE).getBaseValue()+" skin="+(state.bundledSkin()?"bundled:titan.png":state.skin().getPath()));
        }
        if(tick>=FLASH && tick<GROWN+8 && tick%4==2) {
            double a=(tick-FLASH)*.9;Location location=at(Math.cos(a)*2.8,Math.sin(a)*2.8,0);
            bolts.add(new Bolt(model(SummonAssets.TITAN_BOLT,location,2,"lightning"+tick),tick+6));
            if((tick-FLASH)%8==0) {
                LightningStrike bolt=origin.getWorld().strikeLightningEffect(location);
                bolt.setSilent(true);bolt.setVisibleByDefault(false);
                for(Player p:audience.players()) p.showEntity(plugin,bolt);
                lightning.add(bolt);trace("native lightning id="+bolt.getEntityId()+" tick="+tick);
            }
        }
        if(tick>=FLASH && tick<GROWN+8 && tick%2==0) {
            for(double height:new double[]{1.5,5,10,15}) {
                for(Player p:audience.players()) p.spawnParticle(Particle.FLASH,at(0,0,height),2,1.8,1,1.8,0,Color.fromRGB(255,250,225));
                particle(at(0,0,height),Particle.ELECTRIC_SPARK,30,2.2,.15);
                dust(at(0,0,height),new Particle.DustOptions(Color.fromRGB(255,247,195),4),18,2);
            }
        }
        for(var iterator=bolts.iterator();iterator.hasNext();) {
            Bolt bolt=iterator.next();if(tick>=bolt.ends()) { audience.hide(bolt.tracker());iterator.remove(); }
        }
        if(tick>=TRANSFORM && tick%4==0) {
            double height=tick>=GROWN?12:Math.max(2,owner.getAttribute(org.bukkit.attribute.Attribute.SCALE).getValue()*1.2);
            particle(at(0,0,height*.5),Particle.CLOUD,tick>=EXIT?45:8,height*.25,.025);
        }
        if(tick==GROWN) sound(at(0,0,10),Sound.ENTITY_ENDER_DRAGON_GROWL,1,.6f);
        if(tick==EXIT) {
            landing=ground(at(0,-5,0));
            if(landing==null) { plugin.playback().stop(owner.getUniqueId());return; }
            landing.subtract(0,.12,0);
            state.prepareExit(origin);
            body.hideOption(EntityHideOption.FALSE);
            // Hide options are cached per viewer. Remove those caches before
            // refreshing the real skin so its packets can reveal the human.
            var registry=body.registry();
            for(var viewer:registry.viewedPlayer().toList()) registry.remove(viewer.player());
            state.restoreAppearance();registry.refresh();
            trace("nape exit height="+NAPE_HEIGHT+" tick="+tick);
            sound(at(0,-1.8,NAPE_HEIGHT),Sound.ENTITY_PLAYER_BREATH,.9f,.8f);
        }
        if(tick>=EXIT && tick<=LAND) {
            var pose=exitPose(tick,landing.getY()-origin.getY());
            Location next=at(0,pose.forward(),pose.height());
            next.setYaw(owner.getLocation().getYaw());next.setPitch(owner.getLocation().getPitch());
            if(!move(next,tick==EXIT)) { plugin.playback().stop(owner.getUniqueId());return; }
            owner.setFallDistance(0);owner.setVelocity(new Vector());
            if(tick==LAND) {
                state.landed();trace("land x="+next.getX()+" y="+next.getY()+" z="+next.getZ());
                sound(next,Sound.ENTITY_GENERIC_SMALL_FALL,1,.8f);particle(next,Particle.CLOUD,24,.6,.04);
            }
        }
        if(tick==STEAM) { audience.hide(giant);giant=null; }
    }
    @Override public boolean ownsMove(Location from,Location to) {
        return movingFrom!=null && movingFrom.equals(from) && movingTo.equals(to);
    }
    private boolean move(Location to,boolean emerge) {
        Location from=owner.getLocation();Vector delta=to.toVector().subtract(from.toVector());
        int steps=emerge?1:Math.max(1,(int)Math.ceil(delta.length()/.15));
        for(int i=1;i<=steps;i++) {
            Vector offset=delta.clone().multiply(i/(double)steps);Location point=from.clone().add(offset);
            for(double x:new double[]{-.5,.5}) for(double z:new double[]{-.5,.5})
                if(!point.getWorld().isChunkLoaded((int)Math.floor(point.getX()+x)>>4,(int)Math.floor(point.getZ()+z)>>4)) return false;
            if(!point.getWorld().getWorldBorder().isInside(point) || owner.wouldCollideUsing(owner.getBoundingBox().shift(offset))) return false;
        }
        movingFrom=from;movingTo=to;
        try {
            boolean success=owner.teleport(to,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            if(success) lastOwned=to.clone();return success;
        } finally { movingFrom=null;movingTo=null; }
    }
    @Override void onClose() {
        for(var bolt:lightning) bolt.remove();lightning.clear();
        // An unrelated teleport must retain its destination; only our flight owns the return anchor.
        if(lastOwned!=null && (owner.getWorld()!=lastOwned.getWorld() || owner.getLocation().distanceSquared(lastOwned)>.01)) state.landed();
        state.restore();owner.setFallDistance(0);
    }
}
