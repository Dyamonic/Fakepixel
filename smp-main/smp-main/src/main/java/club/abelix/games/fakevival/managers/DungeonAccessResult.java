package club.abelix.games.fakevival.managers;

import java.util.List;

public final class DungeonAccessResult {
    private final boolean exists;
    private final boolean enabled;
    private final boolean requirementsMet;
    private final boolean playerLevelMet;
    private final boolean combatLevelMet;
    private final boolean keyMet;
    private final boolean permissionMet;
    private final List<String> reasons;

    public DungeonAccessResult(boolean exists, boolean enabled, boolean requirementsMet,
                               boolean playerLevelMet, boolean combatLevelMet, boolean keyMet,
                               boolean permissionMet, List<String> reasons) {
        this.exists = exists;
        this.enabled = enabled;
        this.requirementsMet = requirementsMet;
        this.playerLevelMet = playerLevelMet;
        this.combatLevelMet = combatLevelMet;
        this.keyMet = keyMet;
        this.permissionMet = permissionMet;
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public boolean exists() { return exists; }
    public boolean enabled() { return enabled; }
    public boolean requirementsMet() { return requirementsMet; }
    public boolean playerLevelMet() { return playerLevelMet; }
    public boolean combatLevelMet() { return combatLevelMet; }
    public boolean keyMet() { return keyMet; }
    public boolean permissionMet() { return permissionMet; }
    public List<String> reasons() { return reasons; }
    public boolean allowed() { return exists && enabled && requirementsMet; }
}
