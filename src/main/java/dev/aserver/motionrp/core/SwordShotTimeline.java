package dev.aserver.motionrp.core;
/** Double speed up to the first high front kick, followed by the original timing. */
public final class SwordShotTimeline {
 private SwordShotTimeline(){}
 public static int at(int reference){return reference<=78?(int)Math.round(reference/2.0):reference-39;}
 public static final int STRIKE=at(78),SHOT=at(110),IMPACT=at(123),END=at(129);
}
