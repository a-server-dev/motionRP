package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class EpicHandPath {
 private static final Map<String,RasenganHandPath.Hand[]> PATHS=new HashMap<>();
 static{for(String id:EpicTimeline.EFFECTS)try(var in=Objects.requireNonNull(EpicHandPath.class.getResourceAsStream("/bettermodel/"+id+"-hand-path.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){var path=r.lines().map(line->{var v=line.split(",");return new RasenganHandPath.Hand(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}).toArray(RasenganHandPath.Hand[]::new);if(path.length!=EpicTimeline.end(id))throw new IllegalStateException("Epic attachment duration: "+id);PATHS.put(id,path);}catch(IOException e){throw new UncheckedIOException(e);}}
 private EpicHandPath(){}
 public static RasenganHandPath.Hand at(String id,int tick){var path=Objects.requireNonNull(PATHS.get(id));return path[Math.clamp(tick,0,path.length-1)];}
}
