package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
/** Actual nape attachment sampled from the same tackle rig, at ten times player scale. */
public final class TitanTacklePath {
 private static final RasenganHandPath.Hand[] PATH;
 static {try(var in=Objects.requireNonNull(TitanTacklePath.class.getResourceAsStream("/bettermodel/titan-tackle-nape.csv"));var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){PATH=reader.lines().map(s->{var p=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]));}).toArray(RasenganHandPath.Hand[]::new);if(PATH.length!=109)throw new IllegalStateException("Tackle path duration");}catch(IOException e){throw new UncheckedIOException(e);}}
 private TitanTacklePath(){}
 public static RasenganHandPath.Hand at(int tick){return PATH[Math.clamp(tick-TitanTimeline.TACKLE,0,108)];}
}
