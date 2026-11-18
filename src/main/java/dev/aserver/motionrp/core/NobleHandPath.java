package dev.aserver.motionrp.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NobleHandPath {
    private static final RasenganHandPath.Hand[] EXCALIBUR=read("excalibur",176),GAE=read("gae_bolg",GaeBolgTimeline.END);
    private NobleHandPath() {}
    private static RasenganHandPath.Hand[] read(String name,int length) {
        try(var in=Objects.requireNonNull(NobleHandPath.class.getResourceAsStream("/bettermodel/"+name+"-hand-path.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))) {
            var result=r.lines().map(s->{var v=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(result.length!=length)throw new IllegalStateException("Noble attachment duration mismatch");
            for(var p:result)if(!Double.isFinite(p.right()+p.forward()+p.height()))throw new IllegalStateException("Invalid Noble attachment");
            return result;
        } catch(IOException e) {throw new UncheckedIOException(e);}
    }
    public static RasenganHandPath.Hand at(boolean excalibur,int tick) {var path=excalibur?EXCALIBUR:GAE;return path[Math.clamp(tick,0,path.length-1)];}
}
