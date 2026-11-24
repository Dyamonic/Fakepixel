package me.outerspace.thepit.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class PitLevelChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerUuid;
    private final int oldLevel;
    private final int newLevel;
    private final long xpAwarded;
    private final String source;
    private final boolean administrative;
    private final boolean prestigeReset;
    private final boolean statisticsUnlocked;

    public PitLevelChangeEvent(UUID playerUuid, int oldLevel, int newLevel, long xpAwarded, String source,
                                    boolean administrative, boolean prestigeReset, boolean statisticsUnlocked,
                                    boolean asynchronous) {
        super(asynchronous);
        this.playerUuid = playerUuid;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.xpAwarded = xpAwarded;
        this.source = source;
        this.administrative = administrative;
        this.prestigeReset = prestigeReset;
        this.statisticsUnlocked = statisticsUnlocked;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public long getXpAwarded() {
        return xpAwarded;
    }

    public String getSource() {
        return source;
    }

    public boolean isAdministrative() {
        return administrative;
    }

    public boolean isPrestigeReset() {
        return prestigeReset;
    }

    public boolean isStatisticsUnlocked() {
        return statisticsUnlocked;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
