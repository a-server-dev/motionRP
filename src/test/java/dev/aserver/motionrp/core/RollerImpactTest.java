package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RollerImpactTest {
 @Test void joltsAreBoundedAlternatingAndDoNotDriftOutsideHits(){
  for(int t=0;t<270;t++){var p=EpicTimeline.rollerJolt(t);assertTrue(Math.abs(p.right())<=.16&&Math.abs(p.forward())<=.09&&p.up()>=0&&p.up()<=.18);if((t<104||t>=168)&&(t<192||t>=198))assertEquals(new EpicTimeline.RollerJolt(0,0,0,0,0),p);}
  assertTrue(EpicTimeline.rollerJolt(104).right()<0&&EpicTimeline.rollerJolt(106).right()>0);assertTrue(EpicTimeline.rollerJolt(105).up()<EpicTimeline.rollerJolt(104).up());assertEquals(1,EpicTimeline.rollerDent(166));
 }
 @Test void everyPunchLeavesAPermanentDentAfterItsRecoil()throws Exception{
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_road_roller.bbmodel"))){var m=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");assertEquals(3,tracks.size());for(var raw:tracks.entrySet()){double last=1;int hits=0;for(var k:raw.getValue().getAsJsonObject().getAsJsonArray("keyframes")){var f=k.getAsJsonObject();double t=f.get("time").getAsDouble();if(!f.get("channel").getAsString().equals("position")||t>=3.2||Math.abs((t*10)%1-.45)>.00001)continue;double y=f.getAsJsonArray("data_points").get(0).getAsJsonObject().get("y").getAsDouble();assertTrue(y<last);last=y;hits++;}assertEquals(32,hits);assertTrue(last<=-2.3);}}
 }
}
