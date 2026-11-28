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
    static final String EXCALIBUR="motionrp_excalibur_blade",EXCALIBUR_LIGHT="motionrp_excalibur_light",GAE_BOLG="motionrp_gae_bolg_spear",GAE_TRAIL="motionrp_gae_bolg_trail",GAE_THORNS="motionrp_gae_bolg_thorns";
    static final String EXCALIBUR_PILLAR="motionrp_excalibur_pillar",KAME_ORB="motionrp_kamehameha_orb",KAME_BEAM="motionrp_kamehameha_beam",GENKI_ORB="motionrp_genki_dama_orb";
    static final String EXCALIBUR_CHARGE="motionrp_excalibur_charge",EXCALIBUR_WAVE="motionrp_excalibur_wave";
    static List<String> models(String effect) {
        return switch(effect) {
            case "entei" -> List.of("motionrp_entei_sun");
            case "gigant_pistol" -> List.of();
            case "balse_left","balse_right" -> List.of("motionrp_balse_crystal","motionrp_balse_light");
            case "death_sword" -> List.of("motionrp_football","motionrp_death_sword_edge","motionrp_death_sword_core");
            case "eternal_blizzard" -> List.of("motionrp_football","motionrp_blizzard_orb","motionrp_blizzard_flake","motionrp_blizzard_wind","motionrp_blizzard_wake");
            case "death_spear" -> List.of("motionrp_football","motionrp_death_spear_drill");
            case "fire_tornado" -> List.of("motionrp_football");
            case "emperor_penguin_1" -> List.of("motionrp_football","motionrp_emperor_penguin_red");
            case "emperor_penguin_2_left","emperor_penguin_2_right" -> List.of("motionrp_football","motionrp_emperor_penguin");
            case "infinity_castle" -> List.of("motionrp_castle_chamber","motionrp_castle_platform","motionrp_castle_fusuma","motionrp_castle_hatch");
            case "final_battle_naruto","final_battle_sasuke" -> List.of("motionrp_duel_kurama","motionrp_duel_asura_kurama","motionrp_duel_susanoo","motionrp_duel_indra_susanoo","motionrp_duel_gold_rasenshuriken","motionrp_duel_dark_rasenshuriken","motionrp_duel_indra_arrow","motionrp_duel_clash_orb","motionrp_duel_chakra_spark","motionrp_duel_chakra_storm");
            case "road_roller" -> List.of("motionrp_the_world","motionrp_the_world_upper","motionrp_road_roller","motionrp_roller_debris");
            case "night_guy" -> List.of();
            case "kokuryuha" -> List.of("motionrp_black_dragon_head");
            case "kokujo_tengen_myoo" -> List.of("motionrp_tengen_myoo");
            case "chibaku_tensei" -> List.of("motionrp_gravity_core","motionrp_gravity_rock","motionrp_rock_moon");
            case "broly_attacker","broly_victim" -> List.of("motionrp_broly_wall","motionrp_broly_dented_wall","motionrp_broly_crater","motionrp_broly_debris");
            case "fusion_left","fusion_right" -> List.of("motionrp_fusion_warrior");
            case "rho_aias" -> List.of("motionrp_rho_aias_shield");
            case "getsuga_tensho" -> List.of("motionrp_getsuga_tensa","motionrp_getsuga_crescent","motionrp_hollow_mask");
            case "excalibur" -> List.of(EXCALIBUR,EXCALIBUR_LIGHT,EXCALIBUR_PILLAR,EXCALIBUR_CHARGE,EXCALIBUR_WAVE);
            case "kamehameha" -> List.of(KAME_ORB,KAME_BEAM);
            case "genki_dama" -> List.of(GENKI_ORB);
            case "gae_bolg" -> List.of(GAE_BOLG);
            case "zangetsu_bankai" -> List.of(ZANGETSU,TENSA,BANKAI_COCOON,BANKAI_WAVE);
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
