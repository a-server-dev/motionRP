package dev.aserver.motionrp;

import java.util.List;

final class SummonAssets {
    static final String GATE_PLAYER="motionrp_gate_of_babylon", SUSANOO_PLAYER="motionrp_susanoo";
    static final String GATE="motionrp_golden_gate", SWORD="motionrp_treasury_sword",
            SPEAR="motionrp_treasury_spear", AXE="motionrp_treasury_axe",
            RIBS="motionrp_susanoo_ribs", ARMOR="motionrp_susanoo_armor", SLASH="motionrp_susanoo_slash";
    static final String CHERRY_BLADE="motionrp_senbonzakura_blade",CHERRY_PETALS="motionrp_senbonzakura_petals";
    static List<String> models(String effect) {
        return switch(effect) {
            case "gate_of_babylon" -> List.of(GATE,SWORD,SPEAR,AXE);
            case "susanoo" -> List.of(RIBS,ARMOR,SLASH);
            case "senbonzakura" -> List.of(CHERRY_BLADE,CHERRY_PETALS);
            default -> List.of();
        };
    }
}
