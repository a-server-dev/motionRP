package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RasenganAssetsTest {
    private JsonObject read(String file) throws IOException {
        try (var in = Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/" + file));
             var reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void allAnimatedBonesAndOutlinerEntriesResolveToRealModelParts() throws Exception {
        for (String file : List.of("players/motionrp_rasengan.bbmodel", "models/motionrp_rasengan_orb.bbmodel")) {
            var model = read(file);
            Set<String> groups = new HashSet<>();
            Set<String> all = new HashSet<>();
            for (var group : model.getAsJsonArray("groups")) {
                String id = group.getAsJsonObject().get("uuid").getAsString();
                assertTrue(groups.add(id)); assertTrue(all.add(id));
            }
            for (var element : model.getAsJsonArray("elements")) assertTrue(all.add(element.getAsJsonObject().get("uuid").getAsString()));
            checkTree(model.getAsJsonArray("outliner"), all);
            for (var raw : model.getAsJsonArray("animations")) {
                var animation = raw.getAsJsonObject();
                double length = animation.get("length").getAsDouble();
                for (var entry : animation.getAsJsonObject("animators").entrySet()) {
                    assertTrue(groups.contains(entry.getKey()), "Missing animated bone " + entry.getKey());
                    double previous = -1;
                    for (var frame : entry.getValue().getAsJsonObject().getAsJsonArray("keyframes")) {
                        double time = frame.getAsJsonObject().get("time").getAsDouble();
                        assertTrue(time >= 0 && time <= length && time >= previous);
                        previous = time;
                    }
                }
            }
        }
    }

    @Test void bodyAndHelperAnimationDurationsMatchSceneClockAndOrbLoops() throws Exception {
        var player = read("players/motionrp_rasengan.bbmodel");
        var animations = player.getAsJsonArray("animations");
        assertEquals("rasengan", animations.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(RasenganTimeline.END / 20.0, animations.get(0).getAsJsonObject().get("length").getAsDouble());
        assertEquals("rasengan_helper", animations.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals((RasenganTimeline.CLONE_END - RasenganTimeline.CLONE_START) / 20.0,
                animations.get(1).getAsJsonObject().get("length").getAsDouble());
        var spin = read("models/motionrp_rasengan_orb.bbmodel").getAsJsonArray("animations").get(0).getAsJsonObject();
        assertEquals("spin", spin.get("name").getAsString());
        assertEquals("loop", spin.get("loop").getAsString());
    }

    @Test void orbContainsEmbeddedValidPaletteAndThreeSeparateSwirlRings() throws Exception {
        var orb = read("models/motionrp_rasengan_orb.bbmodel");
        assertEquals(4, orb.getAsJsonArray("groups").size());
        String source = orb.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
        assertTrue(source.startsWith("data:image/png;base64,"));
        var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1))));
        assertNotNull(image);
        assertEquals(4, image.getWidth());
        assertEquals(4, image.getHeight());
        assertTrue(orb.getAsJsonArray("elements").size() >= 60);
    }

    private void checkTree(JsonArray nodes, Set<String> all) {
        for (var node : nodes) {
            if (node.isJsonPrimitive()) assertTrue(all.contains(node.getAsString()));
            else {
                var group = node.getAsJsonObject();
                assertTrue(all.contains(group.get("uuid").getAsString()));
                checkTree(group.getAsJsonArray("children"), all);
            }
        }
    }
}
