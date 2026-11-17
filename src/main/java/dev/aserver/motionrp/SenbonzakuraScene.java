package dev.aserver.motionrp;

import dev.aserver.motionrp.core.SenbonzakuraTimeline;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import static dev.aserver.motionrp.core.SenbonzakuraTimeline.*;

/** Two rows of descending-tip katanas dissolve into bounded pink blade clouds. */
final class SenbonzakuraScene extends SummonScene {
    private static final Particle.DustOptions ROSE=new Particle.DustOptions(Color.fromRGB(255,151,207),.65f);
    private DummyTracker dropped;
    private final DummyTracker[] swords=new DummyTracker[SWORDS],clouds=new DummyTracker[CLOUDS];
    private final Location[] surgeFrom=new Location[CLOUDS];
    SenbonzakuraScene(MotionPlugin plugin,Player owner) { super(plugin,owner,"Senbonzakura",END); }
    @Override String phase() { return SenbonzakuraTimeline.phase(tick); }
    @Override void update() {
        if(tick==DROP) {
            Location hand=at(.3,.65,1.1-45*.42/16);
            dropped=model(SummonAssets.CHERRY_BLADE,hand,.42f,"dropped");
            sound(hand,Sound.BLOCK_CHAIN_PLACE,.6f,1.2f);
        }
        if(dropped!=null) {
            Location fall=at(.3,.65,1.1-45*.42/16-1.6*Math.clamp((tick-DROP)/(double)(RISE-DROP),0,1));
            dropped.location(BukkitAdapter.adapt(fall));
            if(tick==RISE) { audience.hide(dropped);dropped=null; }
        }
        for(int i=0;i<SWORDS;i++) {
            var pose=blade(i);double right=pose.right(),forward=pose.forward();
            if(tick==swordAt(i)) {
                swords[i]=model(SummonAssets.CHERRY_BLADE,at(right,forward,-4.5),1.35f,"blade"+i);
                sound(at(right,forward,0),Sound.BLOCK_CHAIN_PLACE,.3f,1.4f);
            }
            if(swords[i]!=null) {
                double rise=Math.clamp((tick-swordAt(i))/(double)RISE_TICKS,0,1);
                swords[i].location(BukkitAdapter.adapt(at(right,forward,-4.5+4.5*rise)));
                if(tick==BLOOM) {
                    dust(at(right,forward,1.1),ROSE,8,.5);dust(at(right,forward,3.2),ROSE,8,.5);
                    audience.hide(swords[i]);swords[i]=null;
                }
            }
        }
        if(tick==BLOOM) {
            sound(at(0,0,2),Sound.BLOCK_CHERRY_LEAVES_BREAK,.9f,1.1f);
            sound(at(0,0,2),Sound.ENTITY_BREEZE_WHIRL,.55f,1.4f);
            for(int i=0;i<CLOUDS;i++) { clouds[i]=model(SummonAssets.CHERRY_PETALS,orbit(i),.35f,"petals"+i);loop(clouds[i],"swirl"); }
        }
        for(int i=0;i<CLOUDS;i++) {
            if(clouds[i]==null) continue;
            Location point;
            if(tick<SURGE) point=orbit(i);
            else {
                if(tick==SURGE) surgeFrom[i]=orbit(i);
                double p=Math.clamp((tick-SURGE)/(double)(IMPACT-SURGE),0,1);
                Location focus=at(0,7,1.2);
                point=surgeFrom[i].clone().add(focus.toVector().subtract(surgeFrom[i].toVector()).multiply(p));
                if(tick>IMPACT) {
                    double spread=(tick-IMPACT)*.06,angle=i*Math.PI*2/CLOUDS;
                    point.add(Math.cos(angle)*spread,Math.sin(angle*2)*spread*.35,Math.sin(angle)*spread);
                }
            }
            clouds[i].location(BukkitAdapter.adapt(point));
            float scale=(float)Math.clamp((tick-BLOOM)/10.0,.1,1);
            if(tick>=FADE) scale*=Math.max(.02f,(END-tick)/(float)(END-FADE));
            clouds[i].scaler(ModelScaler.value(scale));
            if(tick%3==0) { dust(point,ROSE,3,.3);particle(point,Particle.CHERRY_LEAVES,2,.25,0); }
        }
        if(tick==SURGE) {
            sound(at(0,1,1.5),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.9f,1.3f);
            sound(at(0,1,1.5),Sound.ENTITY_WIND_CHARGE_THROW,.7f,1.2f);
        }
        if(tick==IMPACT) {
            Location focus=at(0,7,1.2);dust(focus,ROSE,45,.7);
            particle(focus,Particle.CHERRY_LEAVES,18,.6,.02);particle(focus,Particle.END_ROD,8,.4,.025);
            sound(focus,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1,.75f);trace("impact tick="+tick);
        }
    }
    private Location orbit(int i) {
        double angle=tick*.085+i*Math.PI*2/CLOUDS;
        return at(Math.cos(angle)*3.5,Math.sin(angle)*3,2+1.1*Math.sin(angle*2+i));
    }
}
