package me.outerspace.thepit.perk;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.PerkRegistry;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PassiveManager {
    public enum PurchaseResult {
        STARTED, DISABLED, NOT_FOUND, MAXIMUM, LEVEL, ONE_UPPER, INSUFFICIENT_GOLD, BUSY, WITHDRAWAL_FAILED, STORAGE_FAILED, SUCCESS
    }

    private final ThePitPlugin plugin;
    private final Map<String, PassiveDefinition> definitions = new LinkedHashMap<>();
    private final Set<UUID> transactions = ConcurrentHashMap.newKeySet();
    private boolean enabled;

    public PassiveManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public boolean reload() {
        PerkRegistry registry = plugin.registry().perks();
        Map<String, PassiveDefinition> loaded = new LinkedHashMap<>();
        boolean valid = true;
        for (String rawId : registry.keys("passives.definitions")) {
            String id = normalize(rawId);
            String base = "passives.definitions." + rawId;
            String materialName = registry.getString(base + ".material", "PAPER");
            Material material = Material.matchMaterial(materialName);
            if (material == null && "EXPERIENCE_BOTTLE".equalsIgnoreCase(materialName)) material = Material.EXP_BOTTLE;
            if (material == null) {
                plugin.getLogger().warning("Passive '" + id + "' has invalid material '" + materialName + "' and was disabled.");
                valid = false;
                continue;
            }
            Map<Integer, PassiveDefinition.Tier> tiers = new LinkedHashMap<>();
            for (int tier = 1; tier <= 6; tier++) {
                String tierBase = base + ".tiers." + tier;
                int price = registry.getInt(tierBase + ".price", -1);
                int level = registry.getInt(tierBase + ".required-level", -1);
                String renown = normalize(registry.getString(tierBase + ".required-renown-upgrade", ""));
                if (price < 0 || level < 1 || tier == 6 && renown.isEmpty()) {
                    plugin.getLogger().warning("Passive '" + id + "' tier " + tier + " is invalid and the passive was disabled.");
                    valid = false;
                    tiers.clear();
                    break;
                }
                tiers.put(tier, new PassiveDefinition.Tier(tier, price, level, renown));
            }
            if (tiers.size() != 6) continue;
            double perTier = registry.getDouble(base + ".bonus-per-tier",
                    registry.getDouble(base + ".reduction-per-tier",
                            registry.getDouble(base + ".duration-bonus-per-tier", 0.0D)));
            loaded.put(id, new PassiveDefinition(id, registry.getString(base + ".display-name", id), material,
                    (short) Math.max(0, Math.min(Short.MAX_VALUE, registry.getInt(base + ".data", 0))),
                    registry.getInt(base + ".inventory-slot", -1), perTier,
                    registry.getString(base + ".affected-damage", ""),
                    registry.getInt(base + ".bonus-gold", 0), registry.getInt(base + ".bonus-xp", 0), tiers));
        }
        definitions.clear();
        definitions.putAll(loaded);
        enabled = registry.getBoolean("passives.enabled", true) && !definitions.isEmpty();
        return valid;
    }

    public List<PassiveDefinition> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    public PassiveDefinition definition(String id) {
        return definitions.get(normalize(id));
    }

    public int tier(UUID uuid, String id) {
        return plugin.getStatsManager().getPassiveTier(uuid, normalize(id));
    }

    public double bonus(UUID uuid, String id) {
        PassiveDefinition definition = definition(id);
        return definition == null ? 0.0D : definition.valuePerTier() * tier(uuid, id);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean isBusy(UUID uuid) {
        return transactions.contains(uuid);
    }

    public PurchaseResult purchase(Player player, String rawId) {
        if (!enabled) return PurchaseResult.DISABLED;
        PassiveDefinition definition = definition(rawId);
        if (definition == null) return PurchaseResult.NOT_FOUND;
        UUID uuid = player.getUniqueId();
        if (plugin.getPrestigeManager().isProcessing(uuid)) return PurchaseResult.BUSY;
        if (!transactions.add(uuid)) return PurchaseResult.BUSY;
        int current = tier(uuid, definition.id());
        if (current >= 6) {
            transactions.remove(uuid);
            return PurchaseResult.MAXIMUM;
        }
        PassiveDefinition.Tier next = definition.tier(current + 1);
        if (plugin.getStatsManager().getLevel(uuid) < next.requiredLevel()) {
            transactions.remove(uuid);
            return PurchaseResult.LEVEL;
        }
        if (!next.requiredRenownUpgrade().isEmpty() && !plugin.getStatsManager().hasRenownUpgrade(uuid, next.requiredRenownUpgrade())) {
            transactions.remove(uuid);
            return PurchaseResult.ONE_UPPER;
        }
        int price = plugin.getRenownShopManager().discountedPrice(uuid, next.price());
        int balance = plugin.getPitGoldService().balance(uuid);
        if (balance < price) {
            transactions.remove(uuid);
            return PurchaseResult.INSUFFICIENT_GOLD;
        }
        String expected = plugin.getStatsManager().passiveData(uuid);
        String updated = plugin.getStatsManager().withPassiveTier(expected, definition.id(), next.number());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean persisted = plugin.getStatsManager().persistPassiveTransaction(uuid, player.getName(), expected, updated, balance, price);
            if (!persisted) {
                finish(player, definition, PurchaseResult.STORAGE_FAILED, current);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getStatsManager().applyPassivePurchase(uuid, updated, price);
                finish(player, definition, PurchaseResult.SUCCESS, next.number());
            });
        });
        return PurchaseResult.STARTED;
    }

    private void finish(Player player, PassiveDefinition definition, PurchaseResult result, int tier) {
        transactions.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (result == PurchaseResult.SUCCESS) {
                if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                        new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(player.getUniqueId(),
                                me.outerspace.thepit.managers.AchievementManager.EventType.PASSIVE_PURCHASED,
                                tier, java.util.Map.of("id", definition.id()), false));
                player.sendMessage(CC.color("&aPurchased " + definition.displayName() + " &at tier &e" + roman(tier) + "&a."));
                plugin.getPerkMenu().open(player);
            } else if (result == PurchaseResult.STORAGE_FAILED) {
                player.sendMessage(CC.color("&cThe upgrade could not be saved. No Gold was charged."));
            }
        });
    }

    public String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> "0";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }
}
