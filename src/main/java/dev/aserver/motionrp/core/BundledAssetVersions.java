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
    public static Set<String> knownHashes(String file,Set<String> older) {
        var hashes=new java.util.HashSet<>(older);String previous=PREVIOUS_013.get(file);if(previous!=null)hashes.add(previous);return hashes;
    }
}
