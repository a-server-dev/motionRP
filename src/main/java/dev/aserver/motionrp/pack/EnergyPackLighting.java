package dev.aserver.motionrp.pack;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Scoped full-bright energy copies and exterior-only Hollow mask triangles. */
public final class EnergyPackLighting {
    private static final List<String> IDS=List.of("motionrp_hollow_mask_","motionrp_rho_aias_shield_","motionrp_getsuga_crescent_","motionrp_genki_dama_orb_","motionrp_kamehameha_orb_","motionrp_kamehameha_beam_","motionrp_excalibur_light_","motionrp_excalibur_pillar_","motionrp_excalibur_charge_","motionrp_excalibur_wave_");
    public static final String MANIFEST="assets/motionrp/energy-targets.json";
    public static String targetKind(String model){return IDS.contains(model+"_")?(model.equals("motionrp_hollow_mask")?"mask":"energy"):null;}
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
        Map<String,String> targets=new HashMap<>();
        if(files.containsKey(MANIFEST)){
            try{
                var manifest=JsonParser.parseString(new String(files.get(MANIFEST),StandardCharsets.UTF_8)).getAsJsonObject();
                if(manifest.get("version").getAsInt()!=1)throw new IllegalArgumentException("Unsupported energy manifest");
                var items=manifest.getAsJsonObject("items");if(items.size()>4096)throw new IllegalArgumentException("Too many energy targets");
                for(var entry:items.entrySet()){
                    String key=entry.getKey(),kind=entry.getValue().getAsString();
                    if(!key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||key.contains("..")||!Set.of("mask","energy").contains(kind))throw new IllegalArgumentException("Invalid energy target");
                    targets.put(key,kind);
                }
            }catch(RuntimeException invalid){throw new IOException("Invalid energy manifest",invalid);}
        }
        Map<String,byte[]> additions=new LinkedHashMap<>();boolean changed=false;
        try{
            for(var entry:new ArrayList<>(files.entrySet())){
                String name=entry.getKey();int assets=name.indexOf("assets/");
                if(assets<0 || !name.contains("/items/") || !name.endsWith(".json"))continue;
                String relative=name.substring(assets+7);int itemPath=relative.indexOf("/items/");if(itemPath<1)continue;
                String key=relative.substring(0,itemPath)+":"+relative.substring(itemPath+7,relative.length()-5);
                String kind=targets.get(key);String base=name.substring(name.lastIndexOf('/')+1);
                if(kind==null){if(files.containsKey(MANIFEST)||IDS.stream().noneMatch(base::startsWith))continue;kind=base.startsWith("motionrp_hollow_mask_")?"mask":"energy";}
                JsonObject item=JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
                if(rewrite(item,name.substring(0,assets),files,additions,kind.equals("mask"))) {files.put(name,item.toString().getBytes(StandardCharsets.UTF_8));changed=true;}
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
    private static boolean rewrite(JsonElement value,String prefix,Map<String,byte[]> files,Map<String,byte[]> added,boolean mask){
        boolean changed=false;
        if(value.isJsonArray()){for(var child:value.getAsJsonArray())changed|=rewrite(child,prefix,files,added,mask);}
        else if(value.isJsonObject()){
            var obj=value.getAsJsonObject();
            for(var entry:new ArrayList<>(obj.entrySet())){
                var child=entry.getValue();
                if(entry.getKey().equals("model") && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()){
                    String ref=child.getAsString();if((ref.startsWith("motionrp:energy/") && !mask) || !ref.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || ref.contains(".."))continue;
                    String[] parts=ref.split(":",2);String source="assets/"+parts[0]+"/models/"+parts[1]+".json";
                    byte[] bytes=files.get(prefix+source);if(bytes==null)bytes=files.get(source);if(bytes==null)continue;
                    JsonObject model=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                    if(!model.has("elements"))continue;
                    String face="north";
                    if(mask){
                        // Mesh conversion can use mirrored affine triangles. Choose the winding
                        // that matches Minecraft item rendering (the exported mesh winding is reversed).
                        double determinant=1;
                        if(obj.has("transformation") && obj.get("transformation").isJsonArray()){
                            var t=obj.getAsJsonArray("transformation");if(t.size()==16)determinant=t.get(0).getAsDouble()*t.get(5).getAsDouble()-t.get(1).getAsDouble()*t.get(4).getAsDouble();
                        }
                        face=determinant<0?"south":"north";
                        boolean correct=true;
                        for(var raw:model.getAsJsonArray("elements")){var faces=raw.getAsJsonObject().getAsJsonObject("faces");if(faces==null || faces.size()!=1 || !faces.has(face))correct=false;}
                        if(ref.startsWith("motionrp:energy/") && correct)continue;
                        for(var raw:model.getAsJsonArray("elements")){
                            var cube=raw.getAsJsonObject();var faces=cube.getAsJsonObject("faces");
                            if(faces!=null && !faces.isEmpty()){
                                var side=faces.has(face)?faces.get(face).deepCopy():faces.entrySet().iterator().next().getValue().deepCopy();
                                var one=new JsonObject();one.add(face,side);cube.add("faces",one);
                            }
                        }
                    }
                    model.addProperty("ambientocclusion",false);
                    for(var raw:model.getAsJsonArray("elements")){var cube=raw.getAsJsonObject();cube.addProperty("shade",false);cube.addProperty("light_emission",15);}
                    String id=(mask?"mask_"+face+"_":"")+PackSnapshot.digest("SHA-256",bytes).substring(0,24);String target="motionrp:energy/"+id;
                    added.put(prefix+"assets/motionrp/models/energy/"+id+".json",model.toString().getBytes(StandardCharsets.UTF_8));obj.addProperty("model",target);changed=true;
                }else changed|=rewrite(child,prefix,files,added,mask);
            }
        }
        return changed;
    }
}
