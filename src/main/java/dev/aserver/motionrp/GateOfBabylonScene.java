package dev.aserver.motionrp;

import dev.aserver.motionrp.core.GateTimeline;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.GateTimeline.*;

final class GateOfBabylonScene extends SummonScene {
    private static final Particle.DustOptions GOLD=new Particle.DustOptions(Color.fromRGB(255,211,75),.8f);
    private static final String[] WEAPONS={SummonAssets.SWORD,SummonAssets.SPEAR,SummonAssets.AXE};
    private final DummyTracker[] gates=new DummyTracker[COUNT], weapons=new DummyTracker[COUNT];
    private final Location[] starts=new Location[COUNT], targets=new Location[COUNT], previous=new Location[COUNT];
    private final boolean[] done=new boolean[COUNT];
    GateOfBabylonScene(MotionPlugin plugin,Player owner) { super(plugin,owner,"Gate",END); }
    @Override String phase() { return GateTimeline.phase(tick); }
    @Override void update() {
        if(tick==OPEN) sound(at(0,-1.5,2.5),Sound.BLOCK_ENCHANTMENT_TABLE_USE,.7f,.85f);
        if(tick==VOLLEY) sound(origin,Sound.ENTITY_ENDER_DRAGON_FLAP,.7f,1.4f);
        for(int i=0;i<COUNT;i++) {
            double right=(i%5-2)*1.3, up=i<5 ? 2.35 : 4.0;
            Location gate=at(right,-1.6,up);
            int age=tick-openAt(i);
            if(age==0) {
                gates[i]=model(SummonAssets.GATE,gate,.05f,"portal"+i);loop(gates[i],"spin");
            }
            if(gates[i]!=null) {
                float scale=(float)Math.max(.02,opening(tick,i));
                if(tick>=FADE) scale*=Math.max(.02f,(END-tick)/(float)(END-FADE));
                gates[i].scaler(ModelScaler.value(scale));
                if(tick%4==0) {
                    double a=tick*.3+i;
                    dust(at(right+Math.cos(a)*.6,-1.6,up+Math.sin(a)*.6),GOLD,2,.02);
                }
            }
            if(age==12) {
                starts[i]=at(right,-1.35,up);previous[i]=starts[i].clone();
                weapons[i]=model(WEAPONS[i%3],at(right,-1.85,up),.8f,"weapon"+i);
            }
            if(weapons[i]==null || done[i]) continue;
            if(tick<fireAt(i)) {
                double emerging=Math.clamp((age-12)/10.0,0,1);
                weapons[i].location(BukkitAdapter.adapt(at(right,-1.85+.5*emerging,up)));
            } else {
                if(tick==fireAt(i)) {
                    targets[i]=ground(at((i%5-2)*.65,9+(i%3)*.6,0));
                    if(targets[i]==null) targets[i]=at((i%5-2)*.65,9+(i%3)*.6,.2);
                    sound(starts[i],Sound.ITEM_TRIDENT_THROW,.7f,1.15f+(i%3)*.15f);
                    trace("fire index="+i+" tick="+tick);
                }
                double progress=Math.clamp((tick-fireAt(i))/18.0,0,1);
                Location next=starts[i].clone().add(targets[i].toVector().subtract(starts[i].toVector()).multiply(progress));
                Vector segment=next.toVector().subtract(previous[i].toVector());
                var hit=segment.lengthSquared()<1e-10 ? null : next.getWorld().rayTraceBlocks(previous[i],
                        segment.clone().normalize(),segment.length(),FluidCollisionMode.NEVER,true);
                if(hit!=null) next=hit.getHitPosition().toLocation(next.getWorld());
                Vector direction=targets[i].toVector().subtract(starts[i].toVector());next.setDirection(direction);
                weapons[i].location(BukkitAdapter.adapt(next));previous[i]=next.clone();
                if(tick%2==0) dust(next,GOLD,3,.04);
                if(hit!=null || progress>=1) {
                    done[i]=true;audience.hide(weapons[i]);weapons[i]=null;
                    particle(next,Particle.ELECTRIC_SPARK,12,.14,.03);
                    sound(next,Sound.ENTITY_ARROW_HIT,.65f,1.3f);
                    trace("impact index="+i+" tick="+tick);
                }
            }
        }
    }
}
