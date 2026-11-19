package dev.aserver.motionrp.core;
import java.util.*;

/** Tangent patches for a genuinely translucent native display dome. Coordinates are in blocks. */
public final class ShieldDome {
    public record Patch(float x,float y,float z,float width,float height,float nx,float ny,float nz) {}
    private ShieldDome() {}
    public static List<Patch> patches(double radius){
        if(!Double.isFinite(radius)||radius<=0)throw new IllegalArgumentException("radius");
        var out=new ArrayList<Patch>();int rows=12,columns=7;
        for(int row=0;row<rows;row++){
            double low=Math.sin(-Math.PI/2+row*Math.PI/rows),high=Math.sin(-Math.PI/2+(row+1)*Math.PI/rows),v=(low+high)/2;
            double half=Math.sqrt(1-v*v),w=2*half/columns;
            for(int col=0;col<columns;col++){
                double u=-half+(col+.5)*w,root=Math.sqrt(Math.max(.001,1-u*u-v*v));
                double nx=.48*u/root,ny=.48*v/root,nz=1,length=Math.sqrt(nx*nx+ny*ny+1);
                out.add(new Patch((float)(u*radius),(float)(v*radius),(float)(.48*radius*root),(float)(w*radius*1.02),(float)((high-low)*radius*1.02),(float)(nx/length),(float)(ny/length),(float)(nz/length)));
            }
        }
        return List.copyOf(out);
    }
}
