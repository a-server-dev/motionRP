package dev.aserver.motionrp.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Attachment baked from the exact spread/key/Ea player rig. */
public final class TreasuryHandPath {
    private static final RasenganHandPath.Hand[] PATH;
    static {
        try(var reader=new BufferedReader(new InputStreamReader(Objects.requireNonNull(TreasuryHandPath.class.getResourceAsStream("/bettermodel/gate_of_babylon_enuma-hand-path.csv")),StandardCharsets.UTF_8))) {
            PATH=reader.lines().map(line->{var p=line.split(",");return new RasenganHandPath.Hand(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(PATH.length!=TreasuryTimeline.SERIOUS_END) throw new IllegalStateException("Ea attachment duration mismatch");
            for(var p:PATH) if(!Double.isFinite(p.right()+p.forward()+p.height())) throw new IllegalStateException("Invalid Ea attachment");
        } catch(IOException failure) { throw new UncheckedIOException(failure); }
    }
    private TreasuryHandPath() {}
    public static RasenganHandPath.Hand at(int tick) { return PATH[Math.clamp(tick,0,PATH.length-1)]; }
}
