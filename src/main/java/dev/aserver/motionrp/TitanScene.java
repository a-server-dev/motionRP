package dev.aserver.motionrp;

import dev.aserver.motionrp.core.TitanTimeline;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.profile.*;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static dev.aserver.motionrp.core.TitanTimeline.*;

final class TitanScene extends SummonScene {
    private final EntityTracker body;
    private final String playerModel;
    private final TitanState state;
    private DummyTracker giant;
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
            String url=state.skin().toString();
            String raw=Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\""+url+"\"}}}").getBytes(StandardCharsets.UTF_8));
            var profile=ModelProfile.of(new ModelProfileInfo(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)),"MotionRP_Titan"),
                    new ModelProfileSkin(state.skin(),null,false,raw));
            giant=Objects.requireNonNull(BetterModel.limbOrNull(playerModel)).create(BukkitAdapter.adapt(origin),profile,MODIFIER,
                    t->t.scaler(ModelScaler.value((float)state.originalScale())));
            giant.animate("titan_idle",AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build());
            audience.show(giant,plugin,prefix,"giant");
            sound(origin,Sound.ENTITY_GENERIC_EXPLODE,1.5f,.6f);
        }
        if(giant!=null) {
            body.update(TrackerUpdateAction.togglePart(false));
            double value=TitanTimeline.scale(tick,state.originalScale());state.scale(value);
            giant.scaler(ModelScaler.value((float)Objects.requireNonNull(owner.getAttribute(org.bukkit.attribute.Attribute.SCALE)).getValue()));
            Location current=owner.getLocation();current.setPitch(0);giant.location(BukkitAdapter.adapt(current));
            if(tick==GROWN) trace("scale="+owner.getAttribute(org.bukkit.attribute.Attribute.SCALE).getBaseValue()+" skin="+state.skin().getPath());
        }
        if(tick>=FLASH && tick<GROWN && tick%4==2) {
            double a=(tick-FLASH)*.9;Location location=at(Math.cos(a)*2.8,Math.sin(a)*2.8,0);
            bolts.add(new Bolt(model(SummonAssets.TITAN_BOLT,location,2,"lightning"+tick),tick+6));
            for(Player p:audience.players()) p.spawnParticle(Particle.FLASH,at(0,0,2),1,0,0,0,0,Color.fromRGB(255,244,190));
            particle(at(0,0,4),Particle.ELECTRIC_SPARK,36,2,.1);
        }
        for(var iterator=bolts.iterator();iterator.hasNext();) {
            Bolt bolt=iterator.next();if(tick>=bolt.ends()) { audience.hide(bolt.tracker());iterator.remove(); }
        }
        if(tick>=TRANSFORM && tick%4==0) {
            double height=giant==null?2:Math.max(2,owner.getAttribute(org.bukkit.attribute.Attribute.SCALE).getValue()*1.2);
            particle(at(0,0,height*.5),Particle.CLOUD,tick>=SHRINK?18:8,height*.25,.025);
        }
        if(tick==GROWN) sound(at(0,0,10),Sound.ENTITY_ENDER_DRAGON_GROWL,1,.6f);
    }
    @Override void onClose() { state.restore(); }
}
