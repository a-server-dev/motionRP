package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PoseSocketTest {
    @Test void thighsStayConnectedToTheirHipSocketsAndBothFeetStayOnTheGround() throws Exception {
        for(String name:List.of("gate_of_babylon_enuma","zangetsu_bankai")) {
            JsonObject model;
            try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_"+name+".bbmodel"))) {
                model=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            }
            var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
            for(var raw:model.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
            math.parentTree(model.getAsJsonArray("outliner"),null,parents);var animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
            int end=(int)Math.round(animation.get("length").getAsDouble()*20);
            for(int tick=0;tick<end;tick+=2) for(boolean right:new boolean[]{false,true}) {
                double x=right?1.875:-1.875,time=tick/20.0;String thigh=names.get(right?"prl_right_leg":"pll_left_leg");
                var actual=math.posedOrigin(thigh,time,animation,groups,parents);
                var socket=math.posedPoint(parents.get(thigh),new double[]{x,11.25,0},time,animation,groups,parents);
                assertArrayEquals(socket,actual,1e-6,name+" detached hip at tick "+tick);
                var foot=math.posedPoint(names.get(right?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{x,0,0},time,animation,groups,parents);
                assertEquals(0,foot[1],.025,name+" foot floated/clipped at tick "+tick);
            }
        }
    }
}
