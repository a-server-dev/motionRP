package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.Set;
import static dev.aserver.motionrp.core.BarrierSlashTimeline.*;

/** Cosmetic shield/Bankai crescent. No damage, terrain edits, or projectile interception. */
final class BarrierSlashScene extends SummonScene {
    private final boolean shield;private final EntityTracker body;
    private final DummyTracker[] layers=new DummyTracker[7];private DummyTracker sword,wave;
    private double reach=29.5,hitDistance=32;private int impactTick=GETSUGA_IMPACT;private boolean impacted;
    private static final Particle.DustOptions PINK=new Particle.DustOptions(Color.fromRGB(255,115,202),1.2f),RED=new Particle.DustOptions(Color.fromRGB(226,15,61),1.5f),BLACK=new Particle.DustOptions(Color.fromRGB(15,4,20),2);
    BarrierSlashScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean shield){
        super(plugin,owner,shield?"RhoAias":"GetsugaTensho",end(shield));this.shield=shield;this.body=body;
        body.rotation(()->new ModelRotation(0,origin.getYaw()));body.update(TrackerUpdateAction.moveDuration(0));
    }
    @Override String phase(){return shield?(tick<16?"PREPARE":tick<72?"UNFURL":tick<146?"HOLD":"FADE"):(tick<28?"DRAW":tick<GETSUGA_RELEASE?"CHARGE":tick<impactTick?"FLIGHT":tick<112?"IMPACT":"FADE");}
    @Override void update(){
        body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
        if(shield)barrier();else slash();
    }
    private Location palm(){var p=BarrierHandPath.at(shield,tick);return at(p.right(),p.forward(),p.height());}
    private void flash(Location p,Color color){for(Player viewer:audience.players())viewer.spawnParticle(Particle.FLASH,p,1,0,0,0,0,color);}
    private void barrier(){
        for(int i=0;i<7;i++){
            if(tick==layerStart(i)){
                layers[i]=model("motionrp_rho_aias_shield",at(0,2.1+i*.13,2.2),.05f,"layer"+i);
                layers[i].animate("bloom",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(palm(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,.55f,1.2f+i*.1f);
            }
            var layer=layers[i];if(layer==null)continue;
            double growth=Math.clamp((tick-layerStart(i))/12.0,0,1),fade=tick<146?1:Math.clamp((172-tick)/26.0,0,1);
            layer.scaler(ModelScaler.value((float)Math.max(.01,(1.05+i*.14)*growth*fade)));
            layer.rotation(()->new ModelRotation(0,origin.getYaw()));
            if(tick>=146+i*4){audience.hide(layer);layers[i]=null;}
        }
        if(tick>=16 && tick<146 && tick%3==0){
            double a=tick*.14;Location p=at(Math.cos(a)*1.7,2.1,2.2+Math.sin(a)*1.7);dust(p,PINK,3,.06);particle(p,Particle.ENCHANT,3,.12,.03);
        }
        if(tick==72 || tick==96 || tick==120){
            Location hit=at((tick==96?-.5:.5),2.2,1.8);flash(hit,Color.fromRGB(255,211,242));particle(hit,Particle.ELECTRIC_SPARK,24,.45,.05);sound(hit,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.65f,1.5f);trace("shield pulse tick="+tick);
        }
    }
    private void slash(){
        if(tick==8){sword=model("motionrp_getsuga_tensa",palm(),.65f,"tensa_zangetsu");sword.update(TrackerUpdateAction.moveDuration(0));sword.animate("cut",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(palm(),Sound.ITEM_ARMOR_EQUIP_NETHERITE,.8f,.8f);}
        if(sword!=null){sword.location(BukkitAdapter.adapt(palm()));sword.rotation(()->new ModelRotation(0,origin.getYaw()));sword.forceUpdate(true);}
        if(tick>=28 && tick<GETSUGA_RELEASE && tick%3==0){dust(palm(),BLACK,8,.22);dust(palm(),RED,5,.16);}
        if(tick==28)sound(palm(),Sound.ENTITY_WARDEN_SONIC_CHARGE,.75f,.65f);
        if(tick==GETSUGA_RELEASE){
            // Bound ray tracing to already loaded chunks; a cosmetic cast must not load terrain.
            for(double d=.5;d<=32;d+=.5){Location p=at(0,d,0);if(!p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4)){hitDistance=Math.max(0,d-.5);break;}}
            if(hitDistance>0)for(double h:new double[]{.4,1.8,3.2}){
                Location start=at(0,0,h);var hit=start.getWorld().rayTraceBlocks(start,origin.getDirection(),hitDistance,FluidCollisionMode.NEVER,true);
                if(hit!=null)hitDistance=Math.min(hitDistance,hit.getHitPosition().distance(start.toVector()));
            }
            reach=Math.max(0,hitDistance-2.5);
            impactTick=GETSUGA_RELEASE+Math.max(1,(int)Math.ceil(reach/.8));
            if(reach>0){wave=model("motionrp_getsuga_crescent",at(0,0,2.5),1.6f,"crescent");wave.update(TrackerUpdateAction.moveDuration(0));
            wave.rotation(()->new ModelRotation(0,origin.getYaw()));}
            sound(origin,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.4f,.65f);sound(origin,Sound.ENTITY_BREEZE_SHOOT,1.3f,.55f);trace("release reach="+reach+" impactTick="+impactTick);
        }
        if(wave!=null && !impacted){
            double d=Math.min(reach,distance(tick));Location point=at(0,d,2.5);wave.location(BukkitAdapter.adapt(point));wave.forceUpdate(true);
            for(int j=0;j<7;j++){double a=-1.4+j*2.8/6;Location p=at(0,d+2.4*Math.cos(a),2.5+2.4*Math.sin(a));dust(p,RED,2,.07);dust(p,BLACK,2,.12);}
        }
        if(!impacted && tick>=GETSUGA_RELEASE && tick>=impactTick){
            impacted=true;audience.hide(wave);wave=null;Location p=at(0,hitDistance,.5);flash(p,Color.fromRGB(255,67,100));particle(p,Particle.EXPLOSION,2,.5,0);particle(p,Particle.DUST_PLUME,45,1.3,.045);particle(p,Particle.CLOUD,45,1,.07);dust(p,RED,70,1.7);sound(p,Sound.ENTITY_GENERIC_EXPLODE,1.8f,.65f);trace("crescent impact distance="+hitDistance+" tick="+tick);
        }

        if(tick==112){audience.hide(sword);sword=null;}
    }
}
