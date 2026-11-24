package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PrestigeManager implements Listener {
    private final ThePitPlugin plugin;
    private final Map<UUID, PrestigeConfirmationSession> confirmations = new ConcurrentHashMap<>();

    public PrestigeManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PrestigeHolder holder = new PrestigeHolder();
        int size = menuSize();
        Inventory inventory = Bukkit.createInventory(holder, size, CC.color(plugin.registry().settings().getString("prestige.menu.title", "&8Prestige")));
        holder.inventory = inventory;
        holder.prestigeSlot = boundedSlot(plugin.registry().settings().getInt("prestige.menu.prestige-item.slot", 13), size);
        inventory.setItem(holder.prestigeSlot, prestigeIcon(player));
        if (plugin.getStatsManager().getPrestige(player.getUniqueId()) >= 1) {
            holder.renownSlot = adjacentEmptySlot(inventory, holder.prestigeSlot);
            if (holder.renownSlot >= 0) inventory.setItem(holder.renownSlot, renownIcon(player));
        }
        player.openInventory(inventory);
    }

    public void openConfirmation(Player player) {
        Requirement requirement = requirement(player);
        if (requirement != Requirement.READY) {
            sendRequirement(player, requirement);
            open(player);
            return;
        }
        UUID playerId = player.getUniqueId();
        PrestigeConfirmationSession previous = confirmations.remove(playerId);
        if (previous != null) previous.cancelExpiration();
        StatsManager.PrestigeState state = plugin.getStatsManager().prestigeState(playerId);
        int reward = plugin.getStatsManager().getRenownReward(state.prestige() + 1);
        UUID sessionId = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + 30000L;
        PrestigeConfirmationSession session = new PrestigeConfirmationSession(sessionId, state.prestige(), state.level(), expiresAt);
        confirmations.put(playerId, session);
        BukkitTask expirationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> expireConfirmation(playerId, sessionId), 600L);
        session.setExpirationTask(expirationTask);
        ConfirmHolder holder = new ConfirmHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Confirm Prestige"));
        holder.inventory = inventory;
        inventory.setItem(11, item(Material.REDSTONE_BLOCK, "&cCancel", Arrays.asList("&7Return without prestiging.")));
        inventory.setItem(15, item(Material.EMERALD_BLOCK, "&aConfirm Prestige", Arrays.asList(
                "&7Resets level, XP, temporary perks,",
                "&7combat inventory, bounty, and streak.",
                "",
                "&7Keeps lifetime stats, Renown,",
                "&7permanent unlocks, Fishing Club,",
                "&7Ender Chest contents, and permanent unlocks.",
                "",
                "&e+" + reward + " Renown",
                "",
                "&eClick again to confirm."
        )));
        player.openInventory(inventory);
    }

    public void confirm(Player player) {
        UUID playerId = player.getUniqueId();
        PrestigeConfirmationSession session = confirmations.get(playerId);
        if (session == null || session.isExpired()) {
            clearConfirmation(playerId, session);
            player.sendMessage(CC.color("&cYour prestige confirmation expired."));
            return;
        }
        if (!session.beginProcessing()) {
            if (session.state() == PrestigeTransactionState.PROCESSING) {
                player.sendMessage(message("processing", "&cYour prestige is already being processed."));
            } else if (session.state() == PrestigeTransactionState.AWAITING_CONFIRMATION) {
                player.sendMessage(message("failed", "&cPrestiging failed. Nothing was changed."));
            }
            return;
        }
        session.cancelExpiration();
        StatsManager.PrestigeState state = plugin.getStatsManager().prestigeState(playerId);
        if (state.prestige() != session.prestige || state.level() != session.level) {
            failSession(player, session, "Prestige profile changed before processing.");
            return;
        }
        Requirement requirement = requirementForProcessing(player, session);
        if (requirement != Requirement.READY) {
            session.fail();
            clearConfirmation(playerId, session);
            sendRequirement(player, requirement);
            return;
        }
        int nextPrestige = state.prestige() + 1;
        int reward = plugin.getStatsManager().getRenownReward(nextPrestige);
        long nextRenown = safeAdd(state.renown(), reward);
        long timestamp = System.currentTimeMillis();
        String perkData = plugin.getPerkManager().prestigeResetData(playerId, nextPrestige);
        long requiredCoins = plugin.getStatsManager().getPrestigeCoinsRequired(playerId);
        boolean consumeCurrency = plugin.registry().settings().getBoolean("prestige.consume-currency", true);
        boolean bypassCooldown = me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.prestige.cooldown.bypass");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean persisted;
            try {
                persisted = plugin.getStatsManager().persistPrestigeTransaction(playerId, player.getName(), state, nextPrestige, nextRenown, timestamp, requiredCoins, consumeCurrency, bypassCooldown, perkData);
            } catch (Throwable throwable) {
                Bukkit.getScheduler().runTask(plugin, () -> failSession(player, session, throwable.getClass().getSimpleName() + ": " + throwable.getMessage()));
                return;
            }
            if (!persisted) {
                Bukkit.getScheduler().runTask(plugin, () -> failSession(player, session, "Persistent prestige transaction failed."));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> finishSuccess(player, session, nextPrestige, reward, nextRenown, timestamp, requiredCoins, consumeCurrency, perkData));
        });
    }

    public boolean isProcessing(UUID uuid) {
        PrestigeConfirmationSession session = confirmations.get(uuid);
        return session != null && session.state() == PrestigeTransactionState.PROCESSING;
    }

    public Requirement requirement(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return Requirement.NOT_LOADED;
        StatsManager.PrestigeState state = plugin.getStatsManager().prestigeState(player.getUniqueId());
        if (state.prestige() >= plugin.getStatsManager().getMaxPrestige()) return Requirement.MAXIMUM;
        if (state.level() != 120) return Requirement.LEVEL;
        if (state.coins() < plugin.getStatsManager().getPrestigeCoinsRequired(player.getUniqueId())) return Requirement.COINS;
        if (plugin.getStatsManager().getPrestigeCooldownRemaining(player) > 0L) return Requirement.COOLDOWN;
        if (isProcessing(player.getUniqueId())) return Requirement.PROCESSING;
        return Requirement.READY;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof PrestigeHolder) && !(event.getInventory().getHolder() instanceof ConfirmHolder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getInventory().getHolder() instanceof PrestigeHolder holder) {
            if (event.getRawSlot() == holder.prestigeSlot) {
                if (requirement(player) == Requirement.READY) openConfirmation(player);
                else sendRequirement(player, requirement(player));
            } else if (event.getRawSlot() == holder.renownSlot) {
                plugin.getRenownShopManager().open(player);
            }
        } else if (event.getInventory().getHolder() instanceof ConfirmHolder) {
            if (event.getRawSlot() == 11) {
                clearConfirmation(player.getUniqueId(), confirmations.get(player.getUniqueId()));
                open(player);
            } else if (event.getRawSlot() == 15) confirm(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PrestigeHolder || holder instanceof ConfirmHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearConfirmation(event.getPlayer().getUniqueId(), confirmations.get(event.getPlayer().getUniqueId()));
    }

    private ItemStack prestigeIcon(Player player) {
        StatsManager stats = plugin.getStatsManager();
        int prestige = stats.getPrestige(player.getUniqueId());
        if (prestige >= stats.getMaxPrestige()) {
            return item(Material.DIAMOND, "&dMaximum Prestige", Arrays.asList(
                    "&7You have reached Prestige &dL&7.",
                    "",
                    "&dMaximum prestige reached!"
            ));
        }
        Requirement requirement = requirement(player);
        long required = stats.getPrestigeCoinsRequired(player.getUniqueId());
        long coins = stats.getCoins(player.getUniqueId(), me.outerspace.thepit.model.Period.LIFETIME);
        if (requirement != Requirement.READY) {
            return item(Material.DIAMOND, "&cPrestige Locked", Arrays.asList(
                    "&7Reach level &b120 &7and have",
                    "&6" + format(required) + " Gold&7.",
                    "",
                    "&7Level: " + stats.getLevel(player.getUniqueId()) + "/120",
                    "&7Gold: &6" + format(coins) + "&7/&6" + format(required),
                    "",
                    "&cRequirements not met!"
            ));
        }
        int next = prestige + 1;
        int resetLevel = 1;
        return item(Material.DIAMOND, "&aPrestige Available", Arrays.asList(
                "&7Advance to Prestige " + stats.getNextPrestigeRoman(player.getUniqueId()) + ".",
                "",
                "&7Your level will reset to &b" + resetLevel + "&7.",
                "&7Your temporary perks and upgrades",
                "&7will be reset.",
                "",
                "&7Cost: &6" + format(required) + " Gold",
                "&7Balance: &6" + format(coins),
                "",
                "&7You will receive:",
                "&e" + stats.getRenownReward(next) + " Renown",
                "",
                "&eClick to continue!"
        ));
    }

    private ItemStack renownIcon(Player player) {
        Material material;
        try {
            material = Material.valueOf(plugin.registry().settings().getString("prestige.menu.renown-item.material", "DIAMOND").toUpperCase(Locale.ENGLISH));
        } catch (Exception ignored) {
            material = Material.DIAMOND;
        }
        List<String> configured = plugin.registry().settings().getStringList("prestige.menu.renown-item.lore");
        List<String> lore = configured.isEmpty() ? Arrays.asList(
                "&7Spend Renown on permanent",
                "&7upgrades and unlocks.",
                "",
                "&7Your Renown: &e%renown%",
                "",
                "&eClick to open!"
        ) : configured;
        List<String> resolved = new ArrayList<>();
        for (String line : lore) resolved.add(line.replace("%renown%", format(plugin.getStatsManager().getRenown(player.getUniqueId()))));
        return item(material, plugin.registry().settings().getString("prestige.menu.renown-item.name", "&e&lRenown Shop"), resolved);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private Requirement requirementForProcessing(Player player, PrestigeConfirmationSession session) {
        if (player == null || !player.isOnline() || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return Requirement.NOT_LOADED;
        if (confirmations.get(player.getUniqueId()) != session || session.state() != PrestigeTransactionState.PROCESSING) return Requirement.PROCESSING;
        StatsManager.PrestigeState state = plugin.getStatsManager().prestigeState(player.getUniqueId());
        if (state.prestige() >= plugin.getStatsManager().getMaxPrestige()) return Requirement.MAXIMUM;
        if (state.level() != 120) return Requirement.LEVEL;
        if (state.coins() < plugin.getStatsManager().getPrestigeCoinsRequired(player.getUniqueId())) return Requirement.COINS;
        if (plugin.getStatsManager().getPrestigeCooldownRemaining(player) > 0L) return Requirement.COOLDOWN;
        return Requirement.READY;
    }

    private void finishSuccess(Player player, PrestigeConfirmationSession session, int prestige, int reward, long renown, long timestamp, long requiredCoins, boolean consumeCurrency, String perkData) {
        UUID uuid = player.getUniqueId();
        if (confirmations.get(uuid) != session || session.state() != PrestigeTransactionState.PROCESSING) return;
        try {
            plugin.getStatsManager().applyPrestigeCommit(uuid, prestige, renown, timestamp, requiredCoins, consumeCurrency);
            plugin.getPerkManager().applyPersistedProfile(uuid, perkData);
            plugin.getStatsManager().persistFallbackProfile(uuid);
            if (plugin.registry().tablist().debugEnabled()) {
                StatsManager.PrestigeState committed = plugin.getStatsManager().prestigeState(uuid);
                plugin.getLogger().info("Prestige committed player=" + player.getName() + " prestige=" + committed.prestige()
                        + " level=" + committed.level() + " current_xp=" + committed.currentXp()
                        + " lifetime_xp=" + committed.lifetimeXp());
            }
            session.complete();
            clearConfirmation(uuid, session);
            if (player.isOnline()) {
                player.closeInventory();
                if (plugin.registry().settings().getBoolean("prestige.reset.combat-inventory", true)) {
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(null);
                }
                plugin.getBuildScoreboardManager().update(player);
                plugin.getTablistManager().markRefresh(player, "PRESTIGE_CHANGED");
                String roman = plugin.getStatsManager().getPrestigeRoman(uuid);
                player.sendMessage(CC.color("&e&lPRESTIGE"));
                player.sendMessage(message("success", "&aYou advanced to Prestige %prestige_roman%!").replace("%prestige_roman%", roman));
                player.sendMessage(CC.color("&e+" + reward + " Renown"));
                player.sendMessage(CC.color("&7Your level has been reset to &b" + plugin.getStatsManager().getLevel(uuid) + "&7."));
                String broadcast = plugin.registry().settings().getString("prestige.messages.broadcast", "&b&lPRESTIGE! &r%abelix_prefix%%player% &7advanced to Prestige &e%prestige_roman%&7!");
                broadcast = plugin.replaceNetworkPlaceholders(player, null, broadcast).replace("%prestige_roman%", roman);
                plugin.getPitCommandManager().broadcast("prestige-announcements", CC.color(broadcast));
            }
        } catch (Throwable throwable) {
            failSession(player, session, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void failSession(Player player, PrestigeConfirmationSession session, String reason) {
        plugin.getLogger().warning("Could not prestige " + player.getUniqueId() + ": " + reason);
        session.fail();
        clearConfirmation(player.getUniqueId(), session);
        plugin.getStatsManager().persistFallbackProfile(player.getUniqueId());
        if (player.isOnline()) player.sendMessage(message("failed", "&cPrestiging failed. Nothing was changed."));
    }

    private void expireConfirmation(UUID playerId, UUID sessionId) {
        PrestigeConfirmationSession session = confirmations.get(playerId);
        if (session == null || !session.sessionId.equals(sessionId) || session.state() != PrestigeTransactionState.AWAITING_CONFIRMATION || !session.isExpired()) return;
        clearConfirmation(playerId, session);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) player.sendMessage(CC.color("&cYour prestige confirmation expired."));
    }

    private void clearConfirmation(UUID playerId, PrestigeConfirmationSession session) {
        if (session == null) return;
        session.cancelExpiration();
        confirmations.remove(playerId, session);
    }

    public void shutdown() {
        for (PrestigeConfirmationSession session : confirmations.values()) session.cancelExpiration();
        confirmations.clear();
    }

    private void sendRequirement(Player player, Requirement requirement) {
        StatsManager stats = plugin.getStatsManager();
        if (requirement == Requirement.LEVEL || requirement == Requirement.NOT_LOADED) player.sendMessage(message("need-level", "&cYou must reach level 120 before you can prestige."));
        else if (requirement == Requirement.COINS) {
            long remaining = stats.getPrestigeCoinsRemaining(player.getUniqueId());
            player.sendMessage(message("need-coins", "&cYou need another &6%remaining_coins% Gold &cto prestige.").replace("%remaining_coins%", format(remaining)));
        } else if (requirement == Requirement.COOLDOWN) player.sendMessage(message("cooldown", "&cYou must wait %cooldown% before prestiging again.").replace("%cooldown%", stats.getPrestigeCooldownFormatted(player)));
        else if (requirement == Requirement.MAXIMUM) player.sendMessage(message("maximum", "&cYou have reached the maximum prestige."));
        else if (requirement == Requirement.PROCESSING) player.sendMessage(message("processing", "&eYour prestige is already being processed."));
    }

    private String message(String path, String fallback) {
        return CC.color(plugin.registry().settings().getString("prestige.messages." + path, fallback));
    }

    private long safeAdd(long value, long amount) {
        return amount > 0L && value > Long.MAX_VALUE - amount ? Long.MAX_VALUE : value + amount;
    }

    private int menuSize() {
        int configured = plugin.registry().settings().getInt("prestige.menu.size", 27);
        int bounded = Math.max(9, Math.min(54, configured));
        return bounded - bounded % 9;
    }

    private int boundedSlot(int configured, int size) {
        return Math.max(0, Math.min(size - 1, configured));
    }

    private int adjacentEmptySlot(Inventory inventory, int prestigeSlot) {
        int columns = 9;
        if (prestigeSlot % columns < columns - 1 && inventory.getItem(prestigeSlot + 1) == null) return prestigeSlot + 1;
        if (prestigeSlot % columns > 0 && inventory.getItem(prestigeSlot - 1) == null) return prestigeSlot - 1;
        if (prestigeSlot + columns < inventory.getSize() && inventory.getItem(prestigeSlot + columns) == null) return prestigeSlot + columns;
        if (prestigeSlot - columns >= 0 && inventory.getItem(prestigeSlot - columns) == null) return prestigeSlot - columns;
        return -1;
    }

    public void refreshIfOpen(Player player) {
        if (player == null || !player.isOnline()) return;
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof PrestigeHolder) open(player);
        else if (holder instanceof ConfirmHolder) openConfirmation(player);
    }

    private String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    public enum Requirement { READY, NOT_LOADED, LEVEL, COINS, COOLDOWN, MAXIMUM, PROCESSING }

    public enum PrestigeTransactionState {
        AWAITING_CONFIRMATION,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    private static final class PrestigeConfirmationSession {
        private final UUID sessionId;
        private final int prestige;
        private final int level;
        private final long expiresAt;
        private final AtomicReference<PrestigeTransactionState> state = new AtomicReference<>(PrestigeTransactionState.AWAITING_CONFIRMATION);
        private volatile BukkitTask expirationTask;

        private PrestigeConfirmationSession(UUID sessionId, int prestige, int level, long expiresAt) {
            this.sessionId = sessionId;
            this.prestige = prestige;
            this.level = level;
            this.expiresAt = expiresAt;
        }

        private boolean beginProcessing() {
            return state.compareAndSet(PrestigeTransactionState.AWAITING_CONFIRMATION, PrestigeTransactionState.PROCESSING);
        }

        private PrestigeTransactionState state() {
            return state.get();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }

        private void complete() {
            state.compareAndSet(PrestigeTransactionState.PROCESSING, PrestigeTransactionState.COMPLETED);
        }

        private void fail() {
            PrestigeTransactionState current = state.get();
            if (current != PrestigeTransactionState.COMPLETED) state.set(PrestigeTransactionState.FAILED);
        }

        private void setExpirationTask(BukkitTask expirationTask) {
            this.expirationTask = expirationTask;
        }

        private void cancelExpiration() {
            BukkitTask task = expirationTask;
            expirationTask = null;
            if (task != null) task.cancel();
        }
    }

    private static final class PrestigeHolder implements InventoryHolder {
        private Inventory inventory;
        private int prestigeSlot = -1;
        private int renownSlot = -1;
        public Inventory getInventory() { return inventory; }
    }

    private static final class ConfirmHolder implements InventoryHolder {
        private Inventory inventory;
        public Inventory getInventory() { return inventory; }
    }
}
