package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** The clearing swing, two-hand grip and blade tip are baked from the player choreography. */
public final class GetsugaBladePath {
    public record Blade(RasenganHandPath.Hand grip,RasenganHandPath.Hand tip) {}
    private static final Blade[] PATH;
    static {
        try(var in=Objects.requireNonNull(GetsugaBladePath.class.getResourceAsStream("/bettermodel/getsuga_tensho-blade-path.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            PATH=r.lines().map(s->{var p=s.split(",");if(p.length!=6)throw new IllegalStateException("Invalid sword pose");double[] v=new double[6];for(int i=0;i<6;i++){v[i]=Double.parseDouble(p[i]);if(!Double.isFinite(v[i]))throw new IllegalStateException("Non-finite sword pose");}return new Blade(new RasenganHandPath.Hand(v[0],v[1],v[2]),new RasenganHandPath.Hand(v[3],v[4],v[5]));}).toArray(Blade[]::new);
            if(PATH.length!=BarrierSlashTimeline.GETSUGA_END)throw new IllegalStateException("Sword pose duration mismatch");
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private GetsugaBladePath() {}
    public static Blade at(int tick){return PATH[Math.clamp(tick,0,PATH.length-1)];}
}
