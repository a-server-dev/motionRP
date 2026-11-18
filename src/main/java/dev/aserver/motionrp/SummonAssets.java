package dev.aserver.motionrp;

import java.util.List;

final class SummonAssets {
    static final String GATE_PLAYER="motionrp_gate_of_babylon", SUSANOO_PLAYER="motionrp_susanoo";
    static final String GATE="motionrp_golden_gate", SWORD="motionrp_treasury_sword",
            SPEAR="motionrp_treasury_spear", AXE="motionrp_treasury_axe",
            RIBS="motionrp_susanoo_ribs", ARMOR="motionrp_susanoo_armor", SLASH="motionrp_susanoo_slash";
    static final String CHERRY_BLADE="motionrp_senbonzakura_blade",CHERRY_PETALS="motionrp_senbonzakura_petals";
    static final String CHERRY_FIELD="motionrp_senbonzakura_field_blade",TITAN_BOLT="motionrp_titan_lightning";
    static final String MASS_GATE="motionrp_golden_gate_mass",BAB_ILU="motionrp_bab_ilu",EA="motionrp_ea",ENUMA="motionrp_enuma_vortex",TITAN_BODY="motionrp_titan_body";
    static final String ENUMA_CHARGE="motionrp_enuma_charge",ENUMA_RING="motionrp_enuma_ring",ENUMA_RIFT="motionrp_enuma_rift";
    static final String ZANGETSU="motionrp_zangetsu_cleaver",TENSA="motionrp_tensa_zangetsu",BANKAI_COCOON="motionrp_bankai_cocoon",BANKAI_WAVE="motionrp_bankai_wave",BANKAI_COAT="motionrp_bankai_coat";
    static List<String> models(String effect) {
        return switch(effect) {
            case "zangetsu_bankai" -> List.of(ZANGETSU,TENSA,BANKAI_COCOON,BANKAI_WAVE,BANKAI_COAT);
            case "gate_of_babylon_100" -> List.of(MASS_GATE,SWORD,SPEAR,AXE);
            case "gate_of_babylon_enuma" -> List.of(MASS_GATE,SWORD,SPEAR,AXE,BAB_ILU,EA,ENUMA,ENUMA_RING);
            case "gate_of_babylon" -> List.of(GATE,SWORD,SPEAR,AXE);
            case "susanoo" -> List.of(RIBS,ARMOR,SLASH);
            case "senbonzakura" -> List.of(CHERRY_BLADE,CHERRY_FIELD,CHERRY_PETALS);
            case "titan_transform" -> List.of(TITAN_BOLT,TITAN_BODY);
            default -> List.of();
        };
    }
}
