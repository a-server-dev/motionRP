package dev.aserver.motionrp.pack;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyPackLightingTest {
    @TempDir Path root;
    @Test void maskRepairsOldDuplexCopiesWithExteriorWindingWithoutChangingSharedPrimitives()throws Exception{
        var pack=root.resolve("mask.zip");
        String primitive="{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,0],\"faces\":{\"north\":{\"texture\":\"#0\"},\"south\":{\"texture\":\"#0\"}}}]}";
        Map<String,String> entries=new LinkedHashMap<>();entries.put("pack.mcmeta","{\"pack\":{}}");entries.put("assets/motionrp/models/energy/old.json",primitive);
        for(int sign:new int[]{1,-1})entries.put("assets/bettermodel/items/motionrp_hollow_mask_band_"+sign+".json","{\"model\":{\"type\":\"model\",\"model\":\"motionrp:energy/old\",\"transformation\":["+sign+",0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]}}");
        try(var zip=new ZipOutputStream(Files.newOutputStream(pack))){for(var e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}}
        assertTrue(EnergyPackLighting.prepare(pack));
        try(var zip=new ZipFile(pack.toFile())){
            assertEquals(primitive,new String(zip.getInputStream(zip.getEntry("assets/motionrp/models/energy/old.json")).readAllBytes()));
            for(int sign:new int[]{1,-1}){
                var item=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("assets/bettermodel/items/motionrp_hollow_mask_band_"+sign+".json")).readAllBytes())).getAsJsonObject().getAsJsonObject("model");
                String ref=item.get("model").getAsString();var model=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("assets/motionrp/models/"+ref.split(":")[1]+".json")).readAllBytes())).getAsJsonObject();
                assertEquals(Set.of(sign<0?"north":"south"),model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces").keySet());
            }
        }
        byte[] repaired=Files.readAllBytes(pack);assertFalse(EnergyPackLighting.prepare(pack));assertArrayEquals(repaired,Files.readAllBytes(pack));
    }

    @Test void ownEnergyItemsGetUnshadedCopiesWithoutChangingOtherModelsAndProcessingIsIdempotent()throws Exception{
        var pack=root.resolve("build.zip");String triangle="assets/bettermodel/models/triangle.json",energy="assets/bettermodel/items/motionrp_genki_dama_orb_energy_sphere.json",other="assets/custom/items/example.json";
        var model="{\"textures\":{\"0\":\"bettermodel:pixel\"},\"elements\":[{\"from\":[0,0,0],\"to\":[1,1,0],\"faces\":{}}]}";
        var item="{\"model\":{\"type\":\"composite\",\"models\":[{\"type\":\"model\",\"model\":\"bettermodel:triangle\"}]}}";
        try(var zip=new ZipOutputStream(Files.newOutputStream(pack))){for(var e:Map.of(triangle,model,energy,item,"assets/bettermodel/items/motionrp_rho_aias_shield_petal_0.json",item,"assets/bettermodel/items/motionrp_getsuga_crescent_crescent_0.json",item,other,item,"assets/custom/items/not_motionrp_genki_dama_orb_energy_sphere.json",item,"pack.mcmeta","{\"pack\":{}}").entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}}
        assertTrue(EnergyPackLighting.prepare(pack));
        try(var zip=new ZipFile(pack.toFile())){
            assertEquals(model,new String(zip.getInputStream(zip.getEntry(triangle)).readAllBytes()));assertEquals(item,new String(zip.getInputStream(zip.getEntry(other)).readAllBytes()));assertEquals(item,new String(zip.getInputStream(zip.getEntry("assets/custom/items/not_motionrp_genki_dama_orb_energy_sphere.json")).readAllBytes()));
            for(String id:List.of("motionrp_rho_aias_shield_petal_0","motionrp_getsuga_crescent_crescent_0"))assertTrue(new String(zip.getInputStream(zip.getEntry("assets/bettermodel/items/"+id+".json")).readAllBytes()).contains("motionrp:energy/"),"New energy mesh lighting: "+id);
            var own=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry(energy)).readAllBytes())).getAsJsonObject();
            String ref=own.getAsJsonObject("model").getAsJsonArray("models").get(0).getAsJsonObject().get("model").getAsString();assertTrue(ref.startsWith("motionrp:energy/"));
            var copy=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("assets/motionrp/models/"+ref.split(":")[1]+".json")).readAllBytes())).getAsJsonObject();
            var cube=copy.getAsJsonArray("elements").get(0).getAsJsonObject();assertFalse(cube.get("shade").getAsBoolean());assertEquals(15,cube.get("light_emission").getAsInt());
        }
        byte[] fixed=Files.readAllBytes(pack);assertFalse(EnergyPackLighting.prepare(pack));assertArrayEquals(fixed,Files.readAllBytes(pack));
    }
    @Test void incompleteEnergyArchivesAreNeverRepairedOrPublished()throws Exception{
        var file=root.resolve("partial.zip");
        try(var z=new ZipOutputStream(Files.newOutputStream(file))){
            for(var e:Map.of("pack.mcmeta","{\"pack\":{}}","assets/bettermodel/items/motionrp_genki_dama_orb_energy_sphere.json","{\"model\":{\"type\":\"model\",\"model\":\"bettermodel:triangle\"}}").entrySet()){
                z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue().getBytes());z.closeEntry();
            }
        }
        byte[] complete=Files.readAllBytes(file),partial=Arrays.copyOf(complete,complete.length-18);Files.write(file,partial);
        assertThrows(java.io.IOException.class,()->EnergyPackLighting.prepare(file));assertArrayEquals(partial,Files.readAllBytes(file));
    }
}
