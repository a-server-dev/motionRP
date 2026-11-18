package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import static dev.aserver.motionrp.core.BankaiTimeline.*;

/** First Bankai: low edge-down stance, wrist support, white pressure and blinding dust. */
final class BankaiScene extends SummonScene {
    private static final Particle.DustOptions WHITE=new Particle.DustOptions(Color.WHITE,3.5f);
    private static final Particle.DustOptions BLACK=new Particle.DustOptions(Color.fromRGB(18,18,25),1.6f);
    private final org.bukkit.block.data.BlockData sand=Material.SAND.createBlockData();
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
        if(tick>=CHARGE && tick<BANKAI && tick%6==0) {
            particle(at(0,0,.1),Particle.DUST_PLUME,5,1.3,.02);
        }
        if(tick==BANKAI) {
            audience.hide(cleaver);cleaver=null;
            cocoon=model(SummonAssets.BANKAI_COCOON,origin,.2f,"cocoon");loop(cocoon,"swirl");
            wave=model(SummonAssets.BANKAI_WAVE,origin.clone().add(0,.04,0),.5f,"pressure_wave");
            flash(at(0,0,1));flash(at(0,4,1.5));flash(at(0,8,1.5));
            trace("whiteout and dust begin tick="+tick);
            for(Player p:heard) p.stopSound(Sound.ENTITY_WARDEN_SONIC_CHARGE,SoundCategory.PLAYERS);
            sound(origin,Sound.ENTITY_WARDEN_SONIC_BOOM,1.4f,.6f);sound(origin,Sound.ENTITY_BREEZE_WIND_BURST,1.2f,.6f);
        }
        if(tick>=BANKAI && tick<REVEAL) {
            if(tick%4==0) obscure();
            if(tick<BANKAI+20 && tick%8==0) {flash(at(0,0,1.2));flash(at(0,5,1.5));}
        }
        if(cocoon!=null && tick<REVEAL) {
            cocoon.scaler(ModelScaler.value((float)Math.max(.02,BankaiTimeline.cocoonScale(tick))));
            if(tick%4==0) {particle(at(0,0,2),Particle.GUST,4,.8,.02);particle(at(0,0,.2),Particle.CLOUD,8,1.2,.04);}
        }
        if(wave!=null) wave.scaler(ModelScaler.value((float)(.5+5.5*Math.clamp((tick-BANKAI)/(double)(REVEAL-BANKAI),0,1))));
        if(tick==REVEAL) {
            audience.hide(cocoon);cocoon=null;
            tensa=model(SummonAssets.TENSA,palm,.6f,"tensa_zangetsu");tensa.animate("reveal",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
            coat=model(SummonAssets.BANKAI_COAT,origin,1,"black_coat");loop(coat,"flutter");
            trace("dust clears; Tensa revealed tick="+tick);sound(palm,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.1f,1.3f);
        }
        if(tensa!=null) tensa.location(BukkitAdapter.adapt(palm));
        if(tick==READY) {audience.hide(wave);wave=null;}
        if(tick>=REVEAL && tick%8==0) dust(at(0,0,.3),BLACK,3,.45);
    }
    private void obscure() {
        double density=tick<COMPRESS?1:Math.clamp((REVEAL-tick)/(double)(REVEAL-COMPRESS),0,1);
        for(double[] p:new double[][]{{0,0},{-3,3},{3,3},{-4,6},{4,6},{0,9}}) {
            Location center=at(p[0],p[1],1.4);
            particle(center,Particle.CLOUD,(int)Math.ceil(45*density),2.3,.045);
            particle(center,Particle.DUST_PLUME,(int)Math.ceil(35*density),2.5,.025);
            dust(center,WHITE,(int)Math.ceil(16*density),2.1);
            for(Player viewer:audience.players()) viewer.spawnParticle(Particle.BLOCK,center,(int)Math.ceil(70*density),2.4,1.6,2.4,.06,sand);
        }
    }
    private void flash(Location at) {for(Player p:audience.players())p.spawnParticle(Particle.FLASH,at,1,0,0,0,0,Color.WHITE);}
    @Override void onClose() {for(Player p:heard)p.stopSound(Sound.ENTITY_WARDEN_SONIC_CHARGE,SoundCategory.PLAYERS);}
}
