package dev.aserver.motionrp;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

/** Rename shipped labels only; custom names and motion IDs remain unchanged. */
final class MotionNames {
    private record Rename(String name,Set<String> old) {}
    private static Rename rename(String name,String... old){return new Rename(name,Set.of(old));}
    private static final Map<String,Rename> NAMES=Map.of(
        "road_roller",rename("ロードローラー","ロードローラーだッ！（ザ・ワールド）","ロードローラーだッ！"),
        "broly",rename("岩盤","ブロリー 岩盤（二人用）","ブロリー 岩盤","ブロリーの岩盤"),
        "gate_of_babylon",rename("王の財宝(慢心)","王の財宝"),
        "gate_of_babylon_100",rename("王の財宝","王の財宝・百門"),
        "gate_of_babylon_enuma",rename("王の財宝(本気)","王の財宝・本気（エヌマエリシュ）","王の財宝・本気(エヌマエリシュ)"),
        "susanoo",rename("完全体須佐能乎（マダラ）","須佐能乎（上半身）","完全体須佐能乎（上半身）"),
        "gae_bolg",rename("突き穿つ死翔の槍","刺し穿つ死棘の槍（ゲイ・ボルク）","突き穿つ死翔の槍（ゲイ・ボルク）"),
        "senbonzakura",rename("卍解・千本桜景義","千本桜景厳","千本桜景義"),
        "zangetsu_bankai",rename("卍解・天鎖斬月","斬月・卍解（天鎖斬月）","斬月・卍解(天鎖斬月)"));
    static String updated(String id,String current){var r=NAMES.get(id);return r!=null&&current!=null&&r.old.contains(current)?r.name:current;}
    static boolean migrate(FileConfiguration config){boolean changed=false;for(String id:NAMES.keySet()){String key="motions."+id+".name",old=config.getString(key);String next=updated(id,old);if(!Objects.equals(old,next)){config.set(key,next);changed=true;}}return changed;}
}
