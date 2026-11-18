package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;
import static dev.aserver.motionrp.core.TreasuryTimeline.*;

/** Bounded 100/200 portal performances, with packet-only impacts and Ea. */
final class TreasuryScene extends SummonScene {
    private static final String[] WEAPONS={SummonAssets.SWORD,SummonAssets.SPEAR,SummonAssets.AXE};
    private static final Particle.DustOptions GOLD=new Particle.DustOptions(Color.fromRGB(255,211,75),.9f);
    private static final Particle.DustOptions RED=new Particle.DustOptions(Color.fromRGB(240,20,45),1.5f);
    private final boolean serious;
    private final EntityTracker body;
    private final DummyTracker[] gates,weapons;
    private final int[] loadedAt;
    private final Location focus;
    private final List<Shot> shots=new ArrayList<>();
    private final DummyTracker[] beams=new DummyTracker[16];
    private DummyTracker key,vault,ea;
    private Location beamStart;
    private final Set<Player> chargeListeners=new HashSet<>();
    private record Shot(int index,int fired,DummyTracker model,Location start,Location target,Location previous) {}
    TreasuryScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean serious) {
        super(plugin,owner,serious?"TreasuryEnuma":"Treasury100",end(serious));this.serious=serious;this.body=body;
        // Keep body, baked palm and held props aligned while the camera remains free.
        body.rotation(()->new ModelRotation(0,origin.getYaw()));
        gates=new DummyTracker[count(serious)];weapons=new DummyTracker[count(serious)];loadedAt=new int[count(serious)];
        Location aim=at(0,20,0);
        if(serious) {
            LivingEntity nearest=null;double distance=Double.MAX_VALUE;
            for(Entity entity:origin.getWorld().getNearbyEntities(aim.clone().add(0,1.5,0),4,4,4)) {
                if(!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || entity==owner || living.isDead()
                        || (entity instanceof Player player && !owner.canSee(player)) || !owner.hasLineOfSight(entity)) continue;
                double d=entity.getLocation().distanceSquared(aim);if(d<distance) { distance=d;nearest=living; }
            }
            if(nearest!=null) aim=nearest.getLocation().clone();
        }
        Location floor=ground(aim);focus=floor==null?aim:floor;
    }
    @Override String phase() { return TreasuryTimeline.phase(tick,serious); }
    @Override void update() {
        body.update(TrackerUpdateAction.togglePart(false),bone->Set.of("pri_right_item","pli_left_item").contains(bone.name().rawName()));
        if(tick==0) trace("target x="+focus.getX()+" y="+focus.getY()+" z="+focus.getZ());
        if(tick==OPEN) sound(at(0,-3,4),Sound.BLOCK_ENCHANTMENT_TABLE_USE,1,.65f);
        if((tick>=RAPID && tick<RAPID+RAPID_TICKS && tick%3==0) || tick==VOLLEY)
            sound(at(0,-2,4),Sound.ITEM_TRIDENT_THROW,tick==VOLLEY?1.5f:.75f,tick==VOLLEY?.65f:1.4f);
        for(int i=0;i<gates.length;i++) {
            var pose=portal(i,serious);Location position=portalLocation(pose);
            if(tick==openAt(i,serious)) {
                trace("position index="+i+" x="+position.getX()+" y="+position.getY()+" z="+position.getZ());
                gates[i]=model(SummonAssets.MASS_GATE,position,.03f,"portal"+i);loop(gates[i],"spin");point(gates[i],position);
            }
            if(gates[i]!=null) {
                float scale=(float)Math.clamp((tick-openAt(i,serious))/8.0,.02,1)* (float)pose.scale();
                int close=serious?126:116;
                if(tick>=102) scale*=Math.max(.02f,(close-tick)/(float)(close-102));
                gates[i].scaler(ModelScaler.value(scale));
                if(tick>=close) { audience.hide(gates[i]);gates[i]=null; }
            }
            if(tick==openAt(i,serious)+8 || tick==rapidAt(i,serious)+1) {
                weapons[i]=model(WEAPONS[(i+(tick>DRAW?1:0))%3],position,.8f,"weapon"+i+(tick>DRAW?"_reload":""));
                loadedAt[i]=tick;point(weapons[i],position);
            }
            if(weapons[i]!=null) {
                Vector outward=focus.clone().add(0,1,0).toVector().subtract(position.toVector()).normalize();
                Location loaded=position.clone().add(outward.clone().multiply(.5*Math.clamp((tick-loadedAt[i])/6.0,0,1)));
                loaded.setDirection(outward);weapons[i].location(BukkitAdapter.adapt(loaded));point(weapons[i],loaded);
                if(tick==rapidAt(i,serious) || tick==VOLLEY) {
                    Location target=focus.clone().add(pose.surroundsTarget()?Math.sin(i)*.8:(i%9-4)*.65,pose.surroundsTarget()?.9:.05,Math.cos(i)*.5);
                    shots.add(new Shot(i,tick,weapons[i],loaded,target,loaded.clone()));weapons[i]=null;
                    trace("fire index="+i+" tick="+tick);
                }
            }
        }
        var iterator=shots.iterator();
        while(iterator.hasNext()) {
            var shot=iterator.next();double p=Math.clamp((tick-shot.fired())/(double)FLIGHT,0,1);
            Location next=shot.start().clone().add(shot.target().toVector().subtract(shot.start().toVector()).multiply(p));
            Vector segment=next.toVector().subtract(shot.previous().toVector());
            var hit=segment.lengthSquared()<1e-10?null:next.getWorld().rayTraceBlocks(shot.previous(),segment.clone().normalize(),segment.length(),FluidCollisionMode.NEVER,true);
            if(hit!=null) next=hit.getHitPosition().toLocation(next.getWorld());
            next.setDirection(shot.target().toVector().subtract(shot.start().toVector()));shot.model().location(BukkitAdapter.adapt(next));point(shot.model(),next);
            shot.previous().setX(next.getX());shot.previous().setY(next.getY());shot.previous().setZ(next.getZ());
            if(hit!=null || p>=1) {
                audience.hide(shot.model());iterator.remove();
                if(shot.index()%4==0) { dust(next,GOLD,3,.12);particle(next,Particle.ELECTRIC_SPARK,3,.15,.04); }
                trace("impact index="+shot.index()+" tick="+tick);
            }
        }
        if(tick==VOLLEY+FLIGHT) { blast(focus,serious?7:6);trace("volley blast tick="+tick); }
        if(serious) eaPerformance();
    }
    private Location portalLocation(Portal pose) {
        Location location;
        if(pose.surroundsTarget()) {
            Location offset=at(pose.right(),pose.forward(),pose.height());
            location=focus.clone().add(offset.toVector().subtract(origin.toVector()));
        } else location=at(pose.right(),pose.forward(),pose.height());
        location.setDirection(focus.clone().add(0,1.2,0).toVector().subtract(location.toVector()));return location;
    }
    private static void point(DummyTracker tracker,Location location) {
        float pitch=location.getPitch(),yaw=location.getYaw();
        tracker.rotator(ModelRotator.DEFAULT);tracker.rotation(()->new ModelRotation(pitch,yaw));
    }
    private Location palm() { var p=TreasuryHandPath.at(tick);return at(p.right(),p.forward(),p.height()); }
    private void eaPerformance() {
        Location hand=palm();
        if(tick==KEY) { key=model(SummonAssets.BAB_ILU,hand,.8f,"bab_ilu");sound(hand,Sound.BLOCK_END_PORTAL_FRAME_FILL,.8f,.7f); }
        if(key!=null) { Location turned=hand.clone();turned.setYaw(origin.getYaw()+(tick-KEY)*9);key.location(BukkitAdapter.adapt(turned)); }
        if(tick==EA-8) { vault=model(SummonAssets.MASS_GATE,hand.clone().add(0,.2,0),.7f,"hand_vault");loop(vault,"spin"); }
        if(vault!=null) vault.location(BukkitAdapter.adapt(hand.clone().add(0,.2,0)));
        if(tick==EA) {
            audience.hide(key);key=null;ea=model(SummonAssets.EA,hand,.85f,"ea");loop(ea,"spin");
            ea.animate("level",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(hand,Sound.BLOCK_BEACON_POWER_SELECT,.8f,.55f);
        }
        if(tick==EA+8) { audience.hide(vault);vault=null; }
        if(ea!=null) ea.location(BukkitAdapter.adapt(hand));
        if(tick>=CHARGE && tick<FIRE) {
            if((tick-CHARGE)%12==0) {
                chargeListeners.addAll(audience.players());sound(hand,Sound.ENTITY_BREEZE_CHARGE,.8f,(float)(.6+(tick-CHARGE)*.008));
            }
            if(tick%2==0) {
                double radius=.5+(tick-CHARGE)*.014;
                for(int j=0;j<3;j++) {
                    double a=tick*.4+j*Math.PI*2/3;
                    dust(hand.clone().add(Math.cos(a)*radius,.7+Math.sin(a*.5)*.5,Math.sin(a)*radius),RED,4,.06);
                }
                particle(hand.clone().add(0,1,0),Particle.SMALL_GUST,2,.5,.02);
            }
        }
        if(tick==FIRE) {
            beamStart=hand.clone().add(origin.getDirection().multiply(1.6));
            sound(hand,Sound.ENTITY_WARDEN_SONIC_BOOM,2,.55f);sound(hand,Sound.ENTITY_BREEZE_SHOOT,2,.5f);
            for(Player player:audience.players()) player.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);
        }
        if(tick>=FIRE && tick<RECOVER) {
            Location target=focus.clone().add(0,1,0);Vector line=target.toVector().subtract(beamStart.toVector());
            int visible=(int)Math.ceil(16*Math.clamp((tick-FIRE)/(double)(IMPACT-FIRE),0,1));
            for(int i=0;i<visible;i++) {
                Location center=beamStart.clone().add(line.clone().multiply((i+.5)/16));center.setDirection(line);
                if(beams[i]==null) { beams[i]=model(SummonAssets.ENUMA,center,1.8f,"beam"+i);loop(beams[i],"vortex");point(beams[i],center); }
                if(tick%3==0) {
                    double a=tick*.4+i;
                    dust(center.clone().add(Math.cos(a)*.7,Math.sin(a)*.7,0),RED,3,.1);
                    particle(center,Particle.GUST,1,.35,.02);
                }
            }
        }
        if(tick==IMPACT) { blast(focus,9);trace("enuma impact tick="+tick); }
        if(tick==RECOVER) {
            for(int i=0;i<beams.length;i++) { audience.hide(beams[i]);beams[i]=null; }
            audience.hide(ea);ea=null;
        }
    }
    private void blast(Location center,double radius) {
        for(int j=0;j<12;j++) {
            double angle=Math.PI*2*j/12;
            particle(center.clone().add(Math.cos(angle)*radius*.6,1,Math.sin(angle)*radius*.6),Particle.EXPLOSION_EMITTER,1,0,0);
        }
        particle(center,Particle.DUST_PLUME,40,radius*.55,.07);particle(center.clone().add(0,1,0),Particle.CLOUD,65,radius*.5,.08);
        for(Player player:audience.players()) player.spawnParticle(Particle.BLOCK,center,110,radius*.5,.7,radius*.5,.08,Material.SAND.createBlockData());
        sound(center,Sound.ENTITY_GENERIC_EXPLODE,3,.45f);sound(center,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,2,.6f);
        if(radius>=9) sound(center,Sound.ENTITY_WARDEN_SONIC_BOOM,2,.6f);
    }
    @Override void onClose() {
        if(serious) for(Player player:chargeListeners) player.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);
    }
}
