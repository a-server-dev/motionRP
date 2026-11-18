package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerfectSusanooTest {
    @Test void completeBlueSamuraiIncludesFullLegsTenguWingsAndPairedDetailedBlades() throws Exception {
        JsonObject model;
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_susanoo_armor.bbmodel"))) {
            model=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> names=new HashSet<>();double minX=0,maxX=0,top=0;int feathers=0,nose=0,blades=0;
        for(var raw:model.getAsJsonArray("elements")) {
            var e=raw.getAsJsonObject();String name=e.get("name").getAsString();names.add(name);
            if(name.startsWith("wing_feather")) feathers++;if(name.startsWith("tengu_long_nose")) nose++;if(name.startsWith("katana_blade")) blades++;
            minX=Math.min(minX,e.getAsJsonArray("from").get(0).getAsDouble());maxX=Math.max(maxX,e.getAsJsonArray("to").get(0).getAsDouble());top=Math.max(top,e.getAsJsonArray("to").get(1).getAsDouble());
        }
        assertTrue(feathers>=180 && nose>=8 && blades>=36);assertTrue(top*2/16>14 && (maxX-minX)*2/16>20);
        assertTrue(names.stream().anyMatch(n->n.startsWith("shin_lamella")) && names.stream().anyMatch(n->n.startsWith("claw")));
        var animations=model.getAsJsonArray("animations");assertEquals("unfurl",animations.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals((SusanooTimeline.READY-SusanooTimeline.ARMOR)/20.0,animations.get(1).getAsJsonObject().get("length").getAsDouble());
        byte[] png=Base64.getDecoder().decode(model.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString().split(",")[1]);
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));int rgb=image.getRGB(5*16+1,1);
        assertTrue((rgb&255)>((rgb>>16)&255),"Chakra armor must be blue");
    }
}
