package dev.aserver.motionrp.core;
import java.util.Set;
public final class EpicTimeline {
 public static final Set<String> EFFECTS=Set.of("road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei");
 private EpicTimeline(){}
 public static int end(String effect){if(!EFFECTS.contains(effect))throw new IllegalArgumentException(effect);return switch(effect){case "kokuryuha"->164;case "road_roller"->244;case "chibaku_tensei"->260;default->220;};}
 public static String phase(String effect,int tick){if(tick<0||tick>=end(effect))return "FINISHED";return switch(effect){
  case "road_roller"->tick<12?"POSE":tick<40?"THE_WORLD":tick<70?"LEAP":tick<104?"DROP":tick<176?"BARRAGE":tick<216?"IMPACT":"FADE";
  case "kokuryuha"->tick<16?"POSE":tick<50?"CHARGE":tick<96?"DRAGON":tick<132?"IMPACT":"FADE";
  case "kokujo_tengen_myoo"->tick<16?"POSE":tick<54?"SUMMON":tick<124?"WINDUP":tick<156?"SLASH":tick<196?"IMPACT":"FADE";
  default->tick<28?"SEAL":tick<68?"CORE":tick<200?"ATTRACTION":tick<236?"MOON":"FADE";};}
 public static double rollerHeight(int tick){return tick<70?16:Math.max(.15,16*(1-Math.pow(Math.clamp((tick-70)/34.0,0,1),2)));}
 public static double[] rockNormal(int index){if(index<0||index>=96)throw new IllegalArgumentException("rock");double y=1-2*(index+.5)/96.0,a=index*2.399963229728653,r=Math.sqrt(1-y*y);return new double[]{r*Math.cos(a),y,r*Math.sin(a)};}
 public static double dragonDistance(int tick){return Math.clamp((tick-50)*.8,0,36);}
 public static double moonScale(int tick){return 10*Math.clamp((tick-68)/132.0,0,1);}
}
