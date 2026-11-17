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
        for (String file : List.of("players/motionrp_rasengan.bbmodel", "models/motionrp_rasengan_orb.bbmodel",
                "players/motionrp_rasenshuriken.bbmodel", "models/motionrp_rasenshuriken_orb.bbmodel")) {
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

    @Test void shurikenIsRaisedOverheadThenThrownAndHasFourWideBlades() throws Exception {
        var model=read("players/motionrp_rasenshuriken.bbmodel");
        var animations=model.getAsJsonArray("animations");
        assertEquals("rasenshuriken",animations.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(8,animations.get(0).getAsJsonObject().get("length").getAsDouble());
        assertEquals((RasenshurikenTimeline.RAISE-RasenshurikenTimeline.CLONES)/20.0,
                animations.get(1).getAsJsonObject().get("length").getAsDouble());
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> parents=new HashMap<>();String wrist=null;
        for (var g:model.getAsJsonArray("groups")) {
            var group=g.getAsJsonObject();String id=group.get("uuid").getAsString();groups.put(id,group);
            if (group.get("name").getAsString().equals("pri_right_item")) wrist=id;
        }
        parentTree(model.getAsJsonArray("outliner"),null,parents);
        for (int tick=0;tick<RasenshurikenTimeline.END;tick++) {
            var at=posedOrigin(wrist,tick/20.0,animations.get(0).getAsJsonObject(),groups,parents);
            var path=RasenganHandPath.shurikenAt(tick);
            assertEquals(at[0]/16,path.right(),1e-7);assertEquals(-at[2]/16+.05,path.forward(),1e-7);
            assertEquals(at[1]/16+.03,path.height(),1e-7);
        }
        assertTrue(RasenganHandPath.shurikenAt(80).height()>1.9,"Weapon not overhead");
        assertTrue(RasenganHandPath.shurikenAt(114).height()>1.9,"Weapon lowered before release");
        assertTrue(RasenganHandPath.shurikenAt(119).forward()>.4,"Throw arm did not reach forward");
        var orb=read("models/motionrp_rasenshuriken_orb.bbmodel");
        assertEquals(5,orb.getAsJsonArray("groups").size());
        var blades=orb.getAsJsonArray("outliner").get(4).getAsJsonObject().getAsJsonArray("children");
        assertEquals(28,blades.size());
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
        for (double time : new double[]{3.4, 3.55, 3.6}) {
            var right = posedOrigin(names.get("pri_right_item"), time, animations.get(0).getAsJsonObject(), groups, parents);
            var left = posedOrigin(names.get("pli_left_item"), time, animations.get(0).getAsJsonObject(), groups, parents);
            assertTrue(right[2] > 3, "Windup hand must be behind torso: " + Arrays.toString(right));
            assertTrue(left[0] < -8, "Left guard must extend to the side: " + Arrays.toString(left));
        }
        for (int tick = 0; tick < RasenganTimeline.END; tick++) {
            var wrist = posedOrigin(names.get("pri_right_item"), tick / 20.0,
                    animations.get(0).getAsJsonObject(), groups, parents);
            var hand = RasenganHandPath.at(tick);
            assertEquals(wrist[0] / 16, hand.right(), 1e-7);
            assertEquals(-wrist[2] / 16 + .05, hand.forward(), 1e-7);
            assertEquals(wrist[1] / 16 + .03, hand.height(), 1e-7);
        }
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

    @Test void crouchedStanceLowersHipsBendsKneesBackAndKeepsFeetOutsideHips() throws Exception {
        var model = read("players/motionrp_rasengan.bbmodel");
        Map<String, JsonObject> groups = new HashMap<>();
        Map<String, String> names = new HashMap<>(), parents = new HashMap<>();
        for (var node : model.getAsJsonArray("groups")) {
            var group = node.getAsJsonObject();
            String id = group.get("uuid").getAsString();
            groups.put(id, group); names.put(group.get("name").getAsString(), id);
        }
        parentTree(model.getAsJsonArray("outliner"), null, parents);
        for (var raw : model.getAsJsonArray("animations")) {
            var animation = raw.getAsJsonObject();
            for (double time : new double[]{.8, 2.0}) {
                var hip = posedOrigin(names.get("phip_hip"), time, animation, groups, parents);
                assertTrue(hip[1] < 10, "Hips were not lowered");
                for (boolean right : new boolean[]{true, false}) {
                    String thigh = names.get(right ? "prl_right_leg" : "pll_left_leg");
                    String shin = names.get(right ? "prfl_right_foreleg" : "plfl_left_foreleg");
                    double[] thighOrigin = vector(groups.get(thigh).getAsJsonArray("origin"));
                    var knee = posedOrigin(shin, time, animation, groups, parents);
                    var foot = posedPoint(shin, new double[]{thighOrigin[0], 0, 0}, time, animation, groups, parents);
                    assertTrue(knee[2] < hip[2] - 2, "Knee did not bend forward");
                    assertTrue(foot[2] > knee[2] + 2, "Lower leg bent the wrong way");
                    assertTrue(right ? foot[0] > thighOrigin[0] : foot[0] < thighOrigin[0], "Foot turned inward");
                    assertTrue(foot[1] > -.6 && foot[1] < 1, "Foot off ground: " + Arrays.toString(foot));
                }
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
        return posedPoint(id, vector(groups.get(id).getAsJsonArray("origin")), time, animation, groups, parents);
    }

    private double[] posedPoint(String id, double[] point, double time, JsonObject animation, Map<String, JsonObject> groups, Map<String, String> parents) {
        for (String bone = id; bone != null; bone = parents.get(bone)) {
            double[] origin = vector(groups.get(bone).getAsJsonArray("origin"));
            double[] angle = sample(animation, bone, "rotation", time);
            for (int i = 0; i < 3; i++) angle[i] = Math.toRadians(angle[i]);
            double[] translation = sample(animation, bone, "position", time);
            double x = point[0] - origin[0], y = point[1] - origin[1], z = point[2] - origin[2];
            double y1 = y * Math.cos(angle[0]) - z * Math.sin(angle[0]);
            double z1 = y * Math.sin(angle[0]) + z * Math.cos(angle[0]);
            double x2 = x * Math.cos(angle[1]) + z1 * Math.sin(angle[1]);
            double z2 = -x * Math.sin(angle[1]) + z1 * Math.cos(angle[1]);
            point = new double[]{translation[0] + origin[0] + x2 * Math.cos(angle[2]) - y1 * Math.sin(angle[2]),
                    translation[1] + origin[1] + x2 * Math.sin(angle[2]) + y1 * Math.cos(angle[2]), translation[2] + origin[2] + z2};
        }
        return point;
    }

    private double[] sample(JsonObject animation, String bone, String channel, double time) {
        var raw = animation.getAsJsonObject("animators").get(bone);
        if (raw == null) return new double[3];
        var frames = new ArrayList<JsonObject>();
        for (var frame : raw.getAsJsonObject().getAsJsonArray("keyframes")) {
            var value = frame.getAsJsonObject();
            if (value.get("channel").getAsString().equals(channel)) frames.add(value);
        }
        if (frames.isEmpty()) return new double[3];
        JsonObject before = frames.getFirst(), after = frames.getLast();
        for (var frame : frames) {
            if (frame.get("time").getAsDouble() <= time) before = frame;
            if (frame.get("time").getAsDouble() >= time) { after = frame; break; }
        }
        double t0 = before.get("time").getAsDouble(), t1 = after.get("time").getAsDouble();
        double blend = t1 == t0 ? 0 : Math.clamp((time - t0) / (t1 - t0), 0, 1);
        var a = before.getAsJsonArray("data_points").get(0).getAsJsonObject();
        var b = after.getAsJsonArray("data_points").get(0).getAsJsonObject();
        double[] value = new double[3];
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) value[i] = a.get(axes[i]).getAsDouble() * (1 - blend) + b.get(axes[i]).getAsDouble() * blend;
        return value;
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
