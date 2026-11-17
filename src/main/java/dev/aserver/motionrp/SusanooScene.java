package dev.aserver.motionrp;

import dev.aserver.motionrp.core.SusanooTimeline;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import static dev.aserver.motionrp.core.SusanooTimeline.*;

final class SusanooScene extends SummonScene {
    private static final Particle.DustOptions VIOLET=new Particle.DustOptions(Color.fromRGB(163,95,255),1.1f);
    private DummyTracker ribs,armor,slash;
    SusanooScene(MotionPlugin plugin,Player owner) { super(plugin,owner,"Susanoo",END); }
    @Override String phase() { return SusanooTimeline.phase(tick); }
    @Override void update() {
        if(tick==RIBS) {
            ribs=model(SummonAssets.RIBS,origin,.1f,"ribs");
            sound(origin,Sound.ENTITY_WITHER_SPAWN,.45f,.8f);
        }
        if(ribs!=null) ribs.scaler(ModelScaler.value((float)(.1+1.9*Math.clamp((tick-RIBS)/18.0,0,1))));
        if(tick==ARMOR) {
            armor=model(SummonAssets.ARMOR,origin,.1f,"armor");
            sound(at(0,0,3),Sound.ENTITY_ENDER_DRAGON_GROWL,.45f,.8f);
        }
        if(armor!=null) {
            float scale=(float)(.1+1.9*Math.clamp((tick-ARMOR)/20.0,0,1));
            if(tick>=FADE) scale*=Math.max(.02f,(END-tick)/(float)(END-FADE));
            armor.scaler(ModelScaler.value(scale));
        }
        if(tick==ARMOR+20) { audience.hide(ribs);ribs=null; }
        if(tick==READY) armor.animate("slash",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        if(tick==SLASH) {
            slash=model(SummonAssets.SLASH,at(0,2.5,2.5),.5f,"slash");
            sound(at(0,2,3),Sound.ENTITY_IRON_GOLEM_ATTACK,1,.6f);
            sound(at(0,2,3),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.1f,.7f);
        }
        if(slash!=null) {
            double progress=Math.clamp((tick-SLASH)/20.0,0,1);
            slash.location(BukkitAdapter.adapt(at(0,2.5+4*progress,2.5-.9*progress)));
            slash.scaler(ModelScaler.value((float)(2*(1-.75*progress))));
            if(tick==SLASH+20) { audience.hide(slash);slash=null; }
        }
        if(tick>=RIBS && tick<FADE && tick%3==0) {
            for(int strand=0;strand<4;strand++) {
                double a=tick*.22+strand*Math.PI/2;
                dust(at(Math.cos(a)*1.4,Math.sin(a)*.8,1+(tick%24)/8.0),VIOLET,2,.04);
            }
        }
        if(tick==IMPACT) {
            Location point=ground(at(0,6,0));if(point==null) point=at(0,6,.2);
            particle(point,Particle.POOF,28,.7,.05);dust(point,VIOLET,32,.8);
            sound(point,Sound.ENTITY_GENERIC_EXPLODE,1,.65f);trace("impact tick="+tick);
        }
    }
}
