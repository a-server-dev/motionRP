package dev.aserver.motionrp;

import org.bukkit.Location;

public interface MotionScene extends AutoCloseable {
    void start();
    default void refreshAudience() {}
    boolean ownsMove(Location from, Location to);
    @Override void close();
}
