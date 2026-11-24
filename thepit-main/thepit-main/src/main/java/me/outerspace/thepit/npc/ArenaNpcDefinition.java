package me.outerspace.thepit.npc;

public record ArenaNpcDefinition(String arenaId, String npcId, String typeId, double x, double y, double z,
                                 float yaw, float pitch, String displayName, String skinId, String skinName,
                                 String skinValue, String skinSignature, String actionId, String command,
                                 String executionMode, String hologramId, boolean lookAtPlayers, boolean enabled) {
}
