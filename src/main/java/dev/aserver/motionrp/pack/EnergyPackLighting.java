package dev.aserver.motionrp.pack;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Disable directional cube shading only for MotionRP energy items, including mesh triangle copies. */
public final class EnergyPackLighting {
    private static final List<String> IDS=List.of("motionrp_genki_dama_orb_","motionrp_kamehameha_orb_","motionrp_kamehameha_beam_","motionrp_excalibur_light_","motionrp_excalibur_pillar_","motionrp_excalibur_charge_","motionrp_excalibur_wave_");
    private EnergyPackLighting() {}
    public static synchronized boolean prepare(Path path)throws IOException{
        var before=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class);
        if(before.size()>PackSnapshot.MAX_BYTES)throw new IOException("Energy pack exceeds size limit");
        // Do not turn a partially written ZIP into an apparently complete pack.
        try(var central=new ZipFile(path.toFile())){
            var metadata=central.getEntry("pack.mcmeta");if(metadata==null)throw new IOException("pack.mcmeta is missing");
            try(var in=central.getInputStream(metadata)){
                byte[] meta=in.readNBytes(65537);if(meta.length>65536)throw new IOException("pack.mcmeta is too large");
                try{if(!JsonParser.parseString(new String(meta,StandardCharsets.UTF_8)).getAsJsonObject().has("pack"))throw new IOException("Invalid pack.mcmeta");}
                catch(RuntimeException invalid){throw new IOException("Invalid pack.mcmeta",invalid);}
            }
        }
        Map<String,byte[]> files=new LinkedHashMap<>();long expanded=0;
        try(var zip=new ZipInputStream(Files.newInputStream(path))){
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){
                var out=new ByteArrayOutputStream();byte[] b=new byte[8192];
                for(int n;(n=zip.read(b))!=-1;){expanded+=n;if(expanded>512L*1024*1024)throw new IOException("Expanded energy pack exceeds size limit");out.write(b,0,n);}
                if(files.putIfAbsent(entry.getName(),out.toByteArray())!=null)throw new IOException("Duplicate pack entry");
            }
        }
        Map<String,byte[]> additions=new LinkedHashMap<>();boolean changed=false;
        try{
            for(var entry:new ArrayList<>(files.entrySet())){
                String name=entry.getKey();int assets=name.indexOf("assets/");
                if(assets<0 || !name.contains("/items/") || !name.endsWith(".json") || IDS.stream().noneMatch(id->name.substring(name.lastIndexOf('/')+1).startsWith(id)))continue;
                JsonObject item=JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
                if(rewrite(item,name.substring(0,assets),files,additions)) {files.put(name,item.toString().getBytes(StandardCharsets.UTF_8));changed=true;}
            }
        }catch(RuntimeException invalid){throw new IOException("Invalid energy item definition",invalid);}
        if(!changed)return false;
        files.putAll(additions);var after=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class);
        if(before.size()!=after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()))throw new IOException("Pack is still changing");
        Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"motionrp-energy-",".zip.partial");
        try{
            try(var zip=new ZipOutputStream(Files.newOutputStream(tmp))){for(var e:files.entrySet()){var entry=new ZipEntry(e.getKey());entry.setTime(0);zip.putNextEntry(entry);zip.write(e.getValue());zip.closeEntry();}}
            var current=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class);
            if(after.size()!=current.size() || !after.lastModifiedTime().equals(current.lastModifiedTime()))throw new IOException("Pack changed during energy lighting preparation");
            try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
        return true;
    }
    private static boolean rewrite(JsonElement value,String prefix,Map<String,byte[]> files,Map<String,byte[]> added){
        boolean changed=false;
        if(value.isJsonArray()){for(var child:value.getAsJsonArray())changed|=rewrite(child,prefix,files,added);}
        else if(value.isJsonObject()){
            var obj=value.getAsJsonObject();
            for(var entry:new ArrayList<>(obj.entrySet())){
                var child=entry.getValue();
                if(entry.getKey().equals("model") && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()){
                    String ref=child.getAsString();if(ref.startsWith("motionrp:energy/") || !ref.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || ref.contains(".."))continue;
                    String[] parts=ref.split(":",2);String source="assets/"+parts[0]+"/models/"+parts[1]+".json";
                    byte[] bytes=files.get(prefix+source);if(bytes==null)bytes=files.get(source);if(bytes==null)continue;
                    JsonObject model=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                    if(!model.has("elements"))continue;
                    model.addProperty("ambientocclusion",false);
                    for(var raw:model.getAsJsonArray("elements")){var cube=raw.getAsJsonObject();cube.addProperty("shade",false);cube.addProperty("light_emission",15);}
                    String id=PackSnapshot.digest("SHA-256",bytes).substring(0,24);String target="motionrp:energy/"+id;
                    added.put(prefix+"assets/motionrp/models/energy/"+id+".json",model.toString().getBytes(StandardCharsets.UTF_8));obj.addProperty("model",target);changed=true;
                }else changed|=rewrite(child,prefix,files,added);
            }
        }
        return changed;
    }
}
