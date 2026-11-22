package dev.aserver.motionrp.core;
/** Player-only distribution rule; other administration permissions do not grant give. */
public final class GiveAccess {
 private GiveAccess(){}
 public static boolean playerAllowed(String name,boolean operator){return operator||"EmeraldREX91".equalsIgnoreCase(name);}
}
