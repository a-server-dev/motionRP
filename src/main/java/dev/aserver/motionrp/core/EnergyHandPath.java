package dev.aserver.motionrp.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Both palms baked from the very same body pose, including the crouching root offset. */
public final class EnergyHandPath {
    public record Point(double right,double forward,double height) {}
    public record Palms(Point right,Point left) {
        public Point middle(){return new Point((right.right+left.right)/2,(right.forward+left.forward)/2,(right.height+left.height)/2);}
    }
    private static final Palms[] KAME=read("kamehameha",128),SPIRIT=read("genki_dama",176);
    private EnergyHandPath() {}
    private static Palms[] read(String id,int length){
        try(var in=Objects.requireNonNull(EnergyHandPath.class.getResourceAsStream("/bettermodel/"+id+"-palms.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            Palms[] path=r.lines().map(line->{String[] p=line.split(",");if(p.length!=6)throw new IllegalStateException("Invalid energy palm row");double[] v=java.util.Arrays.stream(p).mapToDouble(Double::parseDouble).toArray();for(double n:v)if(!Double.isFinite(n))throw new IllegalStateException("Nonfinite palm");return new Palms(new Point(v[0],v[1],v[2]),new Point(v[3],v[4],v[5]));}).toArray(Palms[]::new);
            if(path.length!=length)throw new IllegalStateException("Energy palm duration mismatch");return path;
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static Palms at(boolean spirit,int tick){var p=spirit?SPIRIT:KAME;return p[Math.clamp(tick,0,p.length-1)];}
    /** Radius in blocks: the source sphere is exactly two blocks across at scale one. */
    public static Point sphereCenter(int tick,double radius){var p=at(true,tick).middle();return new Point(p.right,p.forward,p.height+radius+.12);}
}
