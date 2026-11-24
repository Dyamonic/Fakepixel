package me.outerspace.thepit.perk;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.registry.PerkRegistry;
import me.outerspace.thepit.registry.RegistryNode;
import me.outerspace.thepit.registry.definition.PerkDefinition;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PerkManager {
    public enum Result { SUCCESS, NOT_FOUND, LOCKED, NOT_UNLOCKED, SLOT_LOCKED, INSUFFICIENT_COINS, STORAGE_UNAVAILABLE }
    public record Reward(int xp, int coins) {}

    private final ThePitPlugin plugin;
    private final Map<UUID, PerkProfile> profiles = new HashMap<>();
    private final File fallbackFile;
    private YamlConfiguration fallback = new YamlConfiguration();

    public PerkManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        fallbackFile = new File(plugin.getDataFolder(), "perk-profiles.yml");
    }

    public void initialize() {
        loadFallback();
        if (plugin.getStorageManager().isMysql()) {
            try {
                plugin.getStorageManager().update("CREATE TABLE IF NOT EXISTS buildffa_perk_profiles (uuid VARCHAR(36) NOT NULL, profile_data MEDIUMTEXT NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(uuid))");
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not create The Pit perk profile table: " + exception.getMessage());
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            load(player.getUniqueId());
            revalidate(player);
        }
    }

    public void reload() {
        Map<UUID, PerkProfile> retained = new HashMap<>(profiles);
        loadFallback();
        profiles.clear();
        for (UUID uuid : retained.keySet()) load(uuid);
        for (Player player : Bukkit.getOnlinePlayers()) {
            revalidate(player);
            applyHealth(player);
        }
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(profiles.keySet())) {
            PerkProfile profile = profiles.get(uuid);
            if (profile != null) saveData(uuid, profile.serialize());
        }
        profiles.clear();
    }

    public PerkProfile load(UUID uuid) {
        PerkProfile loaded = null;
        if (plugin.getStorageManager().isMongo()) {
            String value = plugin.getStorageManager().getMongoString(uuid.toString(), "LIFETIME", "all", "perk_profile_data", "");
            if (!value.isEmpty()) loaded = PerkProfile.deserialize(value);
        } else if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("SELECT profile_data FROM buildffa_perk_profiles WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) loaded = PerkProfile.deserialize(result.getString("profile_data"));
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not load perk profile " + uuid + ": " + exception.getMessage());
            }
        }
        if (loaded == null) {
            String unified = plugin.getStatsManager().fallbackPerkData(uuid);
            loaded = PerkProfile.deserialize(unified.isEmpty() ? fallback.getString("players." + uuid + ".data", "") : unified);
        }
        String before=loaded.serialize();
        profiles.put(uuid, loaded);
        normalize(uuid);
        if(!before.equals(loaded.serialize()))save(uuid);
        return loaded;
    }

    public void unload(UUID uuid) {
        save(uuid);
        profiles.remove(uuid);
    }

    public PerkProfile profile(UUID uuid) {
        PerkProfile profile = profiles.get(uuid);
        return profile == null ? load(uuid) : profile;
    }

    public boolean cached(UUID uuid) { return profiles.containsKey(uuid); }
    public PerkProfile cachedProfile(UUID uuid) { return profiles.get(uuid); }
    public Set<String> perkIds() { return registry().perkIds(); }
    public Optional<PerkDefinition> definition(String id) { return registry().getPerk(id); }
    public List<PerkDefinition> definitionOrder() { return new ArrayList<>(registry().all().values()); }

    public boolean has(Player player, String perk) {
        String id = registry().normalize(perk);
        PerkDefinition definition = registry().getPerk(id).orElse(null);
        if (definition == null || !definition.enabled()) return false;
        PerkProfile profile = profiles.get(player.getUniqueId());
        int slot = profile == null ? -1 : equippedSlot(profile, id);
        return profile != null && usable(profile, definition, id) && slot > 0 && profile.isSlotUnlocked(slot);
    }

    public boolean active(Player player, String perk) {
        return has(player, perk) && validParticipant(player);
    }

    public boolean validParticipant(Player player) {
        return player != null && player.isOnline() && !player.isDead() && player.getHealth() > 0.0D
                && plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) && !player.hasMetadata("NPC")
                && !plugin.isBuildMode(player) && plugin.getArenaManager().getCurrentArena() != null
                && plugin.getArenaManager().isInCurrentPvpArena(player.getLocation())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(player.getLocation());
    }

    public boolean validCombat(Player attacker, Player victim) {
        return attacker != null && victim != null && attacker != victim && validParticipant(attacker)
                && !victim.hasMetadata("NPC") && !plugin.isBuildMode(victim) && plugin.getArenaManager().isInCurrentPvpArena(victim.getLocation())
                && !plugin.getArenaManager().isInCurrentSpawnProtection(victim.getLocation())
                && attacker.getWorld() == victim.getWorld();
    }

    public synchronized Result purchaseAndEquip(Player player, int slot, String rawId) {
        String id = registry().normalize(rawId);
        PerkDefinition definition = registry().getPerk(id).orElse(null);
        if (definition == null || !definition.enabled()) return Result.NOT_FOUND;
        revalidate(player);
        PerkProfile profile = profile(player.getUniqueId());
        if (!profile.isSlotUnlocked(slot)) return Result.SLOT_LOCKED;
        if (usable(profile, definition, id)) return equip(player, slot, id);
        if (!meets(player, definition)) return Result.LOCKED;
        if (!plugin.getPitGoldService().has(player.getUniqueId(), definition.coinPrice())) return Result.INSUFFICIENT_COINS;
        String previous = profile.serialize();
        profile.unlock(id, System.currentTimeMillis());
        profile.equip(slot, id);
        if (!save(player.getUniqueId())) {
            profiles.put(player.getUniqueId(), PerkProfile.deserialize(previous));
            return Result.STORAGE_UNAVAILABLE;
        }
        if (!plugin.getPitGoldService().take(player, definition.coinPrice())) {
            profiles.put(player.getUniqueId(), PerkProfile.deserialize(previous));
            save(player.getUniqueId());
            return Result.INSUFFICIENT_COINS;
        }
        applyHealth(player);
        syncFishingRod(player);
        return Result.SUCCESS;
    }

    public Result equip(Player player, int slot, String rawId) {
        String id = registry().normalize(rawId);
        PerkDefinition definition = registry().getPerk(id).orElse(null);
        PerkProfile profile = profile(player.getUniqueId());
        if (definition == null || !definition.enabled()) return Result.NOT_FOUND;
        if (!usable(profile, definition, id)) return Result.NOT_UNLOCKED;
        if (!profile.isSlotUnlocked(slot)) return Result.SLOT_LOCKED;
        if (!meets(player, definition)) return Result.LOCKED;
        String previous = profile.serialize();
        profile.equip(slot, id);
        if (!save(player.getUniqueId())) {
            profiles.put(player.getUniqueId(), PerkProfile.deserialize(previous));
            return Result.STORAGE_UNAVAILABLE;
        }
        applyHealth(player);
        syncFishingRod(player);
        return Result.SUCCESS;
    }

    public Result unequip(Player player, int slot) {
        PerkProfile profile = profile(player.getUniqueId());
        if (profile.equipped(slot) == null) return Result.NOT_FOUND;
        String previous = profile.equipped(slot);
        profile.unequip(slot);
        if (!save(player.getUniqueId())) {
            profile.equip(slot, previous);
            return Result.STORAGE_UNAVAILABLE;
        }
        applyHealth(player);
        syncFishingRod(player);
        return Result.SUCCESS;
    }

    public Result grant(UUID uuid, String rawId) {
        String id = registry().normalize(rawId);
        if (registry().getPerk(id).isEmpty()) return Result.NOT_FOUND;
        PerkProfile profile = profile(uuid);
        String previous = profile.serialize();
        profile.unlock(id, System.currentTimeMillis());
        if (save(uuid)) return Result.SUCCESS;
        profiles.put(uuid, PerkProfile.deserialize(previous));
        return Result.STORAGE_UNAVAILABLE;
    }

    public Result remove(UUID uuid, String rawId) {
        String id = registry().normalize(rawId);
        PerkProfile profile = profile(uuid);
        if (!profile.isUnlocked(id)) return Result.NOT_UNLOCKED;
        String previous = profile.serialize();
        profile.remove(id);
        if (save(uuid)) return Result.SUCCESS;
        profiles.put(uuid, PerkProfile.deserialize(previous));
        return Result.STORAGE_UNAVAILABLE;
    }

    public Result adminEquip(UUID uuid, int slot, String rawId) {
        if (slot < 1 || slot > 4) return Result.SLOT_LOCKED;
        Player player = Bukkit.getPlayer(uuid);
        String id = registry().normalize(rawId);
        PerkProfile profile = profile(uuid);
        if (registry().getPerk(id).isEmpty()) return Result.NOT_FOUND;
        String previous = profile.serialize();
        profile.unlock(id, System.currentTimeMillis());
        profile.unlockSlot(slot);
        profile.equip(slot, id);
        if (!save(uuid)) { profiles.put(uuid,PerkProfile.deserialize(previous));return Result.STORAGE_UNAVAILABLE; }
        if (player != null) applyHealth(player);
        return Result.SUCCESS;
    }

    public Result adminUnlockSlot(UUID uuid, int slot) {
        if (slot < 1 || slot > 4) return Result.SLOT_LOCKED;
        PerkProfile profile = profile(uuid);
        String previous = profile.serialize();
        profile.unlockPermanentSlot(slot);
        if (save(uuid)) return Result.SUCCESS;
        profiles.put(uuid,PerkProfile.deserialize(previous));
        return Result.STORAGE_UNAVAILABLE;
    }

    public Result reset(UUID uuid) {
        PerkProfile existing=profile(uuid);String previous=existing.serialize();
        profiles.put(uuid, new PerkProfile());
        Player player = Bukkit.getPlayer(uuid);
        if(save(uuid)){if(player!=null)applyHealth(player);return Result.SUCCESS;}
        profiles.put(uuid,PerkProfile.deserialize(previous));
        return Result.STORAGE_UNAVAILABLE;
    }

    public String prestigeResetData(UUID uuid, int nextPrestige) {
        PerkProfile copy = PerkProfile.deserialize(profile(uuid).serialize());
        copy.resetForPrestige(
                plugin.registry().settings().getBoolean("prestige.reset.temporary-perk-purchases", true),
                plugin.registry().settings().getBoolean("prestige.reset.equipped-perks", true)
        );
        int nextLevel = 1;
        for (int slot = 1; slot <= 4; slot++) {
            int level = registry().getInt("loadout-slots." + slot + ".required-level", 1);
            int prestige = registry().getInt("loadout-slots." + slot + ".required-prestige", 0);
            boolean levelMet = plugin.getStatsManager().hasRenownUpgrade(uuid, "the-way") || nextLevel >= level;
            boolean renownMet = slot != 4 || plugin.getStatsManager().hasRenownUpgrade(uuid, "extra-perk-slot");
            if (levelMet && nextPrestige >= prestige && renownMet) copy.unlockSlot(slot);
            else copy.lockSlot(slot);
        }
        return copy.serialize();
    }

    public void applyPersistedProfile(UUID uuid, String serialized) {
        profiles.put(uuid, PerkProfile.deserialize(serialized));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            applyHealth(player);
            syncFishingRod(player);
        }
    }

    public boolean persistFallbackProfile(UUID uuid, String serialized) {
        fallback.set("players." + uuid + ".data", serialized);
        return saveFallback();
    }

    public Reward modifyKillReward(Player killer, Player victim, int xp, int coins, int nextStreak) {
        if (!validCombat(killer, victim)) return new Reward(xp, coins);
        RegistryNode settings;
        if (has(killer, "bounty_hunter")) coins += value("bounty_hunter").getInt("mechanics.kill-coin-bonus", 4);
        if (has(killer, "streaker") && nextStreak >= value("streaker").getInt("mechanics.minimum-streak", 1)) {
            settings = value("streaker");
            coins = multiply(coins, settings.getDouble("mechanics.coin-multiplier", 1.0D));
            xp = multiply(xp, settings.getDouble("mechanics.xp-multiplier", 3.0D));
        }
        if (has(killer, "conglomerate")) {
            double percentage = value("conglomerate").getDouble("mechanics.xp-to-coins-percent", 20.0D) / 100.0D;
            coins += Math.max(0, (int) Math.round(xp * percentage));
            xp = multiply(xp, Math.max(0.0D, 1.0D - percentage));
        }
        return new Reward(Math.max(0, xp), Math.max(0, coins));
    }

    public int collectibleCoinReward(Player player, int base) {
        if (!active(player, "trickle_down")) return base;
        RegistryNode settings = value("trickle_down");
        heal(player, settings.getDouble("mechanics.healing", 4.0D));
        int bonus = Math.max(0, settings.getInt("mechanics.bonus-coins", 10));
        if (bonus > 0) {
            plugin.getStatsManager().addLongStat(player.getUniqueId(), "trickle_down_bonus_gold", bonus);
            if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                    new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(player.getUniqueId(),
                            me.outerspace.thepit.managers.AchievementManager.EventType.EVENT_SCORE_CHANGED,
                            plugin.getStatsManager().getLongStat(player.getUniqueId(), "trickle_down_bonus_gold"),
                            java.util.Map.of("id", "trickle_down_bonus_gold"), false));
        }
        return base + bonus;
    }

    public void onKill(Player killer, Player victim) {
        if (!validCombat(killer, victim)) return;
        if (has(killer, "rambo")) killer.setHealth(killer.getMaxHealth());
        else if (has(killer, "vampire")) { }
        else if (has(killer, "olympus")) giveLimited(killer, special("olympus", Material.POTION), "olympus", limit(killer, "olympus", value("olympus").getInt("mechanics.carrying-limit", 1)));
        else if (has(killer, "soup")) giveLimited(killer, special("kill_soup", Material.MUSHROOM_SOUP), "kill_soup", limit(killer, "soup", value("soup").getInt("mechanics.total-soup-limit", 4)));
        else if (has(killer, "golden_heads")) giveLimited(killer, special("golden_head", Material.SKULL_ITEM, (short) 3), "golden_head", limit(killer, "golden_heads", value("golden_heads").getInt("mechanics.carrying-limit", 2)));
        if (has(killer, "dirty")) effect(killer, "dirty", "mechanics.resistance");
        if (has(killer, "vampire")) effect(killer, "vampire", "mechanics.regeneration");
        if (has(killer, "mineman")) giveTemporaryBlocks(killer, material(value("mineman").getString("mechanics.block-material", "COBBLESTONE"), Material.COBBLESTONE), value("mineman").getInt("mechanics.blocks-per-kill", 3), value("mineman").getInt("mechanics.block-limit", 64));
        if (has(killer, "spammer") && victim.getLastDamageCause() != null && victim.getLastDamageCause().getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE) {
            plugin.getPitGoldService().award(killer, value("spammer").getInt("mechanics.bow-kill-coin-bonus", 4), ThePitPlugin.GoldSource.EVENT);
        }
    }

    public void onLuckyDeath(Player killer, Player victim) {
        if (validCombat(killer, victim) && has(killer, "lucky_diamond")) upgradeArmor(killer, victim);
    }

    public void onAssist(Player player, double participation) {
        if (!validParticipant(player)) return;
        plugin.getStatsManager().recordAssist(player);
        int coins = registry().getInt("assists.base-coins", 2);
        int xp = plugin.registry().settings().getInt("levels.rewards.assist-xp", 5);
        if (has(player, "spammer")) coins += value("spammer").getInt("mechanics.assist-coin-bonus", 2);
        if (has(player, "co_op_cat")) {
            coins = multiply(coins, value("co_op_cat").getDouble("mechanics.assist-coin-multiplier", 1.5D));
            xp = multiply(xp, value("co_op_cat").getDouble("mechanics.assist-xp-multiplier", 1.5D));
        }
        if (has(player, "assistant_streaker") && participation >= value("assistant_streaker").getDouble("mechanics.required-participation", 0.5D)) {
            coins += value("assistant_streaker").getInt("mechanics.assist-coin-bonus", 2);
            xp += value("assistant_streaker").getInt("mechanics.assist-xp-bonus", 1);
            plugin.getStatsManager().addKillstreakProgress(player, 1);
        }
        if (has(player, "conglomerate")) {
            double percentage = value("conglomerate").getDouble("mechanics.xp-to-coins-percent", 20.0D) / 100.0D;
            coins += (int) Math.round(xp * percentage);
            xp = multiply(xp, 1.0D - percentage);
        }
        plugin.getPitGoldService().award(player, Math.max(0, coins), ThePitPlugin.GoldSource.ASSIST);
        plugin.getStatsManager().addXp(player, Math.max(0, xp), me.outerspace.thepit.managers.StatsManager.XpSource.ASSIST);
        if (has(player, "soup")) giveLimited(player, special("assist_soup", Material.MUSHROOM_SOUP), "assist_soup", limit(player, "soup", value("soup").getInt("mechanics.total-soup-limit", 4)));
    }

    public void applyLoadout(Player player) {
        syncFishingRod(player);
        if (!validParticipant(player)) return;
        applyCombatLoadout(player);
    }

    public void applySpawnEquipment(Player player) {
        if (player == null || !player.isOnline() || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return;
        syncFishingRod(player);
        applyCombatLoadout(player);
    }

    public void syncFishingRod(Player player) {
        removeTaggedInventory(player, "fishing_rod");
        if (has(player, "fishing_rod")) {
            ItemStack rod=new ItemStack(Material.FISHING_ROD,Math.max(1,value("fishing_rod").getInt("mechanics.spawn-amount",1)));
            rod=SecureItemTag.string(SecureItemTag.unbreakable(rod),"ThePitItem","fishing_rod");
            giveIfMissing(player,rod);
        }
        player.updateInventory();
    }

    private void applyCombatLoadout(Player player) {
        if (has(player, "lava_bucket")) {
            ItemStack bucket=new ItemStack(Material.LAVA_BUCKET,Math.max(1,value("lava_bucket").getInt("mechanics.spawn-amount",1)));
            giveIfMissing(player,bucket);
        }
        if (has(player, "safety_first")) {
            ItemStack helmet = special("safety_helmet", material(value("safety_first").getString("mechanics.helmet-material","CHAINMAIL_HELMET"),Material.CHAINMAIL_HELMET));
            player.getInventory().setHelmet(helmet);
        }
        if (has(player, "bounty_hunter") && value("bounty_hunter").getBoolean("mechanics.require-armor", true)) player.getInventory().setLeggings(special("bounty_armor", material(value("bounty_hunter").getString("mechanics.armor-material","GOLD_LEGGINGS"),Material.GOLD_LEGGINGS)));
        if (has(player, "mineman")) {
            RegistryNode settings = value("mineman");
            ItemStack pickaxe = new ItemStack(material(settings.getString("mechanics.pickaxe-material", "DIAMOND_PICKAXE"), Material.DIAMOND_PICKAXE));
            int efficiency = settings.getInt("mechanics.pickaxe-efficiency", 4);
            if (efficiency > 0) pickaxe.addUnsafeEnchantment(Enchantment.DIG_SPEED, efficiency);
            giveIfMissing(player, pickaxe);
            giveTemporaryBlocks(player, material(settings.getString("mechanics.block-material", "COBBLESTONE"), Material.COBBLESTONE), settings.getInt("mechanics.spawn-blocks", 24), settings.getInt("mechanics.block-limit", 64));
        }
        if (has(player, "barbarian")) convertSwords(player);
        if (has(player, "marathon")) player.getInventory().setBoots(null);
        applyHealth(player);
        player.updateInventory();
    }

    public void applyHealth(Player player) {
        double max = 20.0D;
        if (has(player, "rambo")) max = value("rambo").getDouble("mechanics.maximum-health", 16.0D);
        if (plugin.getRenownShopManager() != null) {
            int tier = plugin.getRenownShopManager().tier(player.getUniqueId(), "extra-hearts");
            max += tier * plugin.getRenownShopManager().effect("extra-hearts", "hearts-per-tier", 1.0D) * 2.0D;
        }
        if (has(player, "thick")) max += value("thick").getDouble("mechanics.additional-health", 4.0D);
        max = Math.max(2.0D, max);
        try {
            player.setMaxHealth(max);
            if (player.getHealth() > max) player.setHealth(max);
        } catch (Throwable ignored) { }
    }

    public void arenaCleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyHealth(player);
            removeProtectedItems(player);
        }
    }

    public void removeProtectedItems(Player player) {
        if (!has(player, "fishing_rod")) removeTaggedInventory(player, "fishing_rod");
        if (!has(player, "safety_first") && tagged(player.getInventory().getHelmet(), "safety_helmet")) player.getInventory().setHelmet(null);
        if (!has(player, "bounty_hunter") && tagged(player.getInventory().getLeggings(), "bounty_armor")) player.getInventory().setLeggings(null);
    }

    public void restoreProtectedArmor(Player player) {
        if (has(player, "safety_first") && !tagged(player.getInventory().getHelmet(), "safety_helmet")) player.getInventory().setHelmet(special("safety_helmet", material(value("safety_first").getString("mechanics.helmet-material","CHAINMAIL_HELMET"),Material.CHAINMAIL_HELMET)));
        if (has(player, "bounty_hunter") && value("bounty_hunter").getBoolean("mechanics.require-armor", true) && !tagged(player.getInventory().getLeggings(), "bounty_armor")) player.getInventory().setLeggings(special("bounty_armor", material(value("bounty_hunter").getString("mechanics.armor-material","GOLD_LEGGINGS"),Material.GOLD_LEGGINGS)));
    }

    public boolean protectedItem(ItemStack item) {
        return tagged(item, "safety_helmet") || tagged(item, "bounty_armor");
    }

    public boolean special(ItemStack item, String id) { return tagged(item, id); }
    public RegistryNode value(String id) { return registry().getPerk(id).map(PerkDefinition::settings).orElse(RegistryNode.empty()); }
    public String display(String id) { return registry().getPerk(id).map(PerkDefinition::displayName).orElse(id); }
    public String equippedName(UUID uuid, int slot) {
        String id = profile(uuid).equipped(slot);
        return id == null ? "None" : CC.color(display(id));
    }
    public int unlockedCount(UUID uuid) { return profile(uuid).unlockedPerks().size(); }
    public int slotCount(UUID uuid) { return profile(uuid).unlockedSlots().size(); }
    public boolean meets(Player player, PerkDefinition definition) {
        String unlock = renownUnlock(definition.id());
        boolean levelMet = plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "the-way")
                || plugin.getStatsManager().getLevel(player.getUniqueId()) >= definition.requiredLevel();
        return levelMet && plugin.getStatsManager().getPrestige(player.getUniqueId()) >= definition.requiredPrestige()
                && (unlock.isEmpty() || plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), unlock));
    }
    public boolean meetsSlot(Player player, int slot) {
        if (slot < 1 || slot > 4) return false;
        int level = registry().getInt("loadout-slots." + slot + ".required-level", 1);
        int prestige = registry().getInt("loadout-slots." + slot + ".required-prestige", 0);
        boolean levelMet = plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "the-way")
                || plugin.getStatsManager().getLevel(player.getUniqueId()) >= level;
        return levelMet && plugin.getStatsManager().getPrestige(player.getUniqueId()) >= prestige
                && (slot != 4 || plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "extra-perk-slot"));
    }

    private String renownUnlock(String id) {
        if (id == null) return "";
        if (id.equals("co_op_cat")) return "co-op-cat";
        if (id.equals("assistant_streaker")) return "assistant-streaker";
        if (id.equals("kung_fu_knowledge")) return "kung-fu-knowledge";
        if (java.util.Arrays.asList("overheal", "barbarian", "dirty", "rambo", "olympus", "first_strike",
                "marathon", "soup", "recon", "conglomerate", "thick").contains(id)) return id.replace('_', '-');
        return "";
    }

    public void revalidate(Player player) {
        if (player == null) return;
        PerkProfile profile = profile(player.getUniqueId());
        String previous = profile.serialize();
        for (int slot = 1; slot <= 4; slot++) {
            if (meetsSlot(player, slot)) profile.unlockSlot(slot);
            else profile.lockSlot(slot);
        }
        normalize(player.getUniqueId());
        for (int slot = 1; slot <= 4; slot++) {
            String id = profile.equipped(slot);
            PerkDefinition definition = id == null ? null : registry().getPerk(id).orElse(null);
            if (definition != null && !meets(player, definition)) profile.unequip(slot);
        }
        if (!previous.equals(profile.serialize())) save(player.getUniqueId());
        syncFishingRod(player);
    }

    public String requiredLevel(int slot) {
        int level = registry().getInt("loadout-slots." + slot + ".required-level", 1);
        int prestige = registry().getInt("loadout-slots." + slot + ".required-prestige", 0);
        String value = plugin.getStatsManager().getLevelColorFor(level) + "[" + level + "]";
        return prestige > 0 ? value + " &7and Prestige &d" + prestige : value;
    }

    public int equippedSlot(PerkProfile profile, String rawId) {
        String id = registry().normalize(rawId);
        for (Map.Entry<Integer, String> entry : profile.equipped().entrySet()) {
            if (id.equals(entry.getValue())) return entry.getKey();
        }
        return -1;
    }

    public ItemStack special(String id, Material material) { return special(id, material, (short) 0); }
    public ItemStack special(String id, Material material, short data) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(registry().getString("items." + id + ".display-name", "&6" + id.replace('_', ' '))));
        List<String> lore = new ArrayList<>();
        for (String line : registry().getStringList("items." + id + ".lore")) lore.add(CC.color(line));
        lore.add(CC.color("&8buildffa-perk:" + id));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void effect(Player player, String perk, String path) {
        RegistryNode settings = value(perk);
        String type = settings.getString(path + ".type", "SPEED");
        PotionEffectType effect = PotionEffectType.getByName(type.toUpperCase(Locale.ENGLISH));
        if (effect == null) return;
        int duration = Math.max(1, settings.getInt(path + ".duration-seconds", 5)) * 20;
        int level = Math.max(1, settings.getInt(path + ".level", 1)) - 1;
        player.addPotionEffect(new PotionEffect(effect, duration, level), true);
    }

    public void heal(Player player, double amount) {
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + Math.max(0.0D, amount)));
    }

    public int limit(Player player, String perk, int normal) {
        if (has(player, "overheal")) return Math.max(normal, multiply(normal, value("overheal").getDouble("mechanics.carrying-limit-multiplier", 2.0D)));
        return normal;
    }

    public int count(Player player, String id) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (tagged(item, id) || id.equals("kill_soup") && tagged(item, "assist_soup") || id.equals("assist_soup") && tagged(item, "kill_soup")) total += item.getAmount();
        }
        return total;
    }

    public void giveLimited(Player player, ItemStack item, String id, int limit) {
        int available = Math.max(0, limit - count(player, id));
        if (available <= 0) return;
        item.setAmount(Math.min(item.getAmount(), available));
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        if (!remaining.isEmpty()) player.getWorld().dropItemNaturally(player.getLocation(), remaining.values().iterator().next());
    }

    public void play(Player player, String perk, String path) {
        String name = value(perk).getString(path, "");
        if (name.isEmpty()) return;
        try { player.playSound(player.getLocation(), Sound.valueOf(name), 1.0F, 1.0F); } catch (Throwable ignored) { }
    }

    private PerkRegistry registry() { return plugin.registry().perks(); }
    private boolean usable(PerkProfile profile, PerkDefinition definition, String id) {
        return profile.isTemporarilyUnlocked(id)
                || profile.isPermanentlyUnlocked(id) && !definition.settings().getBoolean("renown-repurchase-required", true);
    }
    private void normalize(UUID uuid) {
        PerkProfile profile = profiles.get(uuid);
        if (profile == null) return;
        for (String id : new ArrayList<>(profile.unlockedPerks())) {
            PerkDefinition definition = registry().getPerk(id).orElse(null);
            if (definition == null || !definition.enabled()) profile.remove(id);
        }
        for (int slot = 1; slot <= 4; slot++) {
            String id = profile.equipped(slot);
            PerkDefinition definition=id==null?null:registry().getPerk(id).orElse(null);
            if (id != null && (!profile.isSlotUnlocked(slot) || definition == null || !definition.enabled() || !usable(profile, definition, id))) profile.unequip(slot);
        }
    }
    private boolean save(UUID uuid) {
        PerkProfile profile = profiles.get(uuid);
        if (profile == null) return true;
        String data = profile.serialize();
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveData(uuid, data));
            return true;
        }
        return saveData(uuid, data);
    }

    private boolean saveData(UUID uuid, String data) {
        if (plugin.getStorageManager().isMongo()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("perk_profile_data", data);
            return plugin.getStorageManager().setMongoFields(uuid.toString(), "", "LIFETIME", "all", fields);
        }
        if (plugin.getStorageManager().isMysql()) {
            try (PreparedStatement statement = plugin.getStorageManager().prepare("INSERT INTO buildffa_perk_profiles(uuid,profile_data) VALUES(?,?) ON DUPLICATE KEY UPDATE profile_data=VALUES(profile_data)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, data);
                statement.executeUpdate();
                return true;
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not save perk profile " + uuid + " to MySQL: " + exception.getMessage());
            }
        }
        fallback.set("players." + uuid + ".data", data);
        boolean saved = saveFallback();
        return saved && plugin.getStatsManager().persistFallbackPerkData(uuid, data);
    }
    private void loadFallback() {
        fallback = new YamlConfiguration();
        if (!fallbackFile.exists()) return;
        try { fallback.load(fallbackFile); }
        catch (Exception exception) { plugin.getLogger().warning("Could not load perk-profiles.yml: " + exception.getMessage()); }
    }
    private boolean saveFallback() {
        try {
            fallbackFile.getParentFile().mkdirs();
            File temporary = new File(fallbackFile.getParentFile(), fallbackFile.getName() + ".tmp");
            fallback.save(temporary);
            try { Files.move(temporary.toPath(), fallbackFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary.toPath(), fallbackFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save perk-profiles.yml: " + exception.getMessage());
            return false;
        }
    }
    private void giveIfMissing(Player player, ItemStack item) {
        if (SecureItemTag.is(item, "fishing_rod")) {
            for (ItemStack existing : player.getInventory().getContents()) if (SecureItemTag.is(existing, "fishing_rod")) return;
        } else if (player.getInventory().contains(item.getType())) return;
        player.getInventory().addItem(item);
    }
    private void removeTaggedInventory(Player player, String id) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (SecureItemTag.is(item, id)) player.getInventory().setItem(slot, null);
        }
    }
    private void giveAmount(Player player, Material material, int amount, int limit) {
        int existing = 0;
        for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == material) existing += item.getAmount();
        int add = Math.min(Math.max(0, amount), Math.max(0, limit - existing));
        if (add > 0) player.getInventory().addItem(new ItemStack(material, add));
    }
    private void giveTemporaryBlocks(Player player, Material material, int amount, int limit) {
        int existing = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            if ("MINEMAN_COBBLESTONE".equals(SecureItemTag.string(item, "ThePitItem"))) existing += item.getAmount();
        }
        int add = Math.min(Math.max(0, amount), Math.max(0, limit - existing));
        if (add <= 0) return;
        ItemStack blocks = SecureItemTag.string(new ItemStack(material, add), "ThePitItem", "MINEMAN_COBBLESTONE");
        player.getInventory().addItem(blocks);
    }
    private boolean tagged(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) if (org.bukkit.ChatColor.stripColor(line).equalsIgnoreCase("buildffa-perk:" + id)) return true;
        return false;
    }
    private void convertSwords(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null) continue;
            Material replacement = switch (item.getType()) {
                case WOOD_SWORD -> Material.WOOD_AXE;
                case STONE_SWORD -> Material.STONE_AXE;
                case IRON_SWORD -> Material.IRON_AXE;
                case DIAMOND_SWORD -> Material.DIAMOND_AXE;
                case GOLD_SWORD -> Material.GOLD_AXE;
                default -> null;
            };
            if (replacement != null) { ItemStack converted=item.clone();converted.setType(replacement);player.getInventory().setItem(slot,converted); }
        }
    }
    private void upgradeArmor(Player killer, Player victim) {
        double chance = value("lucky_diamond").getDouble("mechanics.upgrade-chance", 0.30D);
        for (ItemStack armor : victim.getInventory().getArmorContents()) {
            if (armor == null || Math.random() > chance) continue;
            Material upgraded = switch (armor.getType()) {
                case IRON_BOOTS -> Material.DIAMOND_BOOTS;
                case IRON_LEGGINGS -> Material.DIAMOND_LEGGINGS;
                case IRON_CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
                default -> null;
            };
            if (upgraded != null) {
                Map<Integer,ItemStack> remaining=killer.getInventory().addItem(new ItemStack(upgraded));
                for(ItemStack item:remaining.values())killer.getWorld().dropItemNaturally(killer.getLocation(),item);
                plugin.getStatsManager().addLongStat(killer.getUniqueId(), "lucky_diamond_armor", 1L);
                if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                        new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(killer.getUniqueId(),
                                me.outerspace.thepit.managers.AchievementManager.EventType.EVENT_SCORE_CHANGED,
                                plugin.getStatsManager().getLongStat(killer.getUniqueId(), "lucky_diamond_armor"),
                                java.util.Map.of("id", "lucky_diamond_armor"), false));
            }
        }
    }
    private Material material(String name, Material fallback) { Material value=Material.matchMaterial(name);return value==null?fallback:value; }
    private int multiply(int value,double multiplier){return Math.max(0,(int)Math.round(value*multiplier));}
}
