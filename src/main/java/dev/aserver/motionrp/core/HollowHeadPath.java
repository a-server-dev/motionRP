package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
public final class HollowHeadPath {
    private static final RasenganHandPath.Hand[] PATH;
    static {
        try(var in=Objects.requireNonNull(HollowHeadPath.class.getResourceAsStream("/bettermodel/getsuga_tensho-head-path.csv"));var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            PATH=reader.lines().map(s->{var v=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(PATH.length!=BarrierSlashTimeline.GETSUGA_END)throw new IllegalStateException("Head path duration mismatch");
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private HollowHeadPath() {}
    public static RasenganHandPath.Hand at(int tick){return PATH[Math.clamp(tick,0,PATH.length-1)];}
}
