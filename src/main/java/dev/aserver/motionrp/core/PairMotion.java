package dev.aserver.motionrp.core;
import java.util.*;
/** Public paired item kinds. The postponed final battle is deliberately not exposed. */
public enum PairMotion {
 FUSION("fusion","pair_fusion"),BROLY("broly","pair_broly");
 private final String command,effect;
 PairMotion(String command,String effect){this.command=command;this.effect=effect;}
 public String command(){return command;}
 public String effect(){return effect;}
 public int duration(){return this==FUSION?FusionTimeline.END:BrolyTimeline.END;}
 public static Optional<PairMotion> fromEffect(String effect){return Arrays.stream(values()).filter(p->p.effect.equals(effect)).findFirst();}
}
