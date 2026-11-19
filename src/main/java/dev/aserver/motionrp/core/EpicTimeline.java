package dev.aserver.motionrp.core;
import java.util.Set;
public final class EpicTimeline {
 public static final Set<String> EFFECTS=Set.of("road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei");
 private EpicTimeline(){}
 public static int end(String effect){if(!EFFECTS.contains(effect))throw new IllegalArgumentException(effect);return switch(effect){case "kokuryuha"->164;case "road_roller"->260;case "chibaku_tensei"->312;default->220;};}
 public static String phase(String effect,int tick){if(tick<0||tick>=end(effect))return "FINISHED";return switch(effect){
  case "road_roller"->tick<12?"POSE":tick<40?"THE_WORLD":tick<68?"LEAP":tick<82?"CARRY":tick<104?"DROP":tick<168?"BARRAGE":tick<192?"FINISHER":tick<232?"IMPACT":"FADE";
  case "kokuryuha"->tick<16?"POSE":tick<50?"CHARGE":tick<96?"DRAGON":tick<132?"IMPACT":"FADE";
  case "kokujo_tengen_myoo"->tick<16?"POSE":tick<54?"SUMMON":tick<124?"WINDUP":tick<156?"SLASH":tick<196?"IMPACT":"FADE";
  default->tick<28?"SEAL":tick<68?"CORE":tick<240?"ATTRACTION":tick<288?"MOON":"FADE";};}
 public static final int ROCK_COUNT=144;
 public static double rollerHeight(int tick){return tick<82?18.2:Math.max(.15,18.2*(1-Math.pow(Math.clamp((tick-82)/22.0,0,1),2)));}
 public static int rockLayer(int index){if(index<0||index>=ROCK_COUNT)throw new IllegalArgumentException("rock");return index<24?0:index<72?1:2;}
 public static double rockRadius(int index){return switch(rockLayer(index)){case 0->1.2;case 1->3.9;default->6.6;};}
 public static double rockScale(int index){return switch(rockLayer(index)){case 0->2.8;case 1->4.0;default->4.8;};}
 public static double[] rockNormal(int index){int layer=rockLayer(index),count=24*(layer+1),offset=layer==0?0:layer==1?24:72;double y=1-2*(index-offset+.5)/count,a=index*2.399963229728653,r=Math.sqrt(1-y*y);return new double[]{r*Math.cos(a),y,r*Math.sin(a)};}
 public static double dragonDistance(int tick){return Math.clamp((tick-50)*.8,0,36);}
}
