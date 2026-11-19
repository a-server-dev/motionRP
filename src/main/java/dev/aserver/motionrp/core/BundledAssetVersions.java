package dev.aserver.motionrp.core;

import java.util.Set;

/** Hashes of shipped assets that are safe to migrate; custom assets remain untouched. */
public final class BundledAssetVersions {
    private BundledAssetVersions() {}
    private static final java.util.Map<String,String> PREVIOUS_013=java.util.Map.ofEntries(
            java.util.Map.entry("models/motionrp_susanoo_armor.bbmodel","daf7ea131144ddfac2d3dcf667c6b738f759a90537dfdf2eb9bdf29f260f7469"),
            java.util.Map.entry("models/motionrp_susanoo_slash.bbmodel","cc11f07e23521f042546d9f4179dec2b8c41cfc3df6eeeadf1d0218d534d5afa"),
            java.util.Map.entry("models/motionrp_excalibur_light.bbmodel","8b3b49b9eb323ddf63be222bf890eec56d821dfc79a6dc27ccf7424a9a5a2ea9"),
            java.util.Map.entry("models/motionrp_excalibur_blade.bbmodel","8b248fca65a52c658db42d2bf3e93c14edaad8abc2203482029ce648de6aebee"),
            java.util.Map.entry("models/motionrp_zangetsu_cleaver.bbmodel","69e37e93ca81f842db30a770f9cee95cd67ca28210fc3cc6712f3deaf4167ac9"),
            java.util.Map.entry("models/motionrp_gae_bolg_spear.bbmodel","77ca95862f35656b7ab026bf52c0637c129739fd7f0b41e1af1c4f94b3c5bc55"),
            java.util.Map.entry("players/motionrp_gae_bolg.bbmodel","8c9917375373e5c71822ee7fbe9f3e8f6d4da65e490fcbff018f42d2181588ee"),
            java.util.Map.entry("players/motionrp_excalibur.bbmodel","4a0d513e9334b06b222096588068c5be8990782b070c031f9fcc17e98a84014e"));
    private static final java.util.Map<String,String> PREVIOUS_014=java.util.Map.ofEntries(
            java.util.Map.entry("models/motionrp_kamehameha_beam.bbmodel","3415c662830c9c3f573171c0e12fa9390703bb760c24442f1c23c5bfa224ae7e"),
            java.util.Map.entry("models/motionrp_genki_dama_orb.bbmodel","38edd1441828b699d622f1a5fbe5ffb5c4691ce95790bd0bf4165af718444946"),
            java.util.Map.entry("models/motionrp_excalibur_light.bbmodel","4bde46a2d1fec98a7402c6cf80f54924948638d007dc6cf13bcb17bbbf3e2c67"),
            java.util.Map.entry("models/motionrp_excalibur_blade.bbmodel","bd38a474399698083fa955bcc6ab0d55342cd828c68a17e75a7d8dc24a40508c"),
            java.util.Map.entry("models/motionrp_excalibur_pillar.bbmodel","5ba86735a6ac502e61da6c208d778ebc9a17889a1cc5922581ef5e62f23161af"),
            java.util.Map.entry("models/motionrp_kamehameha_orb.bbmodel","8d819ab43ba5afaca1595593e32582e30fe85f3c536068318530523802d56cb4"),
            java.util.Map.entry("players/motionrp_excalibur.bbmodel","c4db9979e6723df4328bce88251ededcedaee258158e2396c25b9218a6f03cff"),
            java.util.Map.entry("players/motionrp_kamehameha.bbmodel","1f197d97a75fa05ec21e9a6148fb84cc3db2217ce728a21b38243d97ce26813d"),
            java.util.Map.entry("players/motionrp_genki_dama.bbmodel","757f02882a900f71b54267ee6e7807f80de1313c1b7b0871bc98222ed9dcf29e"));
    public static Set<String> knownHashes(String file,Set<String> older) {
        var hashes=new java.util.HashSet<>(older);String previous=PREVIOUS_013.get(file);if(previous!=null)hashes.add(previous);String v14=PREVIOUS_014.get(file);if(v14!=null)hashes.add(v14);if(file.equals("players/motionrp_excalibur.bbmodel"))hashes.add("0c9d6dde3fc2e47dabdf376c938aa52c02a4eca1b5108480f678f3b8e7bc4c30");return hashes;
    }
}
