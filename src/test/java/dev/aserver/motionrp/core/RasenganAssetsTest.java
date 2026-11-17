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

    @Test void casterAndHelperHandsReachInFrontOfTheBodyRatherThanBehindItsBack() throws Exception {
        var model = read("players/motionrp_rasengan.bbmodel");
        Map<String, JsonObject> groups = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        for (var node : model.getAsJsonArray("groups")) {
            var group = node.getAsJsonObject();
            String id = group.get("uuid").getAsString();
            groups.put(id, group); names.put(group.get("name").getAsString(), id);
        }
        parentTree(model.getAsJsonArray("outliner"), null, parents);
        var animations = model.getAsJsonArray("animations");
        // This rig faces -Z. Forward wrist position is an observable geometry
        // property, independent of whether any particular keyframe sign is used.
        for (double time : new double[]{1.0, 2.0, 3.8, 4.1}) {
            double[] hand = posedOrigin(names.get("pri_right_item"), time,
                    animations.get(0).getAsJsonObject(), groups, parents);
            assertTrue(hand[2] < -5, "Caster wrist behind torso at " + time + ": " + Arrays.toString(hand));
        }
        for (String wrist : List.of("pri_right_item", "pli_left_item")) {
            for (double time : new double[]{.4, 1.0, 1.6, 2.2}) {
                double[] hand = posedOrigin(names.get(wrist), time,
                        animations.get(1).getAsJsonObject(), groups, parents);
                assertTrue(hand[2] < -4, "Helper wrist behind torso: " + wrist + " at " + time);
            }
        }
    }

    private void parentTree(JsonArray nodes, String parent, Map<String, String> parents) {
        for (var node : nodes) if (node.isJsonObject()) {
            var group = node.getAsJsonObject();
            String id = group.get("uuid").getAsString();
            if (parent != null) parents.put(id, parent);
            parentTree(group.getAsJsonArray("children"), id, parents);
        }
    }

    private double[] posedOrigin(String id, double time, JsonObject animation, Map<String, JsonObject> groups, Map<String, String> parents) {
        double[] point = vector(groups.get(id).getAsJsonArray("origin"));
        for (String bone = id; bone != null; bone = parents.get(bone)) {
            double[] origin = vector(groups.get(bone).getAsJsonArray("origin"));
            JsonElement animator = animation.getAsJsonObject("animators").get(bone);
            if (animator == null) continue;
            var frames = animator.getAsJsonObject().getAsJsonArray("keyframes");
            JsonObject before = frames.get(0).getAsJsonObject(), after = before;
            for (var frame : frames) {
                var value = frame.getAsJsonObject();
                if (!value.get("channel").getAsString().equals("rotation")) continue;
                if (value.get("time").getAsDouble() <= time) before = value;
                after = value;
                if (value.get("time").getAsDouble() >= time) break;
            }
            double t0 = before.get("time").getAsDouble(), t1 = after.get("time").getAsDouble();
            double blend = t1 == t0 ? 0 : Math.clamp((time - t0) / (t1 - t0), 0, 1);
            var a = before.getAsJsonArray("data_points").get(0).getAsJsonObject();
            var b = after.getAsJsonArray("data_points").get(0).getAsJsonObject();
            double[] angle = new double[3];
            String[] axes = {"x", "y", "z"};
            for (int i = 0; i < 3; i++) angle[i] = Math.toRadians(a.get(axes[i]).getAsDouble() * (1 - blend) + b.get(axes[i]).getAsDouble() * blend);
            double x = point[0] - origin[0], y = point[1] - origin[1], z = point[2] - origin[2];
            double y1 = y * Math.cos(angle[0]) - z * Math.sin(angle[0]);
            double z1 = y * Math.sin(angle[0]) + z * Math.cos(angle[0]);
            double x2 = x * Math.cos(angle[1]) + z1 * Math.sin(angle[1]);
            double z2 = -x * Math.sin(angle[1]) + z1 * Math.cos(angle[1]);
            point = new double[]{origin[0] + x2 * Math.cos(angle[2]) - y1 * Math.sin(angle[2]),
                    origin[1] + x2 * Math.sin(angle[2]) + y1 * Math.cos(angle[2]), origin[2] + z2};
        }
        return point;
    }

    private double[] vector(JsonArray values) {
        return new double[]{values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble()};
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
