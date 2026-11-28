package dev.aserver.motionrp;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;
import dev.aserver.motionrp.core.RasenganTimeline;
import dev.aserver.motionrp.core.RasenshurikenTimeline;
import dev.aserver.motionrp.core.GateTimeline;
import dev.aserver.motionrp.core.SusanooTimeline;
import dev.aserver.motionrp.core.SenbonzakuraTimeline;
import dev.aserver.motionrp.core.TitanTimeline;

public record MotionCatalog(Map<String, Definition> motions, String controllerName, NamespacedKey controllerModel) {
    public record Definition(String id, String name, String model, String animation, int maxTicks, NamespacedKey itemModel, String effect) {
        public boolean obtainable(){return !id.equals("infinity_castle")&&!effect.equals("infinity_castle");}
    }

    public static MotionCatalog read(FileConfiguration config) {
        var section = config.getConfigurationSection("motions");
        if (section == null || section.getKeys(false).isEmpty()) throw new IllegalArgumentException("motions is empty");
        var definitions = new LinkedHashMap<String, Definition>();
        for (String id : section.getKeys(false)) {
            if (!id.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid motion id: " + id);
            String base = "motions." + id + ".";
            if(id.equals("emperor_penguin_2") || config.getString(base+"effect","").startsWith("pair_emperor_penguin_2")) continue; // Retain saved configuration, hide the retired two-player version.
            String model = required(config, base + "model");
            String animation = required(config, base + "animation");
            int ticks = config.getInt(base + "max-ticks", 200);
            if (ticks < 1 || ticks > 12_000) throw new IllegalArgumentException(base + "max-ticks must be 1..12000");
            String effect = config.getString(base + "effect", "none");
            if (!Set.of("entei","gigant_pistol","pair_balse","none", "rasengan", "rasenshuriken", "gate_of_babylon", "susanoo", "senbonzakura","titan_transform","gate_of_babylon_100","gate_of_babylon_enuma","zangetsu_bankai","excalibur","gae_bolg","kamehameha","genki_dama","rho_aias","getsuga_tensho","road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei","pair_fusion","pair_broly","infinity_castle","death_sword","eternal_blizzard","death_spear","fire_tornado","emperor_penguin_1","pair_emperor_penguin_2").contains(effect)) throw new IllegalArgumentException("Unknown effect: " + effect);
            if(effect.equals("entei")&&ticks<190)throw new IllegalArgumentException("Entei max-ticks is too short");
            if(Set.of("fire_tornado","emperor_penguin_1").contains(effect)&&ticks<dev.aserver.motionrp.core.FootballTimeline.end(effect.equals("fire_tornado")))throw new IllegalArgumentException("Football max-ticks is too short");
            if(effect.equals("gigant_pistol")&&ticks<150)throw new IllegalArgumentException("Gigant pistol max-ticks is too short");
            if (effect.equals("rasengan") && ticks < RasenganTimeline.END) throw new IllegalArgumentException("Rasengan max-ticks must be at least " + RasenganTimeline.END);
            if (effect.equals("rasenshuriken") && ticks < RasenshurikenTimeline.END) throw new IllegalArgumentException("Rasenshuriken max-ticks must be at least " + RasenshurikenTimeline.END);
            if (effect.equals("gate_of_babylon") && ticks < GateTimeline.END) throw new IllegalArgumentException("Gate max-ticks must be at least " + GateTimeline.END);
            if (effect.equals("susanoo") && ticks < SusanooTimeline.END) throw new IllegalArgumentException("Susanoo max-ticks must be at least " + SusanooTimeline.END);
            if (effect.equals("senbonzakura") && ticks < SenbonzakuraTimeline.END) throw new IllegalArgumentException("Senbonzakura max-ticks must be at least " + SenbonzakuraTimeline.END);
            if (effect.equals("titan_transform") && ticks < TitanTimeline.END) throw new IllegalArgumentException("Titan max-ticks must be at least " + TitanTimeline.END);
            if(effect.startsWith("gate_of_babylon_") && ticks<dev.aserver.motionrp.core.TreasuryTimeline.end(effect.endsWith("enuma"))) throw new IllegalArgumentException("Treasury max-ticks is too short");
            if(Set.of("excalibur","gae_bolg").contains(effect) && ticks<dev.aserver.motionrp.core.NobleTimeline.end(effect.equals("excalibur"))) throw new IllegalArgumentException("Noble max-ticks is too short");
            if(effect.equals("zangetsu_bankai") && ticks<dev.aserver.motionrp.core.BankaiTimeline.END) throw new IllegalArgumentException("Bankai max-ticks is too short");
            if(Set.of("kamehameha","genki_dama").contains(effect) && ticks<dev.aserver.motionrp.core.DragonBallTimeline.end(effect.equals("genki_dama"))) throw new IllegalArgumentException("Dragon Ball max-ticks is too short");
            if(Set.of("rho_aias","getsuga_tensho").contains(effect) && ticks<dev.aserver.motionrp.core.BarrierSlashTimeline.end(effect.equals("rho_aias"))) throw new IllegalArgumentException("Barrier/slash max-ticks is too short");
            if(dev.aserver.motionrp.core.EpicTimeline.EFFECTS.contains(effect)&&ticks<dev.aserver.motionrp.core.EpicTimeline.end(effect))throw new IllegalArgumentException("Epic max-ticks is too short");
            if(effect.equals("infinity_castle")&&ticks<dev.aserver.motionrp.core.InfinityCastleTimeline.END)throw new IllegalArgumentException("Infinity castle max-ticks is too short");
            var pair=dev.aserver.motionrp.core.PairMotion.fromEffect(effect);if(pair.isPresent()&&ticks<pair.get().duration())throw new IllegalArgumentException("Pair max-ticks is too short");
            definitions.put(id, new Definition(id, config.getString(base + "name", id), model, animation, ticks, key(config, base + "item-model"), effect));
        }
        return new MotionCatalog(Collections.unmodifiableMap(definitions), config.getString("controller.name", "モーションセット"), key(config, "controller.item-model"));
    }

    private static String required(FileConfiguration config, String path) {
        String value = config.getString(path);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + path);
        return value;
    }

    private static NamespacedKey key(FileConfiguration config, String path) {
        String value = config.getString(path);
        if (value == null) return null;
        NamespacedKey key = NamespacedKey.fromString(value);
        if (key == null) throw new IllegalArgumentException("Invalid item model: " + path);
        return key;
    }
}
