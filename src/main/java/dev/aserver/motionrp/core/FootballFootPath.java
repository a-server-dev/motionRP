package dev.aserver.motionrp.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Boot and calf samples baked from the actual player rig; attachments follow the raised leg. */
public final class FootballFootPath {
 public record Point(double right,double forward,double height){}
 public record Frame(Point boot,Point calf){}
 private static final Frame[] FIRE=read("fire_tornado",true),PENGUIN=read("emperor_penguin_1",false);
 private FootballFootPath(){}
 private static Frame[] read(String name,boolean fire){
  try(var in=Objects.requireNonNull(FootballFootPath.class.getResourceAsStream("/bettermodel/"+name+"-boot.csv"));var r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
   Frame[] rows=r.lines().map(line->{double[] p=Arrays.stream(line.split(",")).mapToDouble(Double::parseDouble).toArray();if(p.length!=6||Arrays.stream(p).anyMatch(v->!Double.isFinite(v)))throw new IllegalStateException("Invalid football attachment");return new Frame(new Point(p[0],p[1],p[2]),new Point(p[3],p[4],p[5]));}).toArray(Frame[]::new);
   if(rows.length!=FootballTimeline.end(fire))throw new IllegalStateException("Football attachment duration mismatch");return rows;
  }catch(IOException e){throw new UncheckedIOException(e);}
 }
 public static Frame at(boolean fire,int tick){var rows=fire?FIRE:PENGUIN;return rows[Math.clamp(tick,0,rows.length-1)];}
}
