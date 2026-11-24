package me.outerspace.thepit.managers;

import org.bukkit.Location;

import java.util.UUID;

public final class PitDeathContext {
    private final UUID victimId;
    private final UUID killerId;
    private final int victimLevel;
    private final int killerLevel;
    private final int victimKillstreak;
    private final int killerKillstreak;
    private final UUID deathId;
    private final Location deathLocation;

    public PitDeathContext(UUID victimId, UUID killerId, int victimLevel, int killerLevel, int victimKillstreak, int killerKillstreak, UUID deathId, Location deathLocation) {
        this.victimId = victimId;
        this.killerId = killerId;
        this.victimLevel = victimLevel;
        this.killerLevel = killerLevel;
        this.victimKillstreak = victimKillstreak;
        this.killerKillstreak = killerKillstreak;
        this.deathId = deathId;
        this.deathLocation = deathLocation == null ? null : deathLocation.clone();
    }

    public UUID getVictimId() { return victimId; }
    public UUID getKillerId() { return killerId; }
    public int getVictimLevel() { return victimLevel; }
    public int getKillerLevel() { return killerLevel; }
    public int getVictimKillstreak() { return victimKillstreak; }
    public int getKillerKillstreak() { return killerKillstreak; }
    public UUID getDeathId() { return deathId; }
    public Location getDeathLocation() { return deathLocation == null ? null : deathLocation.clone(); }
}
