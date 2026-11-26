package dev.aserver.motionrp;

import dev.aserver.motionrp.core.SusanooTimeline;
import dev.aserver.motionrp.core.SusanooPilotPath;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import static dev.aserver.motionrp.core.SusanooTimeline.*;

final class SusanooScene extends SummonScene {
    private static final Particle.DustOptions VIOLET=new Particle.DustOptions(Color.fromRGB(84,193,255),1.5f);
    private DummyTracker ribs,armor,slash;
    private final AerialAnchor pilot;private static final float SCALE=10;
    SusanooScene(MotionPlugin plugin,Player owner) { super(plugin,owner,"Susanoo",END,128,true);pilot=new AerialAnchor(plugin,owner); }
    @Override public boolean ownsMove(Location from,Location to) {return pilot.owns(from,to);}
    @Override void onClose() {pilot.close();}
    @Override String phase() { return SusanooTimeline.phase(tick); }
    @Override void update() {
        if(tick==RIBS) {
            ribs=model(SummonAssets.RIBS,origin,.1f,"ribs");
            sound(origin,Sound.ENTITY_WITHER_SPAWN,.45f,.8f);
        }
        if(ribs!=null) ribs.scaler(ModelScaler.value((float)(.1+9.9*Math.clamp((tick-RIBS)/18.0,0,1))));
        if(tick==ARMOR) {
            armor=model(SummonAssets.ARMOR,origin,.1f,"armor");
            armor.update(TrackerUpdateAction.viewRange(3));
            armor.animate("unfurl",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
            sound(at(0,0,3),Sound.ENTITY_ENDER_DRAGON_GROWL,.45f,.8f);
        }
        if(armor!=null) {
            float scale=(float)(.1+9.9*Math.clamp((tick-ARMOR)/44.0,0,1));
            if(tick>=FADE) scale*=Math.max(.02f,(END-tick)/(float)(END-FADE));
            armor.scaler(ModelScaler.value(scale));
        }
        if(tick==ARMOR+44) { audience.hide(ribs);ribs=null;trace("complete armor and wings formed tick="+tick);
            var seat=SusanooPilotPath.at(tick);if(!pilot.moveTo(at(seat.right(),seat.forward(),seat.height()),origin)) {plugin.playback().stop(owner.getUniqueId());return;}
            trace("pilot seated scale="+SCALE+" y="+owner.getY()); }
        if(tick>ARMOR+44 && tick<FADE){var seat=SusanooPilotPath.at(tick);Location target=at(seat.right(),seat.forward(),seat.height());target.setYaw(owner.getYaw());target.setPitch(owner.getPitch());if(!pilot.moveTo(target,origin)){plugin.playback().stop(owner.getUniqueId());return;}}
        if(tick==FADE) {pilot.close();trace("pilot returned before fade tick="+tick);}
        if(tick==READY+104||tick==END-4){var blade=armor.bone("waist_blade_-1").worldPosition();trace("blade held tick="+tick+" height="+blade.y); }
        if(tick==READY) armor.animate("slash",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        if(tick==SLASH || tick==RIGHT_SLASH) {
            slash=model(SummonAssets.SLASH,at(0,20,35),11f,tick==SLASH?"slash_left":"slash_right");
            slash.update(TrackerUpdateAction.viewRange(3));
            slash.animate(tick==SLASH?"sweep":"kesa",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
            sound(at(0,2,3),Sound.ENTITY_IRON_GOLEM_ATTACK,4,.6f);
            sound(at(0,2,3),Sound.ENTITY_PLAYER_ATTACK_SWEEP,4,.7f);
        }
        if(slash!=null) {
            int begin=tick<RIGHT_SLASH?SLASH:RIGHT_SLASH,end=tick<RIGHT_SLASH?LEFT_IMPACT:IMPACT;
            double progress=Math.clamp((tick-begin)/(double)(end-begin),0,1);
            slash.location(BukkitAdapter.adapt(at(0,20+50*progress,35-27.5*progress)));
            slash.scaler(ModelScaler.value((float)(11*(1-.6*progress))));
            if(tick==LEFT_IMPACT || tick==IMPACT) { audience.hide(slash);slash=null; }
        }
        if(tick>=RIBS && tick<FADE && tick%3==0) {
            for(int strand=0;strand<4;strand++) {
                double a=tick*.22+strand*Math.PI/2;
                dust(at(Math.cos(a)*1.4,Math.sin(a)*.8,1+(tick%24)/8.0),VIOLET,2,.04);
            }
        }
        if(tick==READY)trace("draw left waist scabbard; both hands on blade and sheath");
        if(tick==READY+23)trace("blade clears scabbard; draw cut begins");
        for(int strike:new int[]{LEFT_IMPACT,IMPACT}){
            int age=tick-strike;
            if(age>=0 && age<=16 && age%2==0) groundBlast(age,strike==LEFT_IMPACT);
        }
        if(tick==LEFT_IMPACT || tick==IMPACT)trace("impact tick="+tick);

    }
    private void groundBlast(int age,boolean draw){
        double distance=10+age*4;
        for(int j=-4;j<=4;j++){
            double side=j*(2+age*.22);Location point=ground(at(side,distance+Math.abs(j)*1.1,0));if(point==null)continue;
            particle(point,Particle.EXPLOSION_EMITTER,1,.5,0);particle(point,Particle.DUST_PLUME,12,1.7,.13);
            particle(point,Particle.CLOUD,16,2,.22);particle(point,Particle.GUST,2,1,.1);dust(point,VIOLET,4,1);
            if(j%2==0)for(Player viewer:audience.players())viewer.spawnParticle(Particle.BLOCK,point,10,1,1,1,.16,point.clone().subtract(0,.1,0).getBlock().getBlockData());
        }
        if(age==0){sound(at(0,18,0),Sound.ENTITY_GENERIC_EXPLODE,6,.5f);sound(at(0,18,0),Sound.ENTITY_BREEZE_WIND_BURST,6,.5f);trace((draw?"draw-cut":"kesa")+" ground blast and wind start");}
    }
}
