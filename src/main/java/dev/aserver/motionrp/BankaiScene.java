package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import static dev.aserver.motionrp.core.BankaiTimeline.*;

/** First Bankai reveal: the broad Zangetsu compresses into Tensa Zangetsu. */
final class BankaiScene extends SummonScene {
    private static final Particle.DustOptions BLACK=new Particle.DustOptions(Color.fromRGB(18,18,25),1.6f);
    private final EntityTracker body;
    private final Set<Player> heard=new HashSet<>();
    private DummyTracker cleaver,cocoon,wave,tensa,coat;
    BankaiScene(MotionPlugin plugin,Player owner,EntityTracker body) {
        super(plugin,owner,"Bankai",END);this.body=body;body.rotation(()->new ModelRotation(0,origin.getYaw()));
    }
    @Override String phase() { return BankaiTimeline.phase(tick); }
    private Location hand() {var p=BankaiHandPath.at(tick);return at(p.right(),p.forward(),p.height());}
    @Override void update() {
        body.update(TrackerUpdateAction.togglePart(false),bone->Set.of("pri_right_item","pli_left_item").contains(bone.name().rawName()));
        Location palm=hand();
        if(tick==DRAW) { cleaver=model(SummonAssets.ZANGETSU,palm,.72f,"zangetsu");loop(cleaver,"point");sound(palm,Sound.ENTITY_PLAYER_ATTACK_SWEEP,.7f,.65f); }
        if(cleaver!=null) cleaver.location(BukkitAdapter.adapt(palm));
        if(tick==CHARGE) { heard.addAll(audience.players());sound(origin,Sound.ENTITY_WARDEN_SONIC_CHARGE,1,.6f); }
        if(tick>=CHARGE && tick<REVEAL && tick%4==0) {
            particle(at(0,0,1.2),Particle.SQUID_INK,12,.75,.035);
            dust(at(0,0,1.1),BLACK,10,.7);
        }
        if(tick==BANKAI) {
            audience.hide(cleaver);cleaver=null;
            cocoon=model(SummonAssets.BANKAI_COCOON,origin,.2f,"cocoon");loop(cocoon,"swirl");
            wave=model(SummonAssets.BANKAI_WAVE,origin.clone().add(0,.04,0),.5f,"pressure_wave");
            particle(at(0,0,1.2),Particle.SQUID_INK,45,1,.08);flash(at(0,0,1));
            for(Player p:heard) p.stopSound(Sound.ENTITY_WARDEN_SONIC_CHARGE,SoundCategory.PLAYERS);
            sound(origin,Sound.ENTITY_WARDEN_SONIC_BOOM,1.4f,.6f);sound(origin,Sound.ENTITY_BREEZE_WIND_BURST,1.2f,.6f);
        }
        if(cocoon!=null && tick<REVEAL) {
            cocoon.scaler(ModelScaler.value((float)Math.max(.02,BankaiTimeline.cocoonScale(tick))));
            if(tick%4==0) {particle(at(0,0,2),Particle.GUST,4,.8,.02);particle(at(0,0,.2),Particle.CLOUD,8,1.2,.04);}
        }
        if(wave!=null) wave.scaler(ModelScaler.value((float)(.5+5.5*Math.clamp((tick-BANKAI)/44.0,0,1))));
        if(tick==REVEAL) {
            audience.hide(cocoon);cocoon=null;
            tensa=model(SummonAssets.TENSA,palm,.6f,"tensa_zangetsu");loop(tensa,"point");
            coat=model(SummonAssets.BANKAI_COAT,origin,1,"black_coat");loop(coat,"flutter");
            flash(at(0,0,1.1));sound(palm,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.1f,1.3f);
        }
        if(tensa!=null) tensa.location(BukkitAdapter.adapt(palm));
        if(tick==READY) {audience.hide(wave);wave=null;}
        if(tick>=REVEAL && tick%8==0) dust(at(0,0,.3),BLACK,3,.45);
    }
    private void flash(Location at) {for(Player p:audience.players())p.spawnParticle(Particle.FLASH,at,1,0,0,0,0,Color.WHITE);}
    @Override void onClose() {for(Player p:heard)p.stopSound(Sound.ENTITY_WARDEN_SONIC_CHARGE,SoundCategory.PLAYERS);}
}
