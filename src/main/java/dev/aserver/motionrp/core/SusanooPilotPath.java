package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
public final class SusanooPilotPath {
    private static final RasenganHandPath.Hand[] PATH;
    static {
        try(var in=Objects.requireNonNull(SusanooPilotPath.class.getResourceAsStream("/bettermodel/susanoo-pilot-path.csv"));var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            PATH=reader.lines().map(s->{var v=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(PATH.length!=SusanooTimeline.END)throw new IllegalStateException("Head path duration mismatch");
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    private SusanooPilotPath() {}
    public static RasenganHandPath.Hand at(int tick){return PATH[Math.clamp(tick,0,PATH.length-1)];}
}
