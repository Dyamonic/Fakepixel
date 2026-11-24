package me.outerspace.thepit.managers;

public final class KillXpResult {
    private final int baseXp;
    private final int firstKillsBonus;
    private final int levelDifferenceBonus;
    private final int killstreakBonus;
    private final int streakShutdownBonus;
    private final int passiveBonus;
    private final int perkBonus;
    private final int mysticBonus;
    private final double multiplier;
    private final int finalXp;

    public KillXpResult(int baseXp, int firstKillsBonus, int levelDifferenceBonus, int killstreakBonus, int streakShutdownBonus, int passiveBonus, int perkBonus, int mysticBonus, double multiplier, int finalXp) {
        this.baseXp = baseXp;
        this.firstKillsBonus = firstKillsBonus;
        this.levelDifferenceBonus = levelDifferenceBonus;
        this.killstreakBonus = killstreakBonus;
        this.streakShutdownBonus = streakShutdownBonus;
        this.passiveBonus = passiveBonus;
        this.perkBonus = perkBonus;
        this.mysticBonus = mysticBonus;
        this.multiplier = multiplier;
        this.finalXp = finalXp;
    }

    public int getBaseXp() { return baseXp; }
    public int getFirstKillsBonus() { return firstKillsBonus; }
    public int getLevelDifferenceBonus() { return levelDifferenceBonus; }
    public int getKillstreakBonus() { return killstreakBonus; }
    public int getStreakShutdownBonus() { return streakShutdownBonus; }
    public int getPassiveBonus() { return passiveBonus; }
    public int getPerkBonus() { return perkBonus; }
    public int getMysticBonus() { return mysticBonus; }
    public double getMultiplier() { return multiplier; }
    public int getFinalXp() { return finalXp; }
}
