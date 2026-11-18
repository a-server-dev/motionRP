package dev.aserver.motionrp;

import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.TreasuryTimeline.*;

/** White-gold charge, red/black gyroscopic rings, expanding vortex and ground tears. */
final class EnumaEffects {
    private static final Particle.DustOptions RED=new Particle.DustOptions(Color.fromRGB(255,24,48),1.7f);
    private static final Particle.DustOptions GOLD=new Particle.DustOptions(Color.fromRGB(255,239,177),1.2f);
    private final TreasuryScene scene;
    private final Location focus;
    private DummyTracker core,storm;
    private final DummyTracker[] halos=new DummyTracker[2],shocks=new DummyTracker[3],beams=new DummyTracker[16],rifts=new DummyTracker[8];
    private Location start;
    EnumaEffects(TreasuryScene scene,Location focus) { this.scene=scene;this.focus=focus.clone(); }
    void update(int tick,Location palm) {
        double tilt=thrust(tick)*Math.PI/2;
        Location tip=palm.clone().add(scene.origin.getDirection().multiply(1.63*Math.sin(tilt))).add(0,1.63*Math.cos(tilt),0);
        Vector axis=scene.origin.getDirection().multiply(Math.sin(tilt)).add(new Vector(0,Math.cos(tilt),0));
        tip.setDirection(axis);
        if(tick==CHARGE) {
            core=scene.model(SummonAssets.ENUMA_CHARGE,tip,.15f,"charge_core");scene.loop(core,"spin");
            for(int i=0;i<halos.length;i++) { halos[i]=scene.model(SummonAssets.ENUMA_RING,tip,.4f,"charge_ring"+i);scene.loop(halos[i],"spin"); }
        }
        if(tick>=CHARGE && tick<FIRE) {
            double p=charge(tick);core.location(kr.toxicity.model.api.bukkit.platform.BukkitAdapter.adapt(tip));core.scaler(ModelScaler.value((float)(.15+1.1*p)));
            for(int i=0;i<halos.length;i++) {
                Location at=tip.clone();at.setPitch(at.getPitch()+i*25);at.setYaw(at.getYaw()+i*tick*2);
                halos[i].location(kr.toxicity.model.api.bukkit.platform.BukkitAdapter.adapt(at));TreasuryScene.point(halos[i],at);
                halos[i].scaler(ModelScaler.value((float)((.4+2.0*p)*(1+i*.18))));
            }
            if(tick%2==0) {
                for(int i=0;i<6;i++) {
                    double angle=tick*.32+i*Math.PI/3,radius=.35+1.3*p;
                    scene.dust(tip.clone().add(Math.cos(angle)*radius,Math.sin(angle)*radius,0),i%2==0?RED:GOLD,3,.05);
                }
                scene.particle(tip,Particle.END_ROD,3,.45,.04);scene.particle(palm,Particle.SMALL_GUST,2,.35,.02);
            }
        }
        if(tick==FIRE) {
            start=tip.clone();scene.audience.hide(core);core=null;
            for(int i=0;i<halos.length;i++) { scene.audience.hide(halos[i]);halos[i]=null; }
            flash(start);
        }
        if(tick>=FIRE && tick<RECOVER) {
            Location target=focus.clone().add(0,1,0);Vector line=target.toVector().subtract(start.toVector());
            double progress=Math.clamp((tick-FIRE)/(double)(IMPACT-FIRE),0,1);
            int visible=(int)Math.ceil(beams.length*progress);
            for(int i=0;i<visible;i++) {
                Location center=start.clone().add(line.clone().multiply((i+.5)/beams.length));center.setDirection(line);
                if(beams[i]==null) { beams[i]=scene.model(SummonAssets.ENUMA,center,(float)beamScale(i),"beam"+i);scene.loop(beams[i],"vortex");TreasuryScene.point(beams[i],center); }
                if(tick%3==0) {
                    double a=tick*.48+i,r=beamScale(i)*.35;
                    Vector side=new Vector(-line.getZ(),0,line.getX()).normalize();
                    scene.dust(center.clone().add(side.multiply(Math.cos(a)*r)).add(0,Math.sin(a)*r,0),RED,3,.09);
                    scene.particle(center,Particle.GUST,1,.3,.03);
                }
            }
            for(int i=0;i<shocks.length;i++) {
                int age=tick-FIRE-i*3;
                if(age>=0 && age<=IMPACT-FIRE) {
                    double p=age/(double)(IMPACT-FIRE);Location at=start.clone().add(line.clone().multiply(p));at.setDirection(line);
                    if(shocks[i]==null) { shocks[i]=scene.model(SummonAssets.ENUMA_RING,at,1,"shock_ring"+i);scene.loop(shocks[i],"spin"); }
                    shocks[i].location(kr.toxicity.model.api.bukkit.platform.BukkitAdapter.adapt(at));TreasuryScene.point(shocks[i],at);
                    shocks[i].scaler(ModelScaler.value((float)(.7+3.5*p)));
                } else if(shocks[i]!=null) { scene.audience.hide(shocks[i]);shocks[i]=null; }
            }
            for(int i=0;i<rifts.length;i++) if(rifts[i]==null && tick-FIRE>=i*2) {
                Location aim=scene.origin.clone().add(focus.toVector().subtract(scene.origin.toVector()).multiply((i+.5)/rifts.length));
                Location ground=scene.ground(aim);
                if(ground!=null) {
                    ground.subtract(0,.10,0);ground.setYaw(scene.origin.getYaw());
                    rifts[i]=scene.model(SummonAssets.ENUMA_RIFT,ground,1.3f,"rift"+i);
                    scene.particle(ground,Particle.DUST_PLUME,4,.3,.02);
                }
            }
        }
        if(tick==IMPACT) { storm=scene.model(SummonAssets.ENUMA_CHARGE,focus.clone().add(0,2,0),2,"impact_core");scene.loop(storm,"spin");flash(focus.clone().add(0,2,0)); }
        if(storm!=null) {
            double p=Math.clamp((tick-IMPACT)/(double)(RECOVER-IMPACT),0,1);
            storm.scaler(ModelScaler.value((float)(2+3*Math.sin(p*Math.PI))));
            if(tick%3==0 && tick<RECOVER) {
                for(int i=0;i<6;i++) { double a=tick*.25+i*Math.PI/3;scene.dust(focus.clone().add(Math.cos(a)*4,1.5+Math.sin(a*2),Math.sin(a)*4),RED,4,.3); }
                scene.particle(focus,Particle.DUST_PLUME,4,3,.05);
            }
        }
        if(tick==RECOVER) {
            for(int i=0;i<beams.length;i++) { scene.audience.hide(beams[i]);beams[i]=null; }
            for(int i=0;i<shocks.length;i++) { scene.audience.hide(shocks[i]);shocks[i]=null; }
            scene.audience.hide(storm);storm=null;
        }
        if(tick>=RECOVER) for(var rift:rifts) if(rift!=null) rift.scaler(ModelScaler.value((float)Math.max(.02,1.3*(SERIOUS_END-tick)/(double)(SERIOUS_END-RECOVER))));
    }
    private void flash(Location at) { for(Player p:scene.audience.players()) p.spawnParticle(Particle.FLASH,at,1,0,0,0,0,Color.fromRGB(255,240,210)); }
}
