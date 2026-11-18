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
    private final int[] loadedAt=new int[COUNT],reloadAt=new int[COUNT],loads=new int[COUNT];
    private final java.util.List<Shot> shots=new java.util.ArrayList<>();
    private boolean volleyBoom;
    private record Shot(int gate,int fired,DummyTracker model,Location start,Location target,Location previous) {}
    GateOfBabylonScene(MotionPlugin plugin,Player owner) { super(plugin,owner,"Gate",END);java.util.Arrays.fill(reloadAt,-1); }
    @Override String phase() { return GateTimeline.phase(tick); }
    @Override void update() {
        if(tick==OPEN) sound(at(0,-1.5,2.5),Sound.BLOCK_ENCHANTMENT_TABLE_USE,.7f,.85f);
        if(tick==VOLLEY) sound(origin,Sound.ENTITY_ENDER_DRAGON_FLAP,.7f,1.4f);
        for(int i=0;i<COUNT;i++) {
            var portal=portal(i);
            double right=portal.right(),up=portal.up(),back=portal.forward();
            Location gate=at(right,back,up);int age=tick-openAt(i);
            if(age==0) {
                gates[i]=model(SummonAssets.GATE,gate,.05f,"portal"+i);
                gates[i].animate("spin",kr.toxicity.model.api.animation.AnimationModifier.builder()
                        .type(kr.toxicity.model.api.animation.AnimationIterator.Type.LOOP).speed(1.5f).build());
            }
            if(gates[i]!=null) {
                float scale=(float)Math.max(.02,opening(tick,i)*portal.scale());
                if(tick>=FADE) scale*=Math.max(.02f,(END-tick)/(float)(END-FADE));
                gates[i].scaler(ModelScaler.value(scale));
                if(tick%4==0) {
                    double a=tick*.3+i;
                    dust(at(right+Math.cos(a)*.6*portal.scale(),back,up+Math.sin(a)*.6*portal.scale()),GOLD,2,.02);
                }
            }
            if(age==OPEN_TICKS || tick==reloadAt[i]) {
                loadedAt[i]=tick;
                weapons[i]=model(WEAPONS[(i+loads[i])%3],at(right,back-.25,up),.8f,
                        "weapon"+i+(loads[i]==0 ? "" : "_reload"+loads[i]));
                if(loads[i]>0) trace("reload index="+i+" tick="+tick);
                loads[i]++;
            }
            if(weapons[i]==null) continue;
            double emerging=Math.clamp((tick-loadedAt[i])/(double)EMERGE_TICKS,0,1);
            weapons[i].location(BukkitAdapter.adapt(at(right,back-.25+.5*emerging,up)));
            if(shoots(tick,i)) {
                double spread=(i%5-2)*2.1+(i/5-1.5)*.15;
                Location start=at(right,back+.25,up),target=ground(at(spread,20+(i%3)*.6,0));
                if(target==null) target=at(spread,20+(i%3)*.6,.2);
                shots.add(new Shot(i,tick,weapons[i],start,target,start.clone()));weapons[i]=null;
                if(tick<VOLLEY) reloadAt[i]=tick+1;
                sound(start,Sound.ITEM_TRIDENT_THROW,.7f,1.15f+(i%3)*.15f);trace("fire index="+i+" tick="+tick);
            }
        }
        var iterator=shots.iterator();
        while(iterator.hasNext()) {
            Shot shot=iterator.next();double progress=Math.clamp((tick-shot.fired())/(double)FLIGHT_TICKS,0,1);
            Location next=shot.start().clone().add(shot.target().toVector().subtract(shot.start().toVector()).multiply(progress));
            Vector segment=next.toVector().subtract(shot.previous().toVector());
            var hit=segment.lengthSquared()<1e-10 ? null : next.getWorld().rayTraceBlocks(shot.previous(),
                    segment.clone().normalize(),segment.length(),FluidCollisionMode.NEVER,true);
            if(hit!=null) next=hit.getHitPosition().toLocation(next.getWorld());
            next.setDirection(shot.target().toVector().subtract(shot.start().toVector()));
            shot.model().location(BukkitAdapter.adapt(next));
            shot.previous().setX(next.getX());shot.previous().setY(next.getY());shot.previous().setZ(next.getZ());
            if(tick%2==0) dust(next,GOLD,3,.04);
            if(hit!=null || progress>=1) {
                audience.hide(shot.model());iterator.remove();particle(next,Particle.ELECTRIC_SPARK,12,.14,.03);
                projectileImpact(next,shot.fired()==VOLLEY);
                trace("explosion index="+shot.gate()+" tick="+tick);
                if(shot.fired()==VOLLEY) {
                    dust(next,GOLD,14,.4);
                    if(shot.gate()%4==0) particle(next,Particle.EXPLOSION_EMITTER,1,0,0);
                    if(!volleyBoom) {
                        volleyBoom=true;sound(next,Sound.ENTITY_GENERIC_EXPLODE,2,.55f);
                        sound(next,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,1.2f,.8f);
                        sound(next,Sound.ENTITY_BREEZE_WIND_BURST,1,.9f);
                    }
                }
                trace("impact index="+shot.gate()+" tick="+tick+" forward="+forwardDistance(next));
            }
        }
    }
    private double forwardDistance(Location point) {
        double yaw=Math.toRadians(origin.getYaw());
        return -(point.getX()-origin.getX())*Math.sin(yaw)+(point.getZ()-origin.getZ())*Math.cos(yaw);
    }
}
