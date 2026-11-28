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
    private final DummyTracker[] layers=new DummyTracker[7];
    private final TranslucentShield[] translucent=new TranslucentShield[7];
    private DummyTracker mask;
    private final DummyTracker[] trails=new DummyTracker[2];
    private final java.util.Set<Player> maskHeard=new java.util.HashSet<>();
    private final java.util.Set<Player> chargingHeard=new java.util.HashSet<>();private DummyTracker sword,wave;
    private double reach=29.5,hitDistance=32;private int impactTick=GETSUGA_IMPACT;private boolean impacted;
    private static final Particle.DustOptions PINK=new Particle.DustOptions(Color.fromRGB(255,115,202),1.2f),VIOLET=new Particle.DustOptions(Color.fromRGB(166,52,245),1.5f),BLACK=new Particle.DustOptions(Color.fromRGB(15,4,20),2);
    BarrierSlashScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean shield){
        super(plugin,owner,shield?"RhoAias":"GetsugaTensho",end(shield));this.shield=shield;this.body=body;
        body.rotation(()->new ModelRotation(0,origin.getYaw()));body.update(TrackerUpdateAction.moveDuration(0));
    }
    @Override String phase(){return shield?(tick<16?"PREPARE":tick<72?"UNFURL":tick<146?"HOLD":"FADE"):(tick<GETSUGA_PREFIX?"HOLLOW_MASK":tick<GETSUGA_FLOURISH_END?"FLOURISH":tick<GETSUGA_CHARGE?"DRAW":tick<GETSUGA_RELEASE?"CHARGE":tick<impactTick?"FLIGHT":tick<GETSUGA_FADE?"IMPACT":"FADE");}
    @Override void update(){
        body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
        if(shield)barrier();else slash();
    }
    private Location palm(){var p=shield?BarrierHandPath.at(true,tick):GetsugaBladePath.at(tick).grip();return at(p.right(),p.forward(),p.height());}
    private void flash(Location p,Color color){for(Player viewer:audience.players())viewer.spawnParticle(Particle.FLASH,p,1,0,0,0,0,color);}
    private void barrier(){
        for(int i=0;i<7;i++){
            if(i>0){
                if(tick==layerStart(i)){
                    translucent[i]=new TranslucentShield(audience,at(0,layerForward(i),4.3),layerScale(i)*18/16);
                    trace("translucent layer="+i+" alpha=42 ids="+translucent[i].ids());sound(palm(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,.5f,1.2f+i*.1f);
                }
                if(translucent[i]!=null){
                    double growth=Math.clamp((tick-layerStart(i))/12.0,0,1),fade=tick<146?1:Math.clamp((172-tick)/26.0,0,1);
                    translucent[i].update((float)(growth*fade),(float)fade);
                    if(tick>=146+i*4){translucent[i].close();translucent[i]=null;}
                }
                continue;
            }
            if(tick==layerStart(i)){
                layers[i]=model("motionrp_rho_aias_shield",at(0,layerForward(0),4.3),.05f,"layer0");
                layers[i].animate("bloom",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(palm(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,.55f,1.2f);
            }
            var layer=layers[i];if(layer==null)continue;
            double growth=Math.clamp((tick-layerStart(i))/12.0,0,1),fade=tick<146?1:Math.clamp((172-tick)/26.0,0,1);
            layer.scaler(ModelScaler.value((float)Math.max(.01,layerScale(i)*growth*fade)));
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
        hollowMask();
        if(tick==0){sword=model("motionrp_getsuga_tensa",palm(),.65f,"tensa_zangetsu");sword.update(TrackerUpdateAction.moveDuration(0));sword.animate("cut",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);sound(palm(),Sound.ITEM_ARMOR_EQUIP_NETHERITE,.8f,.8f);}
        if(sword!=null){sword.location(BukkitAdapter.adapt(palm()));sword.rotation(()->new ModelRotation(0,origin.getYaw()));sword.forceUpdate(true);}
        if(tick==GETSUGA_PREFIX)trace("one-handed clearing swing begins after mask");
        if(tick>=50 && tick<=58){
            var now=GetsugaBladePath.at(tick);var before=GetsugaBladePath.at(tick-1);
            for(int i=0;i<7;i++)for(int j=0;j<3;j++){
                double blade=.3+i*.7/6,time=j/2.0;
                var a=before.grip();var b=before.tip();var c=now.grip();var d=now.tip();
                Location point=at((a.right()+(b.right()-a.right())*blade)*(1-time)+(c.right()+(d.right()-c.right())*blade)*time,
                    (a.forward()+(b.forward()-a.forward())*blade)*(1-time)+(c.forward()+(d.forward()-c.forward())*blade)*time,
                    (a.height()+(b.height()-a.height())*blade)*(1-time)+(c.height()+(d.height()-c.height())*blade)*time);
                dust(point,BLACK,1,.025);dust(point,VIOLET,1,.015);
            }
            if(tick==52){sound(palm(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.8f,1.2f);trace("clearing swing trails blade; no projectile");}
        }
        if(tick==GETSUGA_FLOURISH_END)trace("clearing swing ends; both hands prepare overhead");
        if(tick>=GETSUGA_CHARGE && tick<GETSUGA_RELEASE){
            var blade=GetsugaBladePath.at(tick);var tip=blade.tip();var grip=blade.grip();
            for(int j=0;j<9;j++){
                double f=.23+j*.77/8;Location p=at(grip.right()+(tip.right()-grip.right())*f,grip.forward()+(tip.forward()-grip.forward())*f,grip.height()+(tip.height()-grip.height())*f);
                dust(p,BLACK,5,.09);particle(p,Particle.LARGE_SMOKE,2,.07,.018);
                if(tick%2==0)particle(p,Particle.SQUID_INK,1,.06,.025);
                if(j%3==0){dust(p,VIOLET,1,.04);if(tick%4==0)particle(p,Particle.END_ROD,1,.04,.008);}
            }
            if(tick%3==0){dust(at(0,0,.12),BLACK,3,.32);particle(at(0,0,.15),Particle.SMOKE,1,.25,.01);}
        }
        if(tick==GETSUGA_CHARGE){chargingHeard.addAll(audience.players());sound(palm(),Sound.ENTITY_WARDEN_SONIC_CHARGE,.9f,.6f);trace("two-handed overhead charge; blade aura=45 foot aura=3");}
        if(tick==GETSUGA_RELEASE){
            stopCharge();
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
            for(int i=0;i<2;i++){
                if(tick>=GETSUGA_RELEASE+2+i*2 && trails[i]==null){trails[i]=model("motionrp_getsuga_crescent",at(0,Math.max(0,d-.8*(i+1)),2.5),1.4f-i*.2f,"crescent_trail"+i);trails[i].update(TrackerUpdateAction.moveDuration(0));trails[i].rotation(()->new ModelRotation(0,origin.getYaw()));}
                if(trails[i]!=null){trails[i].location(BukkitAdapter.adapt(at(0,Math.max(0,d-.8*(i+1)),2.5)));trails[i].forceUpdate(true);}
            }
            for(int j=0;j<13;j++){
                double a=-1.4+j*2.8/12,y=2.4*Math.sin(a);Location p=at(y*Math.sin(Math.toRadians(35)),d+2.4*Math.cos(a),2.5+y*Math.cos(Math.toRadians(35)));
                dust(p,BLACK,6,.18);dust(p,VIOLET,3,.1);particle(p,Particle.SMOKE,2,.15,.015);
            }
            if(tick%2==0)particle(at(0,Math.max(0,d-.5),2.5),Particle.SQUID_INK,6,.4,.04);
        }
        if(!impacted && tick>=GETSUGA_RELEASE && tick>=impactTick){
            impacted=true;audience.hide(wave);wave=null;for(int i=0;i<2;i++){audience.hide(trails[i]);trails[i]=null;}Location p=at(0,hitDistance,.5);flash(p,Color.fromRGB(255,67,100));particle(p,Particle.EXPLOSION,2,.5,0);particle(p,Particle.DUST_PLUME,45,1.3,.045);particle(p,Particle.CLOUD,45,1,.07);dust(p,VIOLET,70,1.7);sound(p,Sound.ENTITY_GENERIC_EXPLODE,1.8f,.65f);trace("crescent impact distance="+hitDistance+" tick="+tick);
        }

        if(tick==GETSUGA_FADE){audience.hide(sword);sword=null;}
    }
    private void hollowMask(){
        var p=HollowHeadPath.at(tick);Location head=at(p.right(),p.forward(),p.height());
        if(tick==MASK_START){
            mask=java.util.Objects.requireNonNull(kr.toxicity.model.api.BetterModel.modelOrNull("motionrp_hollow_mask")).create(BukkitAdapter.adapt(head),MODIFIER,t->{t.update(TrackerUpdateAction.brightness(15,15));t.update(TrackerUpdateAction.moveDuration(0));t.update(TrackerUpdateAction.togglePart(false),b->b.name().rawName().startsWith("mask_band_"));});
            mask.rotation(()->new ModelRotation(0,origin.getYaw()));mask.animate("wear",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);audience.show(mask,plugin,prefix,"hollow_mask");maskHeard.addAll(audience.players());sound(head,Sound.ENTITY_ENDERMAN_STARE,.6f,.75f);
        }
        if(mask!=null){mask.location(BukkitAdapter.adapt(head));mask.forceUpdate(true);}
        if(tick>=MASK_START && tick<=MASK_COMPLETE){
            if((tick-MASK_START)%2==0){int band=(tick-MASK_START)/2;mask.update(TrackerUpdateAction.togglePart(true),b->b.name().rawName().equals("mask_band_"+band));trace("mask band="+band+" tick="+tick);}
            var hand=BarrierHandPath.left(tick);Location trace=at(hand.right(),hand.forward(),hand.height());dust(trace,BLACK,3,.09);particle(trace,Particle.SQUID_INK,1,.06,.01);if(tick%3==0)particle(trace,Particle.SMOKE,1,.06,.01);
            if(tick==MASK_COMPLETE){for(Player viewer:maskHeard)if(viewer.isOnline())viewer.stopSound(Sound.ENTITY_ENDERMAN_STARE,SoundCategory.PLAYERS);maskHeard.addAll(audience.players());sound(head,Sound.ENTITY_WITHER_AMBIENT,.7f,1.5f);trace("mask complete; forehead-right to jaw-left");}
        }
    }
    private void stopCharge(){for(Player p:chargingHeard)if(p.isOnline())p.stopSound(Sound.ENTITY_WARDEN_SONIC_CHARGE,SoundCategory.PLAYERS);chargingHeard.clear();}
    @Override void onClose(){stopCharge();for(Player p:maskHeard)if(p.isOnline()){p.stopSound(Sound.ENTITY_ENDERMAN_STARE,SoundCategory.PLAYERS);p.stopSound(Sound.ENTITY_WITHER_AMBIENT,SoundCategory.PLAYERS);}maskHeard.clear();}

}
