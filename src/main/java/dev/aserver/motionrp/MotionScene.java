package dev.aserver.motionrp;

import org.bukkit.Location;

public interface MotionScene extends AutoCloseable {
    void start();
    default void refreshAudience() {}
    default boolean followsMovement(){return false;}
    boolean ownsMove(Location from, Location to);
    @Override void close();
}
