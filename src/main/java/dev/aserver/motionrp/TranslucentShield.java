package dev.aserver.motionrp;
import dev.aserver.motionrp.core.ShieldDome;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.joml.Matrix4f;
import java.util.*;

/** Native text-background quads blend with alpha; vanilla item meshes do not. */
final class TranslucentShield implements AutoCloseable {
    private record Tile(TextDisplay entity,Matrix4f transform) {}
    private final SceneAudience audience;private final List<Tile> tiles=new ArrayList<>();
    private float previous=-1;private int previousAlpha=-1;
    TranslucentShield(SceneAudience audience,Location center,double radius){
        this.audience=audience;
        try{
            for(var p:ShieldDome.patches(radius)){
                // A single vanilla space has a 5x11 pixel background; its local center is (.0125,.1375).
                var basis=new Matrix4f().m02(-p.nx()/p.nz()).m12(-p.ny()/p.nz()).m20(p.nx()).m21(p.ny()).m22(p.nz());
                var matrix=new Matrix4f().translation(p.x(),p.y(),p.z()).mul(basis)
                        .translate(-p.width()*.1f,-p.height()*.5f,0).scale(p.width()*8,p.height()*40/11,1);
                var display=center.getWorld().spawn(center,TextDisplay.class,e->{
                    e.setVisibleByDefault(false);e.setPersistent(false);e.setInvulnerable(true);e.setSilent(true);e.setBillboard(Display.Billboard.FIXED);e.setBrightness(new Display.Brightness(15,15));
                    e.text(Component.text(" "));e.setLineWidth(20);e.setDefaultBackground(false);e.setBackgroundColor(Color.fromARGB(42,255,151,220));e.setShadowed(false);e.setSeeThrough(false);e.setShadowRadius(0);e.setShadowStrength(0);e.setInterpolationDuration(1);e.setTeleportDuration(0);e.setViewRange(3);e.setDisplayWidth(5);e.setDisplayHeight(5);
                    e.setTransformationMatrix(new Matrix4f().scaling(.01f).mul(matrix));
                });
                tiles.add(new Tile(display,matrix));audience.showNative(display);
            }
        }catch(RuntimeException failure){close();throw failure;}
    }
    void update(float scale,float fade){
        int alpha=Math.clamp(Math.round(42*fade),0,42);
        if(Math.abs(scale-previous)<.0001 && alpha==previousAlpha)return;
        for(var tile:tiles){if(Math.abs(scale-previous)>=.0001)tile.entity.setTransformationMatrix(new Matrix4f().scaling(Math.max(.005f,scale)).mul(tile.transform));if(alpha!=previousAlpha)tile.entity.setBackgroundColor(Color.fromARGB(alpha,255,151,220));}
        previous=scale;previousAlpha=alpha;
    }
    String ids(){return tiles.stream().map(t->Integer.toString(t.entity.getEntityId())).collect(java.util.stream.Collectors.joining(","));}
    @Override public void close(){for(var tile:tiles)audience.hideNative(tile.entity);tiles.clear();}
}
