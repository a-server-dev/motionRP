package dev.aserver.motionrp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.Collection;

/** Packet-only wind: never creates an explosion or a wind-charge entity. */
final class WindEffects {
    static void swirl(Collection<Player> viewers, Location center, int tick, double radius) {
        if (tick % 2 != 0) return;
        for (int strand = 0; strand < 3; strand++) {
            double angle = tick * .28 + strand * Math.PI * 2 / 3;
            Location at = center.clone().add(Math.cos(angle) * radius, .4 * Math.sin(angle * 1.7), Math.sin(angle) * radius);
            for (Player viewer : viewers) {
                viewer.spawnParticle(Particle.CLOUD, at, 1, .015, .015, .015, .003);
                if (tick % 8 == 0) viewer.spawnParticle(Particle.SMALL_GUST, at, 1, 0, 0, 0, 0);
            }
        }
    }

    static void burst(Collection<Player> viewers, Location center) {
        for (Player viewer : viewers) {
            viewer.spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1, 0, 0, 0, 0);
            viewer.spawnParticle(Particle.CLOUD, center, 32, .7, .5, .7, .06);
        }
    }
}
