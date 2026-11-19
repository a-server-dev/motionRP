package dev.aserver.motionrp;

import dev.aserver.motionrp.core.DragonBallTimeline;
import dev.aserver.motionrp.core.EnergyHandPath;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.DragonBallTimeline.*;

/** Bounded energy effects. The Spirit Bomb's actual player flight is journalled for recovery. */
final class DragonBallScene extends SummonScene {
    private final boolean spirit;private final EntityTracker body;private final AerialAnchor flight;
    private final java.util.Set<Player> heard=new java.util.HashSet<>();
    private DummyTracker orb;private final DummyTracker[] beam=new DummyTracker[20];private Location launch,target,orbCenter;
    private static final Particle.DustOptions BLUE=new Particle.DustOptions(Color.fromRGB(90,205,255),2);
    DragonBallScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean spirit) {
        super(plugin,owner,spirit?"GenkiDama":"Kamehameha",DragonBallTimeline.end(spirit),192,true);
        this.spirit=spirit;this.body=body;flight=new AerialAnchor(plugin,owner);
        body.rotation(()->new ModelRotation(0,origin.getYaw()));
        if(spirit)body.update(TrackerUpdateAction.moveDuration(1));
        target=ground(at(0,spirit?70:45,0));if(target==null)target=at(0,spirit?70:45,.2);
    }
    @Override String phase(){return DragonBallTimeline.phase(tick,spirit);}
    @Override public boolean ownsMove(Location a,Location b){return flight.owns(a,b);}
    @Override void onClose(){for(Player p:heard)p.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);flight.close();owner.setFallDistance(0);}
    private void flash(Location point){for(Player p:audience.players())p.spawnParticle(Particle.FLASH,point,1,0,0,0,0,Color.fromRGB(215,248,255),true);}
    private void fx(Location point,Particle type,int count,double spread,double speed){for(Player p:audience.players())p.spawnParticle(type,point,count,spread,spread,spread,speed,null,true);}
    private Location attach(EnergyHandPath.Point p){return at(p.right(),p.forward(),p.height()).add(owner.getLocation().toVector().subtract(origin.toVector()));}
    private Location palms(){return attach(EnergyHandPath.at(spirit,tick).middle());}
    @Override void update(){
        body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
        int release=spirit?SPIRIT_RELEASE:KAME_RELEASE,impact=spirit?SPIRIT_IMPACT:KAME_IMPACT,fade=spirit?SPIRIT_FADE:KAME_FADE;
        if(spirit && tick>=RISE && tick<=RISE_END){
            Location desired=at(0,0,DragonBallTimeline.height(tick));Location from=owner.getLocation();
            Vector delta=desired.toVector().subtract(from.toVector());int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));
            for(int j=1;j<=steps;j++){
                Vector shift=delta.clone().multiply(j/(double)steps);Location p=from.clone().add(shift);
                if(!p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4) || owner.wouldCollideUsing(owner.getBoundingBox().shift(shift))){plugin.playback().stop(owner.getUniqueId());return;}
            }
            desired.setYaw(owner.getYaw());desired.setPitch(owner.getPitch());
            if(!flight.moveTo(desired,origin)){plugin.playback().stop(owner.getUniqueId());return;}
            if(tick==RISE_END)trace("rise complete height="+(owner.getY()-origin.getY())+" ticks=40 diameter=80");
        }
        if(tick==(spirit?RISE:16)){
            orbCenter=spirit?attach(EnergyHandPath.sphereCenter(tick,.1)):palms();
            orb=model(spirit?SummonAssets.GENKI_ORB:SummonAssets.KAME_ORB,orbCenter,.1f,"charge_orb");
            orb.update(TrackerUpdateAction.composite(TrackerUpdateAction.viewRange(4),TrackerUpdateAction.moveDuration(1)));loop(orb,"corona");
        }
        if(orb!=null && tick<release){
            float scale=spirit?(float)(.1+39.9*Math.clamp((tick-RISE)/(double)(SPIRIT_RELEASE-RISE-10),0,1)):.3f;
            orb.scaler(ModelScaler.value(scale));orb.forceUpdate(true);
            orbCenter=spirit?attach(EnergyHandPath.sphereCenter(tick,scale)):palms();orb.location(BukkitAdapter.adapt(orbCenter));
            if(tick%2==0)EnergyVisuals.surface(audience.players(),orbCenter,scale*1.03,tick);
            if(spirit && tick==SPIRIT_RELEASE-10)trace("sphere ready diameter=80 bottom tracks raised palms");
            if(tick%12==0){heard.addAll(audience.players());sound(owner.getLocation(),Sound.ENTITY_BREEZE_CHARGE,1,.7f);}
            if(tick%3==0){fx(owner.getLocation().add(0,1,0),Particle.END_ROD,12,1.5,.05);dust(owner.getLocation(),BLUE,10,2);}
        }
        if(tick==release){
            launch=spirit?orbCenter.clone():palms();flash(owner.getLocation());
            for(Player p:heard)p.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);
            sound(owner.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,3,.5f);
            if(!spirit){audience.hide(orb);orb=null;
                Vector line=target.toVector().subtract(launch.toVector());
                var hit=origin.getWorld().rayTraceBlocks(launch,line.clone().normalize(),line.length(),FluidCollisionMode.NEVER,true);
                if(hit!=null)target=hit.getHitPosition().toLocation(origin.getWorld());
            }
        }
        if(spirit && tick>=release && tick<=impact && orb!=null){
            double t=(tick-release)/(double)(impact-release);orbCenter=launch.clone().add(target.toVector().subtract(launch.toVector()).multiply(t));orb.location(BukkitAdapter.adapt(orbCenter));
            if(tick%2==0)EnergyVisuals.surface(audience.players(),orbCenter,41.2,tick);
            if(tick==release+8){flight.close();trace("pilot returned");}
        }
        if(!spirit && tick>=release && tick<fade){
            EnergyVisuals.stream(audience.players(),launch,target,tick,false);
            Vector line=target.toVector().subtract(launch.toVector());int visible=(int)Math.ceil(beam.length*Math.clamp((tick-release)/(double)(impact-release),0,1));
            for(int j=0;j<visible;j++)if(beam[j]==null){Location p=launch.clone().add(line.clone().multiply((j+.5)/beam.length));p.setDirection(line);
                beam[j]=model(SummonAssets.KAME_BEAM,p,(float)Math.max(1.8,line.length()/25),"beam"+j);loop(beam[j],"flow");TreasuryScene.point(beam[j],p);}
        }
        if(tick==impact){
            if(spirit){audience.hide(orb);orb=null;}
            flash(target);fx(target,Particle.EXPLOSION_EMITTER,spirit?20:8,spirit?14:4,0);
            fx(target,Particle.CLOUD,spirit?300:100,spirit?18:5,.15);fx(target,Particle.DUST_PLUME,spirit?180:70,spirit?15:4,.1);
            fx(target,Particle.END_ROD,spirit?200:80,spirit?18:5,.2);sound(target,Sound.ENTITY_GENERIC_EXPLODE,6,.5f);trace("impact");
        }
        if(tick==fade){for(var b:beam)audience.hide(b);java.util.Arrays.fill(beam,null);flight.close();}
    }
}
