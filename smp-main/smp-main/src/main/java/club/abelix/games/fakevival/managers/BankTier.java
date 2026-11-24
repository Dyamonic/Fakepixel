package club.abelix.games.fakevival.managers;

import org.bukkit.Material;

import java.util.Locale;

public enum BankTier {
    STARTER("starter", "Starter", 100_000D, 0D, 1_000D, Material.CHEST),
    SMALL("small", "Small", 1_000_000D, 25_000D, 10_000D, Material.BARREL),
    LARGE("large", "Large", 10_000_000D, 250_000D, 100_000D, Material.ENDER_CHEST),
    VAULT("vault", "Vault", 100_000_000D, 2_500_000D, 1_000_000D, Material.VAULT),
    ROYAL("royal", "Royal", 1_000_000_000D, 25_000_000D, 10_000_000D, Material.NETHER_STAR);

    private final String id;
    private final String displayName;
    private final double capacity;
    private final double upgradeCost;
    private final double dailyInterestCap;
    private final Material icon;

    BankTier(String id, String displayName, double capacity, double upgradeCost, double dailyInterestCap, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.capacity = capacity;
        this.upgradeCost = upgradeCost;
        this.dailyInterestCap = dailyInterestCap;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public double capacity() {
        return capacity;
    }

    public double upgradeCost() {
        return upgradeCost;
    }

    public double dailyInterestCap() {
        return dailyInterestCap;
    }

    public Material icon() {
        return icon;
    }

    public BankTier next() {
        int next = ordinal() + 1;
        return next >= values().length ? null : values()[next];
    }

    public static BankTier parse(String value) {
        if (value == null || value.isBlank()) return STARTER;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (BankTier tier : values()) {
            if (tier.id.equals(normalized) || tier.name().equalsIgnoreCase(normalized)) return tier;
        }
        return STARTER;
    }
}
