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

    @Test void maskRevealsDiagonallyAndHasWhiteRedEyeDetails() throws Exception {
        var mask=read("models/motionrp_hollow_mask.bbmodel");
        assertEquals(8,mask.getAsJsonArray("elements").size());
        double previousRank=-1;
        for(var element:mask.getAsJsonArray("elements")){
            var mesh=element.getAsJsonObject();double x=0,y=0;int n=0;
            for(var vertex:mesh.getAsJsonObject("vertices").entrySet()){
                var v=vertex.getValue().getAsJsonArray();x+=v.get(0).getAsDouble();y+=v.get(1).getAsDouble();n++;
            }
            double rank=(4.3-x/n)/8.6+(8-y/n)/8.3;assertTrue(rank>previousRank,"Reveal must move from right forehead to left jaw");previousRank=rank;
        }
        var image=ImageIO.read(getClass().getResourceAsStream("/bettermodel/textures/motionrp_hollow_mask.png"));
        int white=0,red=0,dark=0,clear=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int c=image.getRGB(x,y),r=c>>>16&255,g=c>>>8&255,b=c&255;
            if((c>>>24)==0){clear++;continue;}
            if(r>180&&g>180&&b>180)white++;
            if(r>130&&g<50&&b<80)red++;
            if(r<50&&g<50&&b<50)dark++;
        }
        assertTrue(white>5000&&red>1000&&dark>100&&clear>1000);
    }

    @Test void allAnimatedBonesAndOutlinerEntriesResolveToRealModelParts() throws Exception {
        for (String file : List.of("models/motionrp_hollow_mask.bbmodel","players/motionrp_rho_aias.bbmodel","players/motionrp_getsuga_tensho.bbmodel","models/motionrp_rho_aias_shield.bbmodel","models/motionrp_getsuga_crescent.bbmodel","models/motionrp_getsuga_tensa.bbmodel","players/motionrp_performances.bbmodel","players/motionrp_excalibur.bbmodel","players/motionrp_gae_bolg.bbmodel","models/motionrp_excalibur_blade.bbmodel","models/motionrp_excalibur_light.bbmodel","models/motionrp_gae_bolg_spear.bbmodel","models/motionrp_gae_bolg_trail.bbmodel","models/motionrp_gae_bolg_thorns.bbmodel","players/motionrp_zangetsu_bankai.bbmodel", "models/motionrp_zangetsu_cleaver.bbmodel", "models/motionrp_tensa_zangetsu.bbmodel", "models/motionrp_bankai_cocoon.bbmodel", "models/motionrp_bankai_wave.bbmodel", "models/motionrp_bankai_coat.bbmodel", "players/motionrp_rasengan.bbmodel", "models/motionrp_rasengan_orb.bbmodel",
                "players/motionrp_rasenshuriken.bbmodel", "models/motionrp_rasenshuriken_orb.bbmodel",
                "players/motionrp_gate_of_babylon.bbmodel", "players/motionrp_susanoo.bbmodel",
                "models/motionrp_golden_gate.bbmodel", "models/motionrp_treasury_sword.bbmodel",
                "models/motionrp_treasury_spear.bbmodel", "models/motionrp_treasury_axe.bbmodel",
                "models/motionrp_susanoo_ribs.bbmodel", "models/motionrp_susanoo_armor.bbmodel", "models/motionrp_susanoo_slash.bbmodel",
                "players/motionrp_senbonzakura.bbmodel","models/motionrp_senbonzakura_blade.bbmodel","models/motionrp_senbonzakura_petals.bbmodel",
                "models/motionrp_senbonzakura_field_blade.bbmodel","players/motionrp_titan_transform.bbmodel","models/motionrp_titan_lightning.bbmodel","models/motionrp_titan_body.bbmodel",
                "players/motionrp_gate_of_babylon_100.bbmodel","players/motionrp_gate_of_babylon_enuma.bbmodel",
                "models/motionrp_golden_gate_mass.bbmodel","models/motionrp_bab_ilu.bbmodel","models/motionrp_ea.bbmodel","models/motionrp_enuma_vortex.bbmodel","models/motionrp_enuma_charge.bbmodel","models/motionrp_enuma_ring.bbmodel","models/motionrp_enuma_rift.bbmodel")) {
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

    @Test void summonsContainEmbeddedTexturesDifferentWeaponsAndAnAnimatedGiantSword() throws Exception {
        for(String id:List.of("golden_gate","treasury_sword","treasury_spear","treasury_axe","susanoo_ribs","susanoo_armor","susanoo_slash","senbonzakura_blade","senbonzakura_petals","senbonzakura_field_blade","titan_lightning","enuma_charge","enuma_ring","enuma_rift")) {
            var model=read("models/motionrp_"+id+".bbmodel");
            String source=model.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
            var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(source.substring(source.indexOf(',')+1))));
            assertNotNull(image);
            if(id.equals("susanoo_ribs")) {
                assertEquals(128,image.getWidth());assertEquals(16,image.getHeight());
                int holes=0,solid=0;
                for(int y=0;y<16;y++) for(int x=0;x<128;x++) {
                    int alpha=(image.getRGB(x,y)>>>24)&255;
                    if(alpha==0) holes++;else if(alpha==255) solid++;
                }
                assertTrue(holes>80 && solid>1000,"Chakra cutout needs visible holes and solid outlines");
            } else { assertEquals(8,image.getWidth());assertEquals(1,image.getHeight()); }
        }
        Set<Integer> counts=new HashSet<>();
        for(String kind:List.of("sword","spear","axe")) counts.add(read("models/motionrp_treasury_"+kind+".bbmodel").getAsJsonArray("elements").size());
        assertEquals(3,counts.size(),"Weapons should have different geometry");
        var armor=read("models/motionrp_susanoo_armor.bbmodel");
        assertEquals("slash",armor.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals((SusanooTimeline.FADE-SusanooTimeline.READY)/20.0,
                armor.getAsJsonArray("animations").get(0).getAsJsonObject().get("length").getAsDouble());
        double top=0;
        for(var element:armor.getAsJsonArray("elements")) top=Math.max(top,element.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
        assertTrue(top*2/16>5.5,"Upper-body guardian should tower over the caster");
        for(String name:List.of("gate_of_babylon","susanoo","senbonzakura","titan_transform")) {
            var motion=read("players/motionrp_"+name+".bbmodel").getAsJsonArray("animations").get(0).getAsJsonObject();
            assertEquals(name,motion.get("name").getAsString());
            int duration=switch(name) {case "susanoo" -> SusanooTimeline.END;case "senbonzakura" -> SenbonzakuraTimeline.END;case "titan_transform" -> TitanTimeline.END;default -> GateTimeline.END;};
            assertEquals(duration/20.0,motion.get("length").getAsDouble());
        }
    }

    @Test void treasuryArmsStayCrossedInFrontOfTheChestWhileStandingStill() throws Exception {
        var model=read("players/motionrp_gate_of_babylon.bbmodel");
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")) {
            var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);
        }
        parentTree(model.getAsJsonArray("outliner"),null,parents);
        var animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        double[] previousRight=null,previousLeft=null;
        for(double time:new double[]{1,2.5,4}) {
            var right=posedOrigin(names.get("pri_right_item"),time,animation,groups,parents);
            var left=posedOrigin(names.get("pli_left_item"),time,animation,groups,parents);
            assertTrue(right[0]<0 && left[0]>0,"Wrists must cross the body's centerline");
            assertTrue(right[2]<-3 && left[2]<-3,"Crossed arms must remain in front of the chest");
            if(previousRight!=null) { assertArrayEquals(previousRight,right,1e-7);assertArrayEquals(previousLeft,left,1e-7); }
            previousRight=right;previousLeft=left;
        }
    }

    @Test void cherryKatanaHasHandleAboveSteelPointAndUsesMetalInsteadOfPink() throws Exception {
        var model=read("models/motionrp_senbonzakura_blade.bbmodel");
        double gripBottom=0,bladeTop=0,tip=1000,top=0;int wraps=0;
        for(var raw:model.getAsJsonArray("elements")) {
            var e=raw.getAsJsonObject();String name=e.get("name").getAsString();
            double from=e.getAsJsonArray("from").get(1).getAsDouble(),to=e.getAsJsonArray("to").get(1).getAsDouble();
            top=Math.max(top,to);
            if(name.equals("black_grip")) gripBottom=from;
            if(name.startsWith("steel_")) { bladeTop=Math.max(bladeTop,to);tip=Math.min(tip,from); }
            if(name.startsWith("hilt_wrap_")) wraps++;
        }
        assertTrue(gripBottom>bladeTop && tip<.1,"Blade must point down below the upper handle");
        assertTrue(top*1.35/16>4,"Standing blades should exceed four blocks");assertTrue(wraps>=7);
        var source=model.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
        var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(source.substring(source.indexOf(',')+1))));
        var metal=new java.awt.Color(image.getRGB(3,0));var handle=new java.awt.Color(image.getRGB(2,0));
        assertTrue(Math.abs(metal.getRed()-metal.getGreen())<25 && Math.abs(metal.getGreen()-metal.getBlue())<25);
        assertTrue(handle.getRed()<50 && handle.getGreen()<50 && handle.getBlue()<50);
    }

    @Test void denseCherryFieldUsesOnlyOutwardCurvingBladeFacesWithoutAnyHilt() throws Exception {
        var model=read("models/motionrp_senbonzakura_field_blade.bbmodel");double top=0,outerTip=0;
        for(var raw:model.getAsJsonArray("elements")) {
            var e=raw.getAsJsonObject();String name=e.get("name").getAsString();
            assertFalse(name.contains("grip") || name.contains("guard") || name.contains("tsuba") || name.contains("pommel"));
            double y=e.getAsJsonArray("to").get(1).getAsDouble();top=Math.max(top,y);
            if(y>50) outerTip=Math.max(outerTip,e.getAsJsonArray("to").get(0).getAsDouble());
        }
        assertTrue(top*1.5/16>4.8);assertTrue(outerTip>5,"The upper blade should sweep toward its outer edge");
    }

    @Test void titanRaisesTheRightHandToTheMouthBeforeTransforming() throws Exception {
        var model=read("players/motionrp_titan_transform.bbmodel");
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")) {
            var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);
        }
        parentTree(model.getAsJsonArray("outliner"),null,parents);
        var animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        for(double time:new double[]{.6,1.2}) {
            double[] hand=posedOrigin(names.get("pri_right_item"),time,animation,groups,parents);
            double[] mouth=posedPoint(names.get("h_ph_head"),new double[]{0,25,-4},time,animation,groups,parents);
            double squared=0;for(int i=0;i<3;i++) squared+=Math.pow(hand[i]-mouth[i],2);
            assertTrue(squared<36,"Right hand missed the mouth: "+Arrays.toString(hand));
        }
        assertEquals("titan_idle",model.getAsJsonArray("animations").get(1).getAsJsonObject().get("name").getAsString());
    }

    @Test void shurikenIsRaisedOverheadThenThrownAndHasFourWideBlades() throws Exception {
        var model=read("players/motionrp_rasenshuriken.bbmodel");
        var animations=model.getAsJsonArray("animations");
        assertEquals("rasenshuriken",animations.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(7.6,animations.get(0).getAsJsonObject().get("length").getAsDouble());
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
        assertTrue(RasenganHandPath.shurikenAt(RasenshurikenTimeline.THROW).height()>1.9,"Weapon lowered before release");
        assertTrue(RasenganHandPath.shurikenAt(111).forward()>.4,"Throw arm did not reach forward");
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

    @Test void celebrationsKeepFeetPlantedAndHaveDistinctSignaturePoses() throws Exception {
        var model=read("players/motionrp_performances.bbmodel");
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        parentTree(model.getAsJsonArray("outliner"),null,parents);
        assertEquals(7,model.getAsJsonArray("animations").size());
        for(var raw:model.getAsJsonArray("animations")){
            var a=raw.getAsJsonObject();var p=Performance.find(a.get("name").getAsString().matches("ronaldo|mbappe|kneeslide|kazu")?"goal":"hr",a.get("name").getAsString()).orElseThrow();
            assertEquals(p.ticks()/20.0,a.get("length").getAsDouble(),.001);
            double highest=0;
            for(int tick=0;tick<=p.ticks();tick++){
                double time=tick/20.0,min=Double.POSITIVE_INFINITY;
                for(boolean right:new boolean[]{true,false}){
                    String thigh=names.get(right?"prl_right_leg":"pll_left_leg"),shin=names.get(right?"prfl_right_foreleg":"plfl_left_foreleg");
                    double x=groups.get(thigh).getAsJsonArray("origin").get(0).getAsDouble();
                    var foot=posedPoint(shin,new double[]{x,0,0},time,a,groups,parents);min=Math.min(min,foot[1]);
                }
                assertTrue(min>-.001,p.id()+" feet below ground at "+tick);highest=Math.max(highest,min);
                if(p!=Performance.RONALDO && p!=Performance.MBAPPE)assertEquals(0,min,.001);
            }
            if(p==Performance.RONALDO)assertEquals(0,highest,.001,"Physical jump must not be doubled in model");
            if(p==Performance.KNEESLIDE)assertTrue(posedOrigin(names.get("phip_hip"),2,a,groups,parents)[1]<6,"Knees not lowered to ground");
            if(p==Performance.MBAPPE){
                var r=posedOrigin(names.get("pri_right_item"),2,a,groups,parents);var l=posedOrigin(names.get("pli_left_item"),2,a,groups,parents);
                assertTrue(r[0]<0 && l[0]>0,"Arms do not cross");assertTrue(r[1]>l[1],"Forearms overlap");assertTrue(r[2]<-4 && l[2]<-4,"Hands buried in chest");
            }
            if(p==Performance.YAMAKAWA)assertTrue(posedOrigin(names.get("phip_hip"),1.4,a,groups,parents)[1]<7,"Dosukoi stance too tall");
            if(p==Performance.MATSUDA){
                for(int frame=30;frame<=78;frame++){
                    var fist=posedOrigin(names.get("pri_right_item"),frame/20.0,a,groups,parents);var head=posedOrigin(names.get("h_ph_head"),frame/20.0,a,groups,parents);
                    assertTrue(fist[1]>head[1]+3,"Atsuo fist must be above the face");
                    assertTrue(fist[2]<head[2]-5,"Atsuo fist went behind the head instead of forward");
                }
                var chest=posedOrigin(names.get("pc_chest"),2,a,groups,parents);var hip=posedOrigin(names.get("phip_hip"),2,a,groups,parents);assertTrue(chest[2]>hip[2]+1,"Torso not arched backwards");
            }
            if(p==Performance.YAMAKAWA)for(int frame=28;frame<=62;frame++){
                double t=frame/20.0;
                var r=posedOrigin(names.get("pri_right_item"),t,a,groups,parents);var l=posedOrigin(names.get("pli_left_item"),t,a,groups,parents);
                var re=posedOrigin(names.get("prfa_right_forearm"),t,a,groups,parents);var le=posedOrigin(names.get("plfa_left_forearm"),t,a,groups,parents);
                var head=posedOrigin(names.get("h_ph_head"),t,a,groups,parents);
                assertTrue(r[0]-l[0]>22,"Dosukoi hands converge above head instead of spreading outward");
                assertTrue(r[0]>re[0]+2 && l[0]<le[0]-2,"Forearms point inward");
                assertTrue(r[1]>head[1]+3 && l[1]>head[1]+3,"Wide V must stay above head");
            }
            if(p==Performance.KAZU){
                assertEquals(4.5,a.get("length").getAsDouble(),.0001,"Kazu choreography should run at double speed");
                for(int cycle=0;cycle<3;cycle++)for(int step=0;step<4;step++){
                    int frame=28+cycle*32+step*8;double[] expected=step==0?new double[]{-4,cycle==0?0:4}:step==1?new double[]{-4,-4}:step==2?new double[]{4,-4}:new double[]{4,4};
                    for(int side=0;side<2;side++){
                        var foot=posedPoint(names.get(side==0?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{side==0?1.875:-1.875,0,0},frame/40.0,a,groups,parents);
                        assertEquals(expected[side],foot[2],.001,"Kazu must step right forward, left forward, right back, left back");
                    }
                }
                var forward=posedOrigin(names.get("pri_right_item"),.7,a,groups,parents);var back=posedOrigin(names.get("pri_right_item"),1.1,a,groups,parents);
                assertTrue(forward[2]>7 && back[2]<-7,"Windmill must pass through front and back");
                var up=posedOrigin(names.get("pri_right_item"),.9,a,groups,parents);var down=posedOrigin(names.get("pri_right_item"),1.3,a,groups,parents);
                assertTrue(up[1]>down[1]+15,"Windmill must make a full shoulder circle");
                var finish=posedOrigin(names.get("pri_right_item"),3.2,a,groups,parents);
                assertTrue(finish[1]>24,"Kazu final pointing pose missing");
                assertArrayEquals(finish,posedOrigin(names.get("pri_right_item"),3.95,a,groups,parents),.001,"Kazu final pose must visibly hold");
                var groin=posedOrigin(names.get("phip_hip"),3.5,a,groups,parents);var left=posedOrigin(names.get("pli_left_item"),3.5,a,groups,parents);
                assertTrue(Math.abs(left[0]-groin[0])<1 && left[1]>groin[1]+.4 && left[1]<groin[1]+2 && left[2]<groin[2]-2,"Kazu finish left hand must rest in front of the groin");
            }
            if(p==Performance.DESTASHA){
                var forward=posedOrigin(names.get("pri_right_item"),.9,a,groups,parents);var down=posedOrigin(names.get("pri_right_item"),1.5,a,groups,parents);
                var hip=posedOrigin(names.get("phip_hip"),1.5,a,groups,parents);
                assertTrue(forward[2]<-9,"Destasha must start with right arm straight forward");
                assertTrue(down[1]<hip[1]+2 && down[2]>forward[2]+8,"Destasha downward sweep missing");
                for(int frame=48;frame<=82;frame++){
                    double t=frame/20.0;var palm=posedOrigin(names.get("pri_right_item"),t,a,groups,parents);var head=posedOrigin(names.get("h_ph_head"),t,a,groups,parents);
                    assertTrue(Math.abs(palm[0])<4 && palm[1]>head[1]+5.5 && palm[1]<head[1]+8 && palm[2]<-5,"Right hand must salute across brow");
                    var left=posedOrigin(names.get("pli_left_item"),t,a,groups,parents);assertEquals(-5.625,left[0],.001);assertEquals(0,left[2],.001);assertTrue(left[1]<12,"Left hand should stay relaxed");
                }
            }
        }
    }

    @Test void newBarrierAndSlashHaveSevenPetalsCurvedEnergyAndRealTensaGeometry() throws Exception {
        var shield=read("models/motionrp_rho_aias_shield.bbmodel");
        long petals=shield.getAsJsonArray("groups").asList().stream().filter(g->g.getAsJsonObject().get("name").getAsString().startsWith("petal_")).count();assertEquals(7,petals);
        double lowest=0;
        for(var element:shield.getAsJsonArray("elements"))if(element.getAsJsonObject().has("vertices"))for(var entry:element.getAsJsonObject().getAsJsonObject("vertices").entrySet())lowest=Math.min(lowest,entry.getValue().getAsJsonArray().get(1).getAsDouble());
        assertTrue(2.2+lowest*1.89/16>0,"The largest shield must not bury its lower petals in the floor");
        double front=0,back=-100;
        for(var element:shield.getAsJsonArray("elements"))if(element.getAsJsonObject().has("vertices"))for(var vertex:element.getAsJsonObject().getAsJsonObject("vertices").entrySet()){double z=vertex.getValue().getAsJsonArray().get(2).getAsDouble();front=Math.min(front,z);back=Math.max(back,z);}
        assertTrue(back-front>8,"Rho Aias petals need a rounded dome, not a flat plane");
        var crescent=read("models/motionrp_getsuga_crescent.bbmodel");assertEquals(3,crescent.getAsJsonArray("elements").size());
        for(var raw:crescent.getAsJsonArray("elements")){
            var e=raw.getAsJsonObject();assertEquals("mesh",e.get("type").getAsString());
            double min=100,max=-100;for(var vertex:e.getAsJsonObject("vertices").entrySet()){var xyz=vertex.getValue().getAsJsonArray();double y=xyz.get(0).getAsDouble()*Math.sin(Math.toRadians(35))+xyz.get(1).getAsDouble()*Math.cos(Math.toRadians(35));min=Math.min(min,y);max=Math.max(max,y);}
            assertTrue(max-min>34,"Crescent must be tall and curved");assertTrue(e.getAsJsonObject("faces").size()>100);
        }
        var weapon=read("models/motionrp_getsuga_tensa.bbmodel");var clip=weapon.getAsJsonArray("animations").get(0).getAsJsonObject();
        String socket=weapon.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("sword_socket")).findFirst().orElseThrow().get("uuid").getAsString();
        double[] rot=sample(clip,socket,"rotation",(BarrierSlashTimeline.GETSUGA_RELEASE-40)/20.0);double rx=Math.toRadians(rot[0]),ry=Math.toRadians(rot[1]),rz=Math.toRadians(rot[2]);
        double x=Math.cos(rz)*Math.sin(ry)*Math.sin(rx)-Math.sin(rz)*Math.cos(rx),y=Math.sin(rz)*Math.sin(ry)*Math.sin(rx)+Math.cos(rz)*Math.cos(rx);
        assertTrue(x<-.2 && y<-.6,"Forceful sword cut must finish diagonally down-left");
        var names=weapon.getAsJsonArray("elements").asList().stream().map(e->e.getAsJsonObject().get("name").getAsString()).toList();
        var grip=weapon.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.get("name").getAsString().equals("black_wrapped_hilt")).findFirst().orElseThrow();
        assertEquals(0,(grip.getAsJsonArray("from").get(1).getAsDouble()+grip.getAsJsonArray("to").get(1).getAsDouble())/2,.001,"Hold Tensa by the grip rather than the guard");
        assertEquals(12,names.stream().filter(n->n.startsWith("black_blade_")).count());assertTrue(names.stream().filter(n->n.startsWith("manji_guard_")).count()>=8);assertTrue(names.stream().anyMatch(n->n.startsWith("chain_side_")));assertFalse(names.stream().anyMatch(n->n.startsWith("cleaver_")));
    }
    @Test void newSceneHandsFollowTheAnimatedRigAndFeetStayGrounded() throws Exception {
        for(boolean shield:new boolean[]{true,false}){
            String id=shield?"rho_aias":"getsuga_tensho";int end=BarrierSlashTimeline.end(shield);var model=read("players/motionrp_"+id+".bbmodel");var a=model.getAsJsonArray("animations").get(0).getAsJsonObject();
            Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
            for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String uid=g.get("uuid").getAsString();groups.put(uid,g);names.put(g.get("name").getAsString(),uid);}parentTree(model.getAsJsonArray("outliner"),null,parents);
            if(!shield){
                var ready=posedOrigin(names.get("h_ph_head"),82/20.0,a,groups,parents);
                var cut=posedOrigin(names.get("h_ph_head"),88/20.0,a,groups,parents);
                assertTrue(ready[1]-cut[1]>1,"Upper body must sink into the cut");
                var first=BarrierHandPath.left(12);var last=BarrierHandPath.left(26);
                assertTrue(first.right()>last.right() && first.height()>last.height(),"Left hand sweeps from forehead toward the opposite jaw");
            }
            for(int tick=0;tick<end;tick++){
                var expected=posedOrigin(names.get(shield?"pli_left_item":"pri_right_item"),tick/20.0,a,groups,parents);var hand=BarrierHandPath.at(shield,tick);
                assertEquals(expected[0]/16,hand.right(),.000001);assertEquals(-expected[2]/16+.05,hand.forward(),.000001);assertEquals(expected[1]/16+.03,hand.height(),.000001);
                if(!shield && tick>=60 && tick<=138){
                    var l=posedOrigin(names.get("pli_left_item"),tick/20.0,a,groups,parents);var r=posedOrigin(names.get("pri_right_item"),tick/20.0,a,groups,parents);double gap=0;for(int k=0;k<3;k++)gap+=(r[k]-l[k])*(r[k]-l[k]);assertEquals(3.5,Math.sqrt(gap),.003,"Both palms must remain on the same sword hilt through the diagonal cut");
                    var grip=GetsugaBladePath.at(tick).grip();assertEquals((r[0]+l[0])/32,grip.right(),.0001);assertEquals(-(r[2]+l[2])/32+.05,grip.forward(),.0001);assertEquals((r[1]+l[1])/32+.03,grip.height(),.0001);
                    if(tick<=82){var head=posedOrigin(names.get("h_ph_head"),tick/20.0,a,groups,parents);assertTrue(r[1]>head[1]+6 && l[1]>head[1]+4,"Hold both hands overhead before releasing Getsuga");}
                }
                if(!shield){var head=posedOrigin(names.get("h_ph_head"),tick/20.0,a,groups,parents);var anchor=HollowHeadPath.at(tick);assertEquals(head[0]/16,anchor.right(),.000001);assertEquals(-head[2]/16,anchor.forward(),.000001);assertEquals(head[1]/16,anchor.height(),.000001);}
                for(boolean right:new boolean[]{true,false}){
                    var foot=posedPoint(names.get(right?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{right?1.875:-1.875,0,0},tick/20.0,a,groups,parents);assertEquals(0,foot[1],.001,"New motion feet detached from ground");
                }
            }
        }
    }

    void parentTree(JsonArray nodes, String parent, Map<String, String> parents) {
        for (var node : nodes) if (node.isJsonObject()) {
            var group = node.getAsJsonObject();
            String id = group.get("uuid").getAsString();
            if (parent != null) parents.put(id, parent);
            parentTree(group.getAsJsonArray("children"), id, parents);
        }
    }

    double[] posedOrigin(String id, double time, JsonObject animation, Map<String, JsonObject> groups, Map<String, String> parents) {
        return posedPoint(id, vector(groups.get(id).getAsJsonArray("origin")), time, animation, groups, parents);
    }

    double[] posedPoint(String id, double[] point, double time, JsonObject animation, Map<String, JsonObject> groups, Map<String, String> parents) {
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
