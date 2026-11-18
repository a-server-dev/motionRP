package dev.aserver.motionrp.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class BankaiHandPath {
    private static final RasenganHandPath.Hand[] PATH;
    static {
        try(var input=Objects.requireNonNull(BankaiHandPath.class.getResourceAsStream("/bettermodel/zangetsu_bankai-hand-path.csv"));
            var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            PATH=reader.lines().map(s->{var p=s.split(",");return new RasenganHandPath.Hand(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]));}).toArray(RasenganHandPath.Hand[]::new);
            if(PATH.length!=BankaiTimeline.END) throw new IllegalStateException("Bankai attachment duration mismatch");
        } catch(IOException failure) { throw new UncheckedIOException(failure); }
    }
    private BankaiHandPath() {}
    public static RasenganHandPath.Hand at(int tick) { return PATH[Math.clamp(tick,0,PATH.length-1)]; }
}
