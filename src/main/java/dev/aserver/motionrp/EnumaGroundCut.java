package dev.aserver.motionrp;

import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.util.Vector;
import java.util.*;

/** A widening shallow V-shaped excavation, shown progressively along the beam. */
final class EnumaGroundCut implements AutoCloseable {
    private final TreasuryScene scene;
    private final Location target;
    private final Set<TerrainOverlayManager.Cell> visible=new HashSet<>();
    private int completed;
    EnumaGroundCut(TreasuryScene scene,Location target) { this.scene=scene;this.target=target.clone(); }
    void update(double progress) {
        Vector direction=target.toVector().subtract(scene.origin.toVector()).setY(0).normalize();
        Vector side=new Vector(direction.getZ(),0,-direction.getX());
        int steps=(int)Math.floor(Math.clamp(progress,0,1)*18);
        for(;completed<steps;completed++) {
            double p=(completed+.5)/18,width=.6+2.4*p;
            Location center=scene.origin.clone().add(direction.clone().multiply(3+(target.distance(scene.origin)-4.5)*p));
            for(int lateral=-(int)Math.ceil(width);lateral<=Math.ceil(width);lateral++) {
                if(Math.abs(lateral)>width) continue;
                Location at=center.clone().add(side.clone().multiply(lateral));
                Location floor=scene.ground(at);if(floor==null) continue;
                var block=floor.clone().subtract(0,.13,0).getBlock();
                int depth=1+(int)Math.floor(1.5*(1-Math.abs(lateral)/width));
                for(int d=0;d<depth;d++) {
                    var cell=block.getRelative(0,-d,0);
                    if(cell.getY()<scene.origin.getWorld().getMinHeight() || !cell.getType().isSolid() || cell.getState(false) instanceof TileState) continue;
                    visible.add(new TerrainOverlayManager.Cell(cell.getWorld(),cell.getX(),cell.getY(),cell.getZ()));
                }
                scene.particle(floor,Particle.DUST_PLUME,2,.25,.02);
                for(var player:scene.audience.players()) player.spawnParticle(Particle.BLOCK,floor,5,.25,.25,.25,.03,block.getBlockData());
            }
        }
        scene.plugin.terrain().show(this,visible,scene.audience.players());
    }
    @Override public void close() { scene.plugin.terrain().release(this); }
}
