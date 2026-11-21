package dev.aserver.motionrp;
import com.google.gson.*;
import dev.aserver.motionrp.core.BundledAssetUpdater;
import org.bukkit.Bukkit;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Compiled data-only projects. No scripts or remote code are loaded from recipes. */
final class StudioProjects {
 private final MotionPlugin plugin;private final Map<String,JsonObject> projects=new LinkedHashMap<>();private final Set<String> files=new LinkedHashSet<>();private final Properties hashes=new Properties();
 StudioProjects(MotionPlugin plugin){this.plugin=plugin;try(var in=plugin.getResource("studio/index.json")){if(in!=null){var root=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();for(var raw:root.getAsJsonArray("projects")){var p=raw.getAsJsonObject();projects.put(p.get("id").getAsString(),p);for(var f:p.getAsJsonArray("assets"))files.add(f.getAsString());}}Path saved=plugin.getDataFolder().toPath().resolve("studio-asset-hashes.properties");if(Files.exists(saved))try(var reader=Files.newInputStream(saved)){hashes.load(reader);}}catch(IOException e){throw new UncheckedIOException(e);}}
 JsonObject project(String effect){return projects.get(effect.substring("studio:".length()));}
 Collection<String> assets(){return files;}
 Set<String> knownHashes(String file,Set<String> old){var result=new HashSet<>(old);if(files.contains(file)&&hashes.containsKey(file))result.add(hashes.getProperty(file));return result;}
 void installed(String file,byte[] bytes,boolean preserved){if(files.contains(file)&&!preserved)hashes.setProperty(file,BundledAssetUpdater.sha256(bytes));}
 void saveHashes()throws IOException{if(files.isEmpty())return;Path p=plugin.getDataFolder().toPath().resolve("studio-asset-hashes.properties");Files.createDirectories(p.getParent());try(var out=Files.newOutputStream(p)){hashes.store(out,"Only previously installed Studio model hashes");}}
 MotionCatalog merge(MotionCatalog catalog){var all=new LinkedHashMap<>(catalog.motions());for(var p:projects.values()){String id=p.get("id").getAsString();all.putIfAbsent("studio_"+id,new MotionCatalog.Definition("studio_"+id,p.get("name").getAsString(),p.get("bodyModel").getAsString(),p.get("bodyAnimation").getAsString(),p.get("duration").getAsInt()+10,null,"studio:"+id));}return new MotionCatalog(Collections.unmodifiableMap(all),catalog.controllerName(),catalog.controllerModel());}
 String unavailable(String effect){var p=project(effect);if(p==null)return "Studioプロジェクトがありません。";for(var raw:p.getAsJsonArray("models")){var m=raw.getAsJsonObject();String name=m.get("asset").getAsString();if(m.get("kind").getAsString().equals("display")&&kr.toxicity.model.api.BetterModel.modelOrNull(name)==null)return "Studioモデルが未読込: "+name;}return null;}
}
