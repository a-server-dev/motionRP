package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyCorrectionsTest {
    private JsonObject model(String id)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+id+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
    @Test void returnFollowsTheImpactInsteadOfTheThrow(){assertTrue(DragonBallTimeline.SPIRIT_RETURN>DragonBallTimeline.SPIRIT_IMPACT);assertTrue(DragonBallTimeline.SPIRIT_RETURN<DragonBallTimeline.SPIRIT_FADE);}
    @Test void theLeftElbowAndForearmStayInFrontOfTheChestDuringTheCharge()throws Exception{
        var m=model("players/motionrp_kamehameha");var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(m.getAsJsonArray("outliner"),null,parents);var a=m.getAsJsonArray("animations").get(0).getAsJsonObject();
        for(int tick=16;tick<=50;tick++){
            var elbow=math.posedOrigin(names.get("plfa_left_forearm"),tick/20.,a,groups,parents);var palm=math.posedOrigin(names.get("pli_left_item"),tick/20.,a,groups,parents);
            for(int i=0;i<=10;i++)assertTrue(elbow[2]+(palm[2]-elbow[2])*i/10.<-4,"Left forearm must clear the torso's front face");
        }
    }
    @Test void chargingParticlesClimbTheBladeAndRotateWithTheDownwardCut(){
        var at70=SwordChargePath.samples(70);var at86=SwordChargePath.samples(86);
        assertTrue(at86.stream().mapToDouble(SwordChargePath.Point::height).max().orElseThrow()>NobleHandPath.at(true,86).height()+15);
        assertNotEquals(at70,at86);
        var hand=NobleHandPath.at(true,92);assertTrue(SwordChargePath.samples(92).stream().anyMatch(p->p.forward()>hand.forward()+10));
    }
    @Test void thePillarIsMadeOfThinRibbonsInsteadOfSolidSlabs()throws Exception{
        var m=model("models/motionrp_excalibur_pillar");
        for(var raw:m.getAsJsonArray("elements")){
            var e=raw.getAsJsonObject();assertEquals("mesh",e.get("type").getAsString());var v=e.getAsJsonObject("vertices");
            for(var entry:v.entrySet())if(entry.getKey().startsWith("a")){var a=entry.getValue().getAsJsonArray();var b=v.getAsJsonArray("b"+entry.getKey().substring(1));double d=0;for(int i=0;i<3;i++)d+=Math.pow(a.get(i).getAsDouble()-b.get(i).getAsDouble(),2);assertTrue(Math.sqrt(d)<.13,"No broad pillar face");}
        }
    }
    @Test void meshTrianglesHaveValidGeometryAndNondegenerateTextureCoordinates()throws Exception{
        for(String id:List.of("genki_dama_orb","kamehameha_orb","excalibur_pillar")){
            var m=model("models/motionrp_"+id);
            for(var raw:m.getAsJsonArray("elements")){
                var e=raw.getAsJsonObject();if(!e.get("type").getAsString().equals("mesh"))continue;
                for(var face:e.getAsJsonObject("faces").entrySet()){
                    var f=face.getValue().getAsJsonObject();var keys=f.getAsJsonArray("vertices");assertEquals(3,keys.size());
                    double[][] pts=new double[3][3],uv=new double[3][2];
                    for(int i=0;i<3;i++){String key=keys.get(i).getAsString();for(int j=0;j<3;j++)pts[i][j]=e.getAsJsonObject("vertices").getAsJsonArray(key).get(j).getAsDouble();for(int j=0;j<2;j++)uv[i][j]=f.getAsJsonObject("uv").getAsJsonArray(key).get(j).getAsDouble();}
                    double[] a=new double[3],b=new double[3];for(int j=0;j<3;j++){a[j]=pts[1][j]-pts[0][j];b[j]=pts[2][j]-pts[0][j];}
                    double area=Math.pow(a[1]*b[2]-a[2]*b[1],2)+Math.pow(a[2]*b[0]-a[0]*b[2],2)+Math.pow(a[0]*b[1]-a[1]*b[0],2);assertTrue(area>1e-8);
                    assertTrue(Math.abs((uv[1][0]-uv[0][0])*(uv[2][1]-uv[0][1])-(uv[1][1]-uv[0][1])*(uv[2][0]-uv[0][0]))>.01);
                }
            }
        }
    }
}
