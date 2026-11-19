package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/** Cosmetic Noble Phantasms; no damage, terrain edits, targeting packets or real projectiles. */
final class NobleScene extends SummonScene {
    private final boolean gold;private final EntityTracker body;private final Location focus;
    private final DummyTracker[] beam;private DummyTracker weapon,thorns,pillar;private Location start;
    private final Set<Player> heard=new HashSet<>();
    private final Particle.DustOptions energy;
    NobleScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean excalibur) {
        super(plugin,owner,excalibur?"Excalibur":"GaeBolg",NobleTimeline.end(excalibur));gold=excalibur;this.body=body;
        body.rotation(()->new ModelRotation(0,origin.getYaw()));beam=new DummyTracker[NobleTimeline.segments(gold)];
        energy=new Particle.DustOptions(gold?Color.fromRGB(255,218,72):Color.fromRGB(239,14,51),gold?1.7f:1.3f);
        Location aim=at(0,NobleTimeline.reach(gold),0);
        if(!gold) {
            LivingEntity nearest=null;double best=9;
            for(var e:origin.getWorld().getNearbyLivingEntities(aim,3)) if(e!=owner && !e.isDead() && owner.hasLineOfSight(e)) {
                double d=e.getLocation().distanceSquared(aim);if(d<best) {best=d;nearest=e;}
            }
            if(nearest!=null) aim=nearest.getLocation();
        }
        Location ground=ground(aim);focus=(ground==null?aim:ground).clone().add(0,gold?.5:.9,0);
        Vector direction=focus.toVector().subtract(at(0,0,1.2).toVector());
        var wall=origin.getWorld().rayTraceBlocks(at(0,0,1.2),direction.clone().normalize(),direction.length(),FluidCollisionMode.NEVER,true);
        if(wall!=null) {focus.setX(wall.getHitPosition().getX());focus.setY(wall.getHitPosition().getY());focus.setZ(wall.getHitPosition().getZ());}
    }
    @Override String phase() {return NobleTimeline.phase(tick,gold);}
    private Location palm() {var p=NobleHandPath.at(gold,tick);return at(p.right(),p.forward(),p.height());}
    @Override void update() {
        body.update(TrackerUpdateAction.togglePart(false),bone->Set.of("pri_right_item","pli_left_item").contains(bone.name().rawName()));
        Location hand=palm();
        if(tick==NobleTimeline.draw(gold)) {
            weapon=model(gold?SummonAssets.EXCALIBUR:SummonAssets.GAE_BOLG,hand,gold?.8f:.7f,"weapon");
            weapon.animate(gold?"release":"thrust",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
            sound(hand,Sound.ENTITY_PLAYER_ATTACK_SWEEP,.8f,.8f);
        }
        if(weapon!=null) weapon.location(BukkitAdapter.adapt(hand));
        if(tick>=32 && tick<NobleTimeline.release(gold)) {
            if((tick-32)%20==0) {heard.addAll(audience.players());sound(hand,Sound.ENTITY_BREEZE_CHARGE,.8f,gold?.7f:1.1f);}
            if(tick%2==0) {
                for(int j=0;j<12;j++) {
                    double angle=j*Math.PI/6+tick*.17,r=.6+(j%3)*.35,up=(tick+j*3)%38/15.0;
                    dust(at(Math.cos(angle)*r,Math.sin(angle)*r,up),energy,2,.02);
                }
                particle(hand,gold?Particle.END_ROD:Particle.ELECTRIC_SPARK,5,.2,.02);
            }
        }
        if(tick==NobleTimeline.release(gold)) {
            start=hand.clone();flash(hand);heard.addAll(audience.players());
            for(Player p:heard)p.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);
            sound(hand,Sound.ENTITY_WARDEN_SONIC_BOOM,gold?2:1.2f,gold?.6f:1.5f);sound(hand,Sound.ENTITY_BREEZE_SHOOT,1.5f,gold?.65f:1.6f);
        }
        if(tick>=NobleTimeline.release(gold) && tick<NobleTimeline.fade(gold)) {
            double progress=Math.clamp((tick-NobleTimeline.release(gold))/(double)(NobleTimeline.impact(gold)-NobleTimeline.release(gold)),0,1);
            EnergyVisuals.stream(audience.players(),start,focus,tick,true);
            Vector line=focus.toVector().subtract(start.toVector());int visible=(int)Math.ceil(beam.length*progress);
            for(int i=0;i<visible;i++) {
                Location at=start.clone().add(line.clone().multiply((i+.5)/beam.length));at.setDirection(line);
                if(beam[i]==null) {
                    beam[i]=model(gold?SummonAssets.EXCALIBUR_LIGHT:SummonAssets.GAE_TRAIL,at,gold?2.3f:2.5f,"beam"+i);
                    loop(beam[i],"flow");TreasuryScene.point(beam[i],at);
                }
                if(tick%3==0) {dust(at,energy,6,gold?.5:.2);particle(at,gold?Particle.END_ROD:Particle.ELECTRIC_SPARK,2,.2,.03);}
            }
        }
        if(tick==NobleTimeline.impact(gold)) {
            flash(focus);trace("impact reach="+String.format(Locale.ROOT,"%.2f",focus.distance(origin)));
            if(gold) {pillar=model(SummonAssets.EXCALIBUR_PILLAR,focus,20,"light_pillar");pillar.update(TrackerUpdateAction.viewRange(4));loop(pillar,"surge");}
            if(!gold) thorns=model(SummonAssets.GAE_THORNS,focus,2,"heart_thorns");
            particle(focus,Particle.EXPLOSION_EMITTER,gold?5:1,gold?3:.2,0);particle(focus,Particle.DUST_PLUME,gold?60:12,gold?4:1,.06);
            particle(focus,Particle.CLOUD,gold?90:15,gold?4:1,.08);dust(focus,energy,gold?80:40,gold?3:.7);
            sound(focus,Sound.ENTITY_GENERIC_EXPLODE,gold?2.5f:1.1f,gold?.5f:1.4f);
        }
        if(pillar!=null)EnergyVisuals.stream(audience.players(),focus,focus.clone().add(0,30,0),tick,true);
        if(thorns!=null) thorns.scaler(ModelScaler.value((float)(2+.25*Math.sin(tick*.6))));
        if(tick==NobleTimeline.fade(gold)) {
            for(int i=0;i<beam.length;i++) {audience.hide(beam[i]);beam[i]=null;}
            audience.hide(pillar);pillar=null;audience.hide(thorns);thorns=null;audience.hide(weapon);weapon=null;
        }
    }
    private void flash(Location at) {for(Player p:audience.players())p.spawnParticle(Particle.FLASH,at,1,0,0,0,0,gold?Color.fromRGB(255,245,211):Color.fromRGB(255,180,185));}
    @Override void onClose() {for(Player p:heard)p.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);}
}
