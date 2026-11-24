package me.outerspace.thepit.perk;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PassiveDefinition {
    private final String id;
    private final String displayName;
    private final Material material;
    private final short data;
    private final int inventorySlot;
    private final double valuePerTier;
    private final String affectedDamage;
    private final int bonusGold;
    private final int bonusXp;
    private final Map<Integer, Tier> tiers;

    public PassiveDefinition(String id, String displayName, Material material, short data, int inventorySlot,
                             double valuePerTier, String affectedDamage, int bonusGold, int bonusXp,
                             Map<Integer, Tier> tiers) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.data = data;
        this.inventorySlot = inventorySlot;
        this.valuePerTier = valuePerTier;
        this.affectedDamage = affectedDamage;
        this.bonusGold = bonusGold;
        this.bonusXp = bonusXp;
        this.tiers = Collections.unmodifiableMap(new LinkedHashMap<>(tiers));
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public short data() { return data; }
    public int inventorySlot() { return inventorySlot; }
    public double valuePerTier() { return valuePerTier; }
    public String affectedDamage() { return affectedDamage; }
    public int bonusGold() { return bonusGold; }
    public int bonusXp() { return bonusXp; }
    public Map<Integer, Tier> tiers() { return tiers; }
    public Tier tier(int tier) { return tiers.get(tier); }

    public record Tier(int number, int price, int requiredLevel, String requiredRenownUpgrade) {}
}
