package dev.aserver.motionrp;

import java.util.Collection;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Moving highlights on rounded meshes. Explicit long distance packets retain the giant sphere's corona. */
final class EnergyVisuals {
    private EnergyVisuals() {}
    static void surface(Collection<Player> viewers,Location center,double radius,int tick){
        int count=radius>5?64:16;
        for(int j=0;j<count;j++){
            double y=1-2*(j+.5)/count,angle=j*2.399963229728653+tick*.16,r=Math.sqrt(1-y*y);
            Location p=center.clone().add(radius*r*Math.cos(angle),radius*y,radius*r*Math.sin(angle));
            for(Player viewer:viewers)viewer.spawnParticle(Particle.END_ROD,p,1,.025,.025,.025,.015,null,true);
        }
    }
    static void stream(Collection<Player> viewers,Location start,Location end,int tick,boolean gold){
        Vector axis=end.toVector().subtract(start.toVector());double length=axis.length();if(length<.01)return;axis.normalize();
        Vector reference=Math.abs(axis.getY())>.9?new Vector(1,0,0):new Vector(0,1,0);
        Vector u=axis.clone().crossProduct(reference).normalize(),v=axis.clone().crossProduct(u).normalize();
        var dust=new Particle.DustTransition(Color.WHITE,gold?Color.fromRGB(255,228,112):Color.fromRGB(90,205,255),1.2f);
        for(int j=0;j<24;j++){
            double distance=(tick*1.9+j*length/24)%length,angle=tick*.65+j*.9,r=.6+.2*Math.sin(j+tick*.5);
            Location p=start.clone().add(axis.clone().multiply(distance)).add(u.clone().multiply(r*Math.cos(angle))).add(v.clone().multiply(r*Math.sin(angle)));
            for(Player viewer:viewers){
                viewer.spawnParticle(Particle.DUST_COLOR_TRANSITION,p,1,.015,.015,.015,0,dust,true);
                if(j%3==0)viewer.spawnParticle(Particle.END_ROD,p,0,axis.getX(),axis.getY(),axis.getZ(),.35,null,true);
            }
        }
    }
}
