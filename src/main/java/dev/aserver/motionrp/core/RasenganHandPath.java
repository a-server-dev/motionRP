package dev.aserver.motionrp.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Palm attachment baked by generate_rasengan.py from the shipped body rig. */
public final class RasenganHandPath {
    public record Hand(double right, double forward, double height) {}
    private static final Hand[] PATH = read("rasengan", RasenganTimeline.END);
    private static final Hand[] SHURIKEN = read("rasenshuriken", RasenshurikenTimeline.END);
    private RasenganHandPath() {}
    public static Hand at(int tick) { return PATH[Math.clamp(tick, 0, PATH.length - 1)]; }
    public static Hand shurikenAt(int tick) { return SHURIKEN[Math.clamp(tick, 0, SHURIKEN.length - 1)]; }

    private static Hand[] read(String name, int length) {
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                RasenganHandPath.class.getResourceAsStream("/bettermodel/" + name + "-hand-path.csv")), StandardCharsets.UTF_8))) {
            Hand[] path = reader.lines().map(line -> {
                String[] parts = line.split(",");
                if (parts.length != 3) throw new IllegalStateException("Invalid Rasengan palm attachment");
                double x = Double.parseDouble(parts[0]), z = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]);
                if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(y))
                    throw new IllegalStateException("Non-finite Rasengan palm attachment");
                return new Hand(x, z, y);
            }).toArray(Hand[]::new);
            if (path.length != length) throw new IllegalStateException("Rasengan attachment duration mismatch");
            return path;
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
}
