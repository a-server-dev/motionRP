package dev.aserver.motionrp;

import org.bukkit.Location;

public interface MotionScene extends AutoCloseable {
    void start();
    default void refreshAudience() {}
    default boolean followsMovement(){return false;}
    default void externalTeleport() {}
    boolean ownsMove(Location from, Location to);
    @Override void close();
}
