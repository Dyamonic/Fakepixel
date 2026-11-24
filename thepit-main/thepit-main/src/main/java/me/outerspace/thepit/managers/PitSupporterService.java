package me.outerspace.thepit.managers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PitSupporterService {
    public static final String ENTITLEMENT_ID = "pit_supporter";
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public boolean ownsPitSupporter(UUID playerId) {
        State state = states.get(playerId);
        return state != null && state.loaded && (state.purchased || state.granted || state.permissionEntitled);
    }

    public boolean isLoaded(UUID playerId) {
        State state = states.get(playerId);
        return state != null && state.loaded;
    }

    public boolean starEnabled(UUID playerId) {
        State state = states.get(playerId);
        return ownsPitSupporter(playerId) && state.starEnabled;
    }

    public boolean persistentOwnership(UUID playerId) {
        State state = states.get(playerId);
        return state != null && state.purchased;
    }

    public boolean legacyDetected(UUID playerId) {
        State state = states.get(playerId);
        return state != null && state.legacyDetected;
    }

    public void load(UUID playerId, boolean purchased, boolean granted, boolean permissionEntitled, boolean starEnabled, boolean legacyDetected) {
        states.put(playerId, new State(true, purchased, granted, permissionEntitled, starEnabled, legacyDetected));
    }

    public void setPurchased(UUID playerId, boolean value) { states.computeIfPresent(playerId, (id, state) -> state.purchased(value)); }
    public void setGranted(UUID playerId, boolean value) { states.computeIfPresent(playerId, (id, state) -> state.granted(value)); }
    public void setPermissionEntitled(UUID playerId, boolean value) { states.computeIfPresent(playerId, (id, state) -> state.permissionEntitled(value)); }
    public void setStarEnabled(UUID playerId, boolean value) { states.computeIfPresent(playerId, (id, state) -> state.starEnabled(value)); }
    public void unload(UUID playerId) { states.remove(playerId); }
    public void clear() { states.clear(); }

    private record State(boolean loaded, boolean purchased, boolean granted, boolean permissionEntitled,
                         boolean starEnabled, boolean legacyDetected) {
        private State purchased(boolean value) { return new State(loaded, value, granted, permissionEntitled, starEnabled, legacyDetected); }
        private State granted(boolean value) { return new State(loaded, purchased, value, permissionEntitled, starEnabled, legacyDetected); }
        private State permissionEntitled(boolean value) { return new State(loaded, purchased, granted, value, starEnabled, legacyDetected); }
        private State starEnabled(boolean value) { return new State(loaded, purchased, granted, permissionEntitled, value, legacyDetected); }
    }
}
