package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BarrierHandPath {
    private static final RasenganHandPath.Hand[] RHO=read("rho_aias",BarrierSlashTimeline.RHO_END),GETSUGA=read("getsuga_tensho",BarrierSlashTimeline.GETSUGA_END),LEFT=read("getsuga_tensho-left",BarrierSlashTimeline.GETSUGA_END);
    private BarrierHandPath() {}
    private static RasenganHandPath.Hand[] read(String id,int duration){
        try(var in=Objects.requireNonNull(BarrierHandPath.class.getResourceAsStream("/bettermodel/"+id+"-hand-path.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            var a=r.lines().map(s->{var v=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(a.length!=duration)throw new IllegalStateException("Attachment duration mismatch: "+id);
            for(var p:a)if(!Double.isFinite(p.right()+p.forward()+p.height()))throw new IllegalStateException("Invalid palm: "+id);return a;
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static RasenganHandPath.Hand left(int tick){return LEFT[Math.clamp(tick,0,LEFT.length-1)];}
    public static RasenganHandPath.Hand at(boolean shield,int tick){var a=shield?RHO:GETSUGA;return a[Math.clamp(tick,0,a.length-1)];}
}
