package dev.aserver.motionrp.core;
import com.google.gson.*;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class BrolyAssetsTest {
 private JsonObject load(String file)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void boundedFlightCapsAt24BlocksAndKeepsABufferBeforeTheHit(){
  double d=0;for(int i=0;i<100;i++){double next=BrolyTimeline.nextDistance(d);assertTrue(next>=d&&next-d<=1.5);d=next;}assertEquals(24,d);assertEquals(0,BrolyTimeline.safeTravel(1.5,.02));assertEquals(.42,BrolyTimeline.safeTravel(1.5,.5),1e-9);assertEquals(1.5,BrolyTimeline.safeTravel(1.5,8));
 }
 @Test void earlyAndDistantImpactsHaveTheirOwnPhaseClock(){
  assertEquals("GRAB",BrolyTimeline.phase(20,-1));assertEquals("FLIGHT",BrolyTimeline.phase(32,-1));for(int impact:new int[]{32,40,47}){assertEquals("IMPACT",BrolyTimeline.phase(impact,impact));assertEquals("RECOVER",BrolyTimeline.phase(impact+20,impact));assertTrue(impact+20<BrolyTimeline.END);}
 }
 @Test void allPhaseAnimationsPreserveTheIndependentClockBone()throws Exception{
  for(String role:List.of("attacker","victim")){var m=load("players/motionrp_broly_"+role);String root=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("player_root")).findFirst().orElseThrow().get("uuid").getAsString();Set<String> clips=new HashSet<>();for(var raw:m.getAsJsonArray("animations")){var a=raw.getAsJsonObject();String name=a.get("name").getAsString();clips.add(name);assertTrue(a.get("override").getAsBoolean());if(name.startsWith("broly_")){assertEquals(6,a.get("length").getAsDouble());assertTrue(a.getAsJsonObject("animators").has(root));}else assertFalse(a.getAsJsonObject("animators").has(root),"Phase changes must not replace the six-second recovery clock");}assertTrue(clips.containsAll(Set.of("grapple","flight","impact","recover")));}
 }
 @Test void brolyInvitationsRequireTheirOwnAcceptance(){var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"broly"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertTrue(sessions.accept(b,a,1,"finalbattle").isEmpty());assertEquals("broly",sessions.accept(b,a,2,"broly").orElseThrow().kind());}
 @Test void cosmeticWallAndCracksHaveRealGeometry()throws Exception{for(String id:List.of("wall","crater")){var m=load("models/motionrp_broly_"+id);assertFalse(m.getAsJsonArray("elements").isEmpty());assertFalse(m.getAsJsonArray("outliner").isEmpty());}}
}
