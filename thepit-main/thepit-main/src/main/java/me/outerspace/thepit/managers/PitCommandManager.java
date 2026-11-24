package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.registry.RegistryNode;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.ItemStackCodec;
import me.outerspace.thepit.util.SecureItemTag;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class PitCommandManager implements Listener {
    private static final List<Integer> LEFT_SLOTS = Arrays.asList(10, 11, 12, 13, 19, 20, 21, 22);
    private static final List<Integer> RIGHT_SLOTS = Arrays.asList(14, 15, 16, 17, 23, 24, 25, 26);
    private static final Map<String, DyeColor> DYES = dyes();
    private final ThePitPlugin plugin;
    private final PitSupporterService supporterService = new PitSupporterService();
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> transactionLocks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> sharedCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> showCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Request> tradeRequests = new ConcurrentHashMap<>();
    private final Map<UUID, TradeSession> trades = new ConcurrentHashMap<>();
    private final Map<UUID, Offer> offersBySender = new ConcurrentHashMap<>();
    private final Map<UUID, Offer> offersByRecipient = new ConcurrentHashMap<>();
    private final Map<UUID, DropState> dropStates = new ConcurrentHashMap<>();
    private final Map<UUID, SupporterPurchase> supporterPurchases = new ConcurrentHashMap<>();
    private final Map<UUID, String> supporterFailures = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, ItemStack>> pendingStashRewards = new ConcurrentHashMap<>();
    private BukkitTask updater;
    private BukkitTask saver;
    private CarePackageState carePackage;
    private long nextCarePackageAt;
    private long managerTicks;

    public PitCommandManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleTasks();
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
    }

    public void reload() {
        cleanupCarePackage(false);
        scheduleTasks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getTablistManager().markRefresh(player, "SUPPORTER_RELOAD");
        }
    }

    private void scheduleTasks() {
        stopTasks();
        scheduleNextCarePackage();
        updater = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        saver = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirtyAsync, 200L, 200L);
    }

    public void shutdown() {
        cleanupCarePackage(false);
        stopTasks();
        for (TradeSession session : new HashSet<>(trades.values())) cancelTrade(session, "&cThe server closed your trade.");
        for (Offer offer : new HashSet<>(offersBySender.values())) cancelOffer(offer, false);
        flushDirty();
        profiles.clear();
        supporterService.clear();
        dirty.clear();
        loading.clear();
        sharedCooldowns.clear();
        showCooldowns.clear();
        dropStates.clear();
        supporterPurchases.clear();
        if (!pendingStashRewards.isEmpty()) plugin.getLogger().severe("The Pit stopped with " + pendingStashRewards.values().stream().mapToInt(Map::size).sum() + " Stash reward transaction(s) still pending MySQL persistence.");
        pendingStashRewards.clear();
    }

    private void stopTasks() {
        if (updater != null) updater.cancel();
        if (saver != null) saver.cancel();
        updater = null;
        saver = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TradeSession trade = trades.get(player.getUniqueId());
        if (trade != null) cancelTrade(trade, "&cThe trade was cancelled because a player disconnected.");
        Offer sent = offersBySender.get(player.getUniqueId());
        if (sent != null) cancelOffer(sent, false);
        Offer received = offersByRecipient.get(player.getUniqueId());
        if (received != null) cancelOffer(received, false);
        tradeRequests.entrySet().removeIf(entry -> entry.getKey().equals(player.getUniqueId())
                || entry.getValue().sender.equals(player.getUniqueId()));
        dropStates.remove(player.getUniqueId());
        Profile profile = profiles.remove(player.getUniqueId());
        supporterService.unload(player.getUniqueId());
        dirty.remove(player.getUniqueId());
        if (profile != null) persistAsync(profile);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        dropStates.remove(uuid);
        TradeSession session = trades.get(uuid);
        if (session != null) cancelTrade(session, "&cThe trade was cancelled because a player changed worlds.");
    }

    private void load(Player player) {
        UUID uuid = player.getUniqueId();
        if (!loading.add(uuid)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("pit-command-profile", uuid.toString()).orElse("");
            Profile profile = Profile.decode(uuid, stored);
            if (stored.isEmpty()) profile.starVisible = settings().getBoolean("pit-supporter.star.default-visible", true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(uuid);
                if (!player.isOnline()) return;
                profile.permissionEntitled = me.outerspace.thepit.util.PermissionCompat.has(plugin, player, settings().getString(
                        "pit-supporter.entitlement-permission", "thepit.pit.supporter"));
                profiles.put(uuid, profile);
                supporterService.load(uuid, profile.supporterOwned, profile.supporterGrant, profile.permissionEntitled,
                        profile.starVisible, profile.legacySupporterDetected);
                migrateLegacyStash(profile);
                retryPendingStash(uuid);
                resetDaily(profile);
                dirty.add(uuid);
                plugin.getTablistManager().markRefresh(player, "SUPPORTER_PROFILE_READY");
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder && holder.type == MenuType.SUPPORTER) openSupporter(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> applyFancyHat(player), 3L);
            });
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyFancyHat(event.getPlayer()), 5L);
    }

    public boolean handles(String command) {
        return Set.of("spawn", "respawn", "oof", "trade", "tradelimits", "offer", "pitchat", "view", "blue",
                "azul", "pickupstash", "dropconfirm", "show", "events", "squad", "cool", "credit", "pitsupporter", "supporter")
                .contains(command.toLowerCase(Locale.ENGLISH));
    }

    public boolean handle(CommandSender sender, String command, String[] args) {
        String id = command.toLowerCase(Locale.ENGLISH);
        if ((id.equals("pitsupporter") || id.equals("supporter")) && args.length > 0 && args[0].equalsIgnoreCase("debug")) return supporterDebug(sender, args);
        if ((id.equals("pitsupporter") || id.equals("supporter")) && args.length > 0 && Set.of("grant", "revoke", "status", "reload").contains(args[0].toLowerCase(Locale.ENGLISH))) {
            return supporterAdmin(sender, args);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cPlayers only."));
            return true;
        }
        return switch (id) {
            case "spawn", "respawn" -> respawn(player);
            case "oof" -> oof(player);
            case "trade" -> tradeCommand(player, args);
            case "tradelimits" -> tradeLimits(player);
            case "offer" -> offerCommand(player, args);
            case "pitchat" -> openPitChat(player);
            case "view", "blue", "azul" -> viewCommand(player, args);
            case "pickupstash" -> pickupStash(player);
            case "dropconfirm" -> dropConfirm(player, args);
            case "show" -> show(player);
            case "events" -> openEvents(player);
            case "squad" -> unavailableSquads(player);
            case "cool" -> cool(player);
            case "credit" -> credit(player);
            case "pitsupporter", "supporter" -> openSupporter(player);
            default -> true;
        };
    }

    private boolean respawn(Player player) {
        if (!loaded(player)) return true;
        if (settings().getBoolean("pit-commands.respawn.deny-in-combat", true)
                && plugin.getBuildScoreboardManager().isCombatTagged(player)) {
            player.sendMessage(CC.color("&cYou cannot use this command while fighting!"));
            return true;
        }
        if (settings().getBoolean("pit-commands.respawn.deny-during-megastreak", true)
                && plugin.getBuildScoreboardManager().hasMegastreak(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYou cannot use this command during a megastreak!"));
            return true;
        }
        if (!cooldown(player)) return true;
        if (plugin.getArenaManager().getCurrentSpawn() == null) {
            player.sendMessage(CC.color("&cThe active arena spawn is unavailable."));
            return true;
        }
        plugin.getArenaManager().teleportToCurrentSpawn(player);
        sharedCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(CC.color("&aTeleported to spawn."));
        return true;
    }

    private boolean oof(Player player) {
        if (!loaded(player) || player.isDead() || player.getHealth() <= 0.0D) return true;
        if (!cooldown(player)) return true;
        sharedCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.getGameListener().forceSelfDeath(player);
        return true;
    }

    private boolean cooldown(Player player) {
        long seconds = Math.max(1L, settings().getLong("pit-commands.respawn.cooldown-seconds", 10L));
        long remaining = sharedCooldowns.getOrDefault(player.getUniqueId(), 0L) + seconds * 1000L - System.currentTimeMillis();
        if (remaining <= 0L) return true;
        player.sendMessage(CC.color("&cWait " + Math.max(1L, (remaining + 999L) / 1000L) + "s before using this command again."));
        return false;
    }

    private boolean tradeCommand(Player player, String[] args) {
        if (!permanentFeature(player, "trade", settings().getInt("pit-commands.trade.unlock-level", 60))) return true;
        if (args.length < 1) {
            player.sendMessage(CC.color("&c/trade <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (!validTradePair(player, target)) return true;
        Offer incoming = offersBySender.get(target.getUniqueId());
        if (incoming != null && incoming.recipient.equals(player.getUniqueId())) {
            openOffer(player, target, incoming);
            return true;
        }
        Request request = tradeRequests.get(player.getUniqueId());
        if (request != null && request.sender.equals(target.getUniqueId()) && request.expiresAt > System.currentTimeMillis()) {
            tradeRequests.remove(player.getUniqueId());
            openTrade(player, target);
            return true;
        }
        tradeRequests.put(target.getUniqueId(), new Request(player.getUniqueId(), System.currentTimeMillis()
                + Math.max(5L, settings().getLong("pit-commands.trade.request-expiration-seconds", 30L)) * 1000L));
        player.sendMessage(CC.color("&aTrade request sent to &f" + target.getName() + "&a."));
        target.sendMessage(CC.color("&e" + player.getName() + " &7sent you a trade request. Type &e/trade " + player.getName() + " &7to accept."));
        return true;
    }

    private boolean validTradePair(Player player, Player target) {
        if (target == null || !target.isOnline()) {
            player.sendMessage(CC.color("&cThat player is not online."));
            return false;
        }
        if (player.equals(target)) {
            player.sendMessage(CC.color("&cYou cannot trade with yourself."));
            return false;
        }
        if (!loaded(player) || !loaded(target)) {
            player.sendMessage(CC.color("&cBoth player profiles must be loaded."));
            return false;
        }
        if (!permanentFeature(target, "trade", settings().getInt("pit-commands.trade.unlock-level", 60))) {
            player.sendMessage(CC.color("&cThat player has not unlocked trading."));
            return false;
        }
        if (trades.containsKey(player.getUniqueId()) || trades.containsKey(target.getUniqueId())) {
            player.sendMessage(CC.color("&cOne of you is already trading."));
            return false;
        }
        return true;
    }

    private void openTrade(Player first, Player second) {
        TradeHolder holder = new TradeHolder(first.getUniqueId(), second.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, CC.color("&8Pit Trade"));
        holder.inventory = inventory;
        TradeSession session = new TradeSession(first.getUniqueId(), second.getUniqueId(), inventory);
        trades.put(first.getUniqueId(), session);
        trades.put(second.getUniqueId(), session);
        redrawTrade(session);
        first.openInventory(inventory);
        second.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player clickedPlayer) dropStates.remove(clickedPlayer.getUniqueId());
        Inventory top = event.getView().getTopInventory();
        if (isCareInventory(top)) {
            event.setCancelled(true);
            if (carePackage != null && carePackage.unlocked && event.getWhoClicked() instanceof Player player
                    && event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
                claimCarePackageReward(player, event.getRawSlot());
            }
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (holder instanceof TradeHolder tradeHolder) {
            tradeClick(event, tradeHolder);
            return;
        }
        if (!(holder instanceof MenuHolder menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !menu.owner.equals(player.getUniqueId())
                || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        if (menu.type == MenuType.CHAT) toggleChat(player, menu.actions.get(event.getRawSlot()));
        else if (menu.type == MenuType.SUPPORTER) supporterClick(player, menu.actions.get(event.getRawSlot()));
        else if (menu.type == MenuType.SUPPORTER_CONFIRM && event.getRawSlot() == 11) confirmSupporterPurchase(player);
        else if (menu.type == MenuType.SUPPORTER_CONFIRM && event.getRawSlot() == 15) player.closeInventory();
        else if (menu.type == MenuType.DYES) dyeClick(player, menu.actions.get(event.getRawSlot()));
        else if (menu.type == MenuType.OFFER && event.getRawSlot() == 11 && menu.offer != null) acceptOffer(player, menu.offer);
        else if (menu.type == MenuType.OFFER && event.getRawSlot() == 15 && menu.offer != null) {
            cancelOffer(menu.offer, false);
            player.closeInventory();
        }
        else if (menu.type == MenuType.SHOW_CONFIRM && event.getRawSlot() == 11) confirmShowBypass(player);
        else if (menu.type == MenuType.SHOW_CONFIRM && event.getRawSlot() == 15) player.closeInventory();
        else if (event.getRawSlot() == 22 || event.getRawSlot() == 49) player.closeInventory();
    }

    private void tradeClick(InventoryClickEvent event, TradeHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradeSession session = trades.get(player.getUniqueId());
        if (session == null || session.inventory != event.getView().getTopInventory()) return;
        int raw = event.getRawSlot();
        if (raw >= session.inventory.getSize()) {
            if (!event.isShiftClick() && !event.getClick().name().contains("NUMBER")
                    && !event.getClick().name().contains("DOUBLE")) event.setCancelled(false);
            return;
        }
        boolean first = session.first.equals(player.getUniqueId());
        List<Integer> own = tradeSlots(first);
        if (own.contains(raw)) {
            if (event.isShiftClick() || event.getClick().name().contains("NUMBER") || event.getClick().name().contains("DOUBLE")) return;
            ItemStack cursor = player.getItemOnCursor();
            ItemStack current = session.inventory.getItem(raw);
            if (cursor != null && cursor.getType() != Material.AIR && !tradeable(player, cursor)) {
                player.sendMessage(CC.color("&cThat item cannot be traded."));
                return;
            }
            session.inventory.setItem(raw, cursor == null || cursor.getType() == Material.AIR ? null : cursor.clone());
            player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
            resetConfirmations(session);
            redrawTradeControls(session);
            return;
        }
        if (raw == (first ? 36 : 44)) {
            long delta = event.isRightClick() ? -100L : 100L;
            if (event.isShiftClick()) delta *= 10L;
            long current = first ? session.firstGold : session.secondGold;
            long updated = Math.max(0L, Math.min(Integer.MAX_VALUE, current + delta));
            if (first) session.firstGold = updated;
            else session.secondGold = updated;
            resetConfirmations(session);
            redrawTradeControls(session);
            return;
        }
        if (raw == (first ? 37 : 43)) {
            if (first) session.firstConfirmed = !session.firstConfirmed;
            else session.secondConfirmed = !session.secondConfirmed;
            redrawTradeControls(session);
            if (session.firstConfirmed && session.secondConfirmed) completeTrade(session);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isCareInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof TradeHolder
                || event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        dropStates.remove(player.getUniqueId());
        TradeSession session = trades.get(player.getUniqueId());
        if (session != null && !session.completing) Bukkit.getScheduler().runTask(plugin, () -> {
            if (trades.get(player.getUniqueId()) == session && player.getOpenInventory().getTopInventory() != session.inventory) {
                cancelTrade(session, "&cThe trade was cancelled.");
            }
        });
    }

    private void redrawTrade(TradeSession session) {
        ItemStack filler = item(Material.STAINED_GLASS_PANE, 15, " ", Collections.emptyList(), false);
        for (int slot = 0; slot < session.inventory.getSize(); slot++) session.inventory.setItem(slot, filler);
        for (int slot : tradeSlots(true)) session.inventory.setItem(slot, null);
        for (int slot : tradeSlots(false)) session.inventory.setItem(slot, null);
        redrawTradeControls(session);
    }

    private void redrawTradeControls(TradeSession session) {
        session.inventory.setItem(36, item(Material.GOLD_INGOT, 0, "&6Your Gold: " + format(session.firstGold),
                Arrays.asList("&7Left-click: &a+100", "&7Right-click: &c-100", "&7Shift: &e1,000"), false));
        session.inventory.setItem(44, item(Material.GOLD_INGOT, 0, "&6Your Gold: " + format(session.secondGold),
                Arrays.asList("&7Left-click: &a+100", "&7Right-click: &c-100", "&7Shift: &e1,000"), false));
        session.inventory.setItem(37, confirmItem(session.firstConfirmed));
        session.inventory.setItem(43, confirmItem(session.secondConfirmed));
        session.inventory.setItem(40, item(Material.PAPER, 0, "&eSecure Pit Trade",
                Arrays.asList("&7Changing items or Gold clears", "&7both confirmations."), false));
    }

    private ItemStack confirmItem(boolean confirmed) {
        return item(confirmed ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 0,
                confirmed ? "&aConfirmed" : "&cClick to Confirm", Collections.emptyList(), false);
    }

    private void resetConfirmations(TradeSession session) {
        session.firstConfirmed = false;
        session.secondConfirmed = false;
    }

    private void completeTrade(TradeSession session) {
        if (session.completing || !transactionLocks.add(session.first)) return;
        if (!transactionLocks.add(session.second)) {
            transactionLocks.remove(session.first);
            return;
        }
        session.completing = true;
        try {
            Player first = Bukkit.getPlayer(session.first);
            Player second = Bukkit.getPlayer(session.second);
            if (first == null || second == null || !loaded(first) || !loaded(second)) {
                cancelTrade(session, "&cThe trade could not be completed.");
                return;
            }
            Profile firstProfile = profiles.get(session.first);
            Profile secondProfile = profiles.get(session.second);
            resetDaily(firstProfile);
            resetDaily(secondProfile);
            if (!withinTradeLimits(firstProfile, session.firstGold) || !withinTradeLimits(secondProfile, session.secondGold)
                    || !plugin.getPitGoldService().has(session.first, session.firstGold)
                    || !plugin.getPitGoldService().has(session.second, session.secondGold)) {
                first.sendMessage(CC.color("&cThe trade exceeds a balance or daily limit."));
                second.sendMessage(CC.color("&cThe trade exceeds a balance or daily limit."));
                session.completing = false;
                resetConfirmations(session);
                redrawTradeControls(session);
                return;
            }
            List<ItemStack> left = items(session.inventory, tradeSlots(true));
            List<ItemStack> right = items(session.inventory, tradeSlots(false));
            if (left.stream().anyMatch(value -> !tradeable(first, value))
                    || right.stream().anyMatch(value -> !tradeable(second, value))) {
                cancelTrade(session, "&cThe trade contained a protected item.");
                return;
            }
            if (session.firstGold > 0 && !plugin.getPitGoldService().take(first, (int) session.firstGold)) {
                cancelTrade(session, "&cThe trade balance transaction failed.");
                return;
            }
            if (session.secondGold > 0 && !plugin.getPitGoldService().take(second, (int) session.secondGold)) {
                plugin.getPitGoldService().add(first, (int) session.firstGold);
                cancelTrade(session, "&cThe trade balance transaction failed.");
                return;
            }
            plugin.getPitGoldService().add(second, (int) session.firstGold);
            plugin.getPitGoldService().add(first, (int) session.secondGold);
            clear(session.inventory, tradeSlots(true));
            clear(session.inventory, tradeSlots(false));
            deliverOrStash(second, left);
            deliverOrStash(first, right);
            firstProfile.tradesToday++;
            secondProfile.tradesToday++;
            firstProfile.goldToday += session.firstGold;
            secondProfile.goldToday += session.secondGold;
            markDirty(firstProfile);
            markDirty(secondProfile);
            persistAsync(firstProfile);
            persistAsync(secondProfile);
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(first.getUniqueId(),
                    AchievementManager.EventType.TRADE_COMPLETED, 1L, Collections.emptyMap(), false));
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(second.getUniqueId(),
                    AchievementManager.EventType.TRADE_COMPLETED, 1L, Collections.emptyMap(), false));
            finishTrade(session);
            first.sendMessage(CC.color("&aTrade completed securely."));
            second.sendMessage(CC.color("&aTrade completed securely."));
        } finally {
            transactionLocks.remove(session.first);
            transactionLocks.remove(session.second);
        }
    }

    private boolean withinTradeLimits(Profile profile, long gold) {
        int tradeLimit = settings().getInt("pit-commands.trade.daily-trade-limit", 25);
        long goldLimit = settings().getLong("pit-commands.trade.daily-gold-limit", 50000L);
        return profile.tradesToday < tradeLimit && gold >= 0L && profile.goldToday <= goldLimit - gold;
    }

    private void finishTrade(TradeSession session) {
        trades.remove(session.first);
        trades.remove(session.second);
        Player first = Bukkit.getPlayer(session.first);
        Player second = Bukkit.getPlayer(session.second);
        if (first != null) first.closeInventory();
        if (second != null) second.closeInventory();
    }

    private void cancelTrade(TradeSession session, String message) {
        if (session == null || session.cancelled) return;
        session.cancelled = true;
        session.completing = true;
        Player first = Bukkit.getPlayer(session.first);
        Player second = Bukkit.getPlayer(session.second);
        List<ItemStack> left = items(session.inventory, tradeSlots(true));
        List<ItemStack> right = items(session.inventory, tradeSlots(false));
        clear(session.inventory, tradeSlots(true));
        clear(session.inventory, tradeSlots(false));
        deliverOrStash(session.first, left);
        deliverOrStash(session.second, right);
        trades.remove(session.first);
        trades.remove(session.second);
        if (first != null) {
            first.closeInventory();
            first.sendMessage(CC.color(message));
        }
        if (second != null) {
            second.closeInventory();
            second.sendMessage(CC.color(message));
        }
    }

    private boolean tradeLimits(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return true;
        resetDaily(profile);
        player.sendMessage(CC.color("&eDaily Trade Limits"));
        player.sendMessage(CC.color("&7Trades: &e" + profile.tradesToday + "&7/&e"
                + settings().getInt("pit-commands.trade.daily-trade-limit", 25)));
        player.sendMessage(CC.color("&7Gold transferred: &6" + format(profile.goldToday) + "&7/&6"
                + format(settings().getLong("pit-commands.trade.daily-gold-limit", 50000L))));
        return true;
    }

    private boolean offerCommand(Player player, String[] args) {
        if (!permanentFeature(player, "trade", settings().getInt("pit-commands.trade.unlock-level", 60))) return true;
        if (args.length < 2) {
            player.sendMessage(CC.color("&c/offer <player> <gold>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        long price;
        try {
            price = Long.parseLong(args[1]);
        } catch (NumberFormatException exception) {
            price = -1L;
        }
        if (!validTradePair(player, target) || price <= 0L || price > Integer.MAX_VALUE) {
            if (price <= 0L || price > Integer.MAX_VALUE) player.sendMessage(CC.color("&cEnter a valid positive Gold price."));
            return true;
        }
        if (!plugin.getPitGoldService().has(target.getUniqueId(), price)) {
            player.sendMessage(CC.color("&cThat player cannot afford this offer."));
            return true;
        }
        if (offersBySender.containsKey(player.getUniqueId()) || offersByRecipient.containsKey(target.getUniqueId())) {
            player.sendMessage(CC.color("&cOne of you already has an active offer."));
            return true;
        }
        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == Material.AIR || !tradeable(player, held)) {
            player.sendMessage(CC.color("&cHold a tradeable item first."));
            return true;
        }
        ItemStack reserved = held.clone();
        player.setItemInHand(null);
        Offer offer = new Offer(player.getUniqueId(), target.getUniqueId(), reserved, price,
                System.currentTimeMillis() + Math.max(5L, settings().getLong("pit-commands.offer.expiration-seconds", 30L)) * 1000L);
        offersBySender.put(player.getUniqueId(), offer);
        offersByRecipient.put(target.getUniqueId(), offer);
        player.sendMessage(CC.color("&aOffer sent to &f" + target.getName() + "&a."));
        target.sendMessage(CC.color("&e" + player.getName() + " &7offers an item for &6" + format(price)
                + " Gold&7. Type &e/trade " + player.getName() + " &7within 30 seconds to inspect and accept."));
        return true;
    }

    private void acceptOffer(Player buyer, Offer offer) {
        Player seller = Bukkit.getPlayer(offer.sender);
        if (seller == null || !loaded(seller) || offer.expiresAt <= System.currentTimeMillis()) {
            cancelOffer(offer, true);
            return;
        }
        if (!transactionLocks.add(offer.sender)) return;
        if (!transactionLocks.add(offer.recipient)) {
            transactionLocks.remove(offer.sender);
            return;
        }
        try {
            Profile sellerProfile = profiles.get(offer.sender);
            Profile buyerProfile = profiles.get(offer.recipient);
            resetDaily(sellerProfile);
            resetDaily(buyerProfile);
            if (!withinTradeLimits(sellerProfile, 0L) || !withinTradeLimits(buyerProfile, offer.price)
                    || !plugin.getPitGoldService().take(buyer, (int) offer.price)) {
                buyer.sendMessage(CC.color("&cThe offer exceeds your balance or daily limits."));
                return;
            }
            plugin.getPitGoldService().add(seller, (int) offer.price);
            deliverOrStash(buyer, Collections.singletonList(offer.item));
            sellerProfile.tradesToday++;
            buyerProfile.tradesToday++;
            buyerProfile.goldToday += offer.price;
            markDirty(sellerProfile);
            markDirty(buyerProfile);
            persistAsync(sellerProfile);
            persistAsync(buyerProfile);
            offersBySender.remove(offer.sender);
            offersByRecipient.remove(offer.recipient);
            buyer.closeInventory();
            seller.sendMessage(CC.color("&aYour item offer was accepted for &6" + format(offer.price) + " Gold&a."));
            buyer.sendMessage(CC.color("&aOffer accepted securely."));
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(seller.getUniqueId(),
                    AchievementManager.EventType.TRADE_COMPLETED, 1L, Collections.emptyMap(), false));
            plugin.getAchievementManager().publish(new AchievementManager.ProgressEvent(buyer.getUniqueId(),
                    AchievementManager.EventType.TRADE_COMPLETED, 1L, Collections.emptyMap(), false));
        } finally {
            transactionLocks.remove(offer.sender);
            transactionLocks.remove(offer.recipient);
        }
    }

    private void openOffer(Player buyer, Player seller, Offer offer) {
        MenuHolder holder = new MenuHolder(buyer.getUniqueId(), MenuType.OFFER);
        holder.offer = offer;
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Item Offer"));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(13, offer.item.clone());
        put(inventory, 11, item(Material.EMERALD_BLOCK, 0, "&aAccept Offer",
                Arrays.asList("&7Seller: &f" + seller.getName(), "&7Price: &6" + format(offer.price) + " Gold",
                        "", "&eClick to accept!"), false));
        put(inventory, 15, item(Material.REDSTONE_BLOCK, 0, "&cDecline Offer",
                Arrays.asList("&7The reserved item will be", "&7returned safely to the seller."), false));
        buyer.openInventory(inventory);
    }

    private void cancelOffer(Offer offer, boolean expired) {
        if (offer == null || offersBySender.remove(offer.sender) == null) return;
        offersByRecipient.remove(offer.recipient);
        deliverOrStash(offer.sender, Collections.singletonList(offer.item));
        Player sender = Bukkit.getPlayer(offer.sender);
        if (sender != null) sender.sendMessage(CC.color(expired ? "&cYour item offer expired." : "&cYour item offer was cancelled."));
    }

    private boolean openPitChat(Player player) {
        if (plugin.getStatsManager().getLevel(player.getUniqueId()) < settings().getInt("pit-commands.pitchat.unlock-level", 5)) {
            player.sendMessage(CC.color("&cRequires Level 5."));
            return true;
        }
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return true;
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.CHAT);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color(settings().getString("pit-commands.pitchat.title", "&8Pit Chat")));
        holder.inventory = inventory;
        fill(inventory);
        RegistryNode section = settings().getConfigurationSection("pit-commands.pitchat.categories");
        if (section != null) for (String category : section.getKeys(false)) {
            int slot = section.getInt(category + ".slot", 10);
            Material material = material(section.getString(category + ".material", "PAPER"), Material.PAPER);
            boolean enabled = !profile.disabledChat.contains(category.toLowerCase(Locale.ENGLISH));
            holder.actions.put(slot, category);
            put(inventory, slot, item(material, material == Material.SKULL_ITEM ? 3 : 0,
                    (enabled ? "&a" : "&c") + section.getString(category + ".display", category),
                    Arrays.asList("&7Status: " + (enabled ? "&aEnabled" : "&cDisabled"), "", "&eClick to toggle!"), false));
        }
        put(inventory, 22, item(Material.BARRIER, 0, "&cClose", Collections.emptyList(), false));
        player.openInventory(inventory);
        return true;
    }

    private void toggleChat(Player player, String category) {
        if (category == null) {
            player.closeInventory();
            return;
        }
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        String normalized = category.toLowerCase(Locale.ENGLISH);
        if (!profile.disabledChat.add(normalized)) profile.disabledChat.remove(normalized);
        markDirty(profile);
        openPitChat(player);
    }

    public boolean allowsChat(UUID uuid, String category) {
        Profile profile = profiles.get(uuid);
        return profile == null || !profile.disabledChat.contains(category.toLowerCase(Locale.ENGLISH));
    }

    public void broadcast(String category, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (allowsChat(player.getUniqueId(), category)) player.sendMessage(message);
        }
    }

    public String carePackageSet(Player player, String arenaId, String locationId) {
        if (player == null || !me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.admin.carepackage")) return CC.color("&cNo permission.");
        String arena = PitYamlManager.normalizeArenaId(arenaId);
        String id = PitYamlManager.normalizeStableId(locationId);
        ArenaFeatureConfiguration.CarePackages configured = plugin.getYamlManager().carePackages(arena);
        me.outerspace.thepit.registry.definition.ArenaDefinition definition = plugin.getArenaManager().getArena(arena).orElse(null);
        if (definition == null || id.isEmpty()) return CC.color("&cUnknown arena or invalid location ID.");
        if (!definition.worldName().equalsIgnoreCase(player.getWorld().getName())) {
            return CC.color("&cStand inside that arena's world before saving this location.");
        }
        if (!safeLanding(player.getLocation().getBlock().getLocation(), arena)) {
            return CC.color("&cYour current block position is not a safe Care Package landing location.");
        }
        double height = settings().getDouble("care-package.falling.drop-height", 25.0D);
        boolean saved = plugin.getYamlManager().saveCarePackage(arena, id, player.getLocation(), height);
        if (!saved) return CC.color("&cThe Care Package location could not be saved.");
        return CC.color("&aSaved Care Package location &e" + id + " &afor &e" + arena + "&a.");
    }

    public String carePackageRemove(Player player, String arenaId, String locationId) {
        if (player == null || !me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.admin.carepackage")) return CC.color("&cNo permission.");
        boolean removed = plugin.getYamlManager().removeCarePackage(arenaId, locationId);
        return CC.color(removed ? "&aRemoved Care Package location &e" + locationId + "&a."
                : "&cThat Care Package location does not exist.");
    }

    public List<String> carePackageList(Player player, String arenaId) {
        if (player == null || !me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.admin.carepackage")) return Collections.singletonList(CC.color("&cNo permission."));
        String arena = PitYamlManager.normalizeArenaId(arenaId);
        ArenaFeatureConfiguration.CarePackages configured = plugin.getYamlManager().carePackages(arena);
        List<String> lines = new ArrayList<>();
        lines.add(CC.color("&eCare Package locations for &f" + arena + "&e:"));
        if (configured.locations().isEmpty()) lines.add(CC.color("&7None configured."));
        for (ArenaFeatureConfiguration.CarePackageLocation value : configured.locations().values()) {
            me.outerspace.thepit.registry.definition.StoredLocation location = value.location();
            lines.add(CC.color("&e" + value.id() + " &7- &f" + location.worldName() + " "
                    + location.x() + ", " + location.y() + ", " + location.z()));
        }
        return lines;
    }

    public String carePackageTest(Player player, String arenaId, String locationId) {
        if (player == null || !me.outerspace.thepit.util.PermissionCompat.has(plugin, player, "thepit.admin.carepackage")) return CC.color("&cNo permission.");
        return startCarePackage(arenaId, locationId, true);
    }

    public String eventCommand(CommandSender sender, String action, String eventId) {
        if (!me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.event")) return CC.color("&cNo permission.");
        String normalized = normalize(eventId).replace('-', '_');
        if ("carepackage".equals(normalized) || "care_package".equals(normalized)) {
            if ("start".equalsIgnoreCase(action)) return startCarePackage(plugin.getArenaManager().getCurrentArena(), null, false);
            if ("stop".equalsIgnoreCase(action)) {
                if (carePackage == null) return CC.color("&cNo Care Package is active.");
                cleanupCarePackage(false);
                return CC.color("&aStopped the Care Package event.");
            }
        }
        if ("start".equalsIgnoreCase(action)) return plugin.getMinorEventManager().startEvent(normalized);
        if ("stop".equalsIgnoreCase(action)) {
            return plugin.getMinorEventManager().stopEvent(normalized);
        }
        return CC.color("&c/event <list|status|start <event-id>|stop [event-id]>");
    }

    public String carePackageStatus() {
        CarePackageState state = carePackage;
        if (state == null) return plugin.getMinorEventManager() == null ? "" : plugin.getMinorEventManager().statusLine();
        if (state.unlocked) return "&aCare Package Open";
        if (state.landed) return "&eCare Package: &f" + state.clicks + "&7/&f" + requiredClicks();
        return "&eCare Package";
    }

    public Set<String> carePackageLocationIds(String arenaId) {
        return plugin.getYamlManager().carePackages(arenaId).locations().keySet();
    }

    private boolean viewCommand(Player viewer, String[] args) {
        if (args.length < 1) {
            viewer.sendMessage(CC.color("&c/view <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        openView(viewer, target);
        return true;
    }

    public void openView(Player viewer, Player target) {
        int level = settings().getInt("pit-commands.view.unlock-level", 70);
        if (!permanentFeature(viewer, "view", level)) return;
        if (target == null || !target.isOnline() || !loaded(target)
                || me.outerspace.thepit.util.PermissionCompat.has(plugin, target, "thepit.view.hidden") && !me.outerspace.thepit.util.PermissionCompat.has(plugin, viewer, "thepit.view.hidden")) {
            viewer.sendMessage(CC.color("&cThat player cannot be viewed."));
            return;
        }
        MenuHolder holder = new MenuHolder(viewer.getUniqueId(), MenuType.VIEW);
        Inventory inventory = Bukkit.createInventory(holder, 54, CC.color("&8" + target.getName()));
        holder.inventory = inventory;
        fill(inventory);
        UUID uuid = target.getUniqueId();
        List<String> summary = Arrays.asList(
                "&7Level: " + plugin.getStatsManager().getFormattedLevel(uuid),
                "&7Prestige: &e" + plugin.getStatsManager().getPrestigeFormatted(uuid),
                "&7Gold: &6" + format(plugin.getPitGoldService().balance(uuid)),
                "&7Hours: &b" + String.format(Locale.US, "%.2f",
                        plugin.getStatsManager().getLongStat(uuid, StatsManager.PLAYTIME_MILLIS) / 3600000.0D),
                "&7Renown: &d" + format(plugin.getStatsManager().getRenown(uuid)),
                "&7Bounty: &6" + format(plugin.getStatsManager().getBounty(uuid)) + "g",
                "&7Kills: &a" + format(plugin.getStatsManager().getKills(uuid, Period.LIFETIME)),
                "&7Deaths: &c" + format(plugin.getStatsManager().getDeaths(uuid, Period.LIFETIME)));
        put(inventory, 4, item(Material.SKULL_ITEM, 3, "&b" + target.getName(), summary, false));
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int index = 0; index < armor.length; index++) if (armor[index] != null) inventory.setItem(10 + index, armor[index].clone());
        ItemStack[] contents = target.getInventory().getContents();
        for (int index = 0; index < Math.min(9, contents.length); index++) if (contents[index] != null) inventory.setItem(19 + index, contents[index].clone());
        ItemStack[] ender = target.getEnderChest().getContents();
        for (int index = 0; index < Math.min(9, ender.length); index++) if (ender[index] != null) inventory.setItem(28 + index, ender[index].clone());
        List<String> perks = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++) {
            String perk = plugin.getPerkManager().profile(uuid).equipped().get(slot);
            if (perk != null) perks.add("&7Slot " + slot + ": &a" + perk);
        }
        List<String> passives = new ArrayList<>();
        for (String passive : Arrays.asList("xp-boost", "gold-boost", "melee-damage", "bow-damage", "damage-reduction", "build-battler", "el-gato")) {
            passives.add("&7" + passive + ": &e" + plugin.getPassiveManager().tier(uuid, passive));
        }
        put(inventory, 39, item(Material.ENCHANTED_BOOK, 0, "&aSelected Perks", perks, false));
        put(inventory, 40, item(Material.EXP_BOTTLE, 0, "&bPassive Tiers", passives, false));
        put(inventory, 41, item(Material.PAPER, 0, "&eStatistics", Arrays.asList(
                "&7Highest streak: &a" + plugin.getStatsManager().getHighestStreak(uuid),
                "&7Damage dealt: &c" + String.format(Locale.US, "%.2f", plugin.getStatsManager().getDoubleStat(uuid, StatsManager.DAMAGE_DEALT)),
                "&7Damage taken: &9" + String.format(Locale.US, "%.2f", plugin.getStatsManager().getDoubleStat(uuid, StatsManager.DAMAGE_TAKEN))), false));
        put(inventory, 49, item(Material.BARRIER, 0, "&cClose", Collections.emptyList(), false));
        viewer.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerView(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target) || !event.getPlayer().isSneaking()) return;
        event.setCancelled(true);
        openView(event.getPlayer(), target);
    }

    private boolean pickupStash(Player player) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || !transactionLocks.add(player.getUniqueId())) return true;
        if (!plugin.getStorageManager().isMysql()) {
            pickupLegacyStash(player, profile);
            transactionLocks.remove(player.getUniqueId());
            return true;
        }
        UUID claimId = UUID.randomUUID();
        int limit = Math.max(1, settings().getInt("pit-commands.stash.claim-batch-size", 54));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StorageManager.StashEntry> claimed = plugin.getStorageManager().claimStash(player.getUniqueId(), claimId, limit);
            int totalBefore = plugin.getStorageManager().stashCount(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> applyStashClaim(player.getUniqueId(), claimId, claimed, totalBefore));
        });
        return true;
    }

    public void addToStash(UUID uuid, ItemStack item) {
        addToStash(uuid, item, UUID.randomUUID());
    }

    public void addToStash(UUID uuid, ItemStack item, UUID transactionId) {
        if (uuid == null || transactionId == null || !stashEligible(item)) return;
        ItemStack stored = item.clone();
        pendingStashRewards.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>()).putIfAbsent(transactionId, stored.clone());
        String encoded = ItemStackCodec.encode(new ItemStack[]{stored});
        int capacity = Math.max(1, settings().getInt("pit-commands.stash.capacity", 108));
        if (!plugin.getStorageManager().isMysql()) {
            storeLegacyStash(uuid, stored, capacity, transactionId);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean saved = plugin.getStorageManager().enqueueStash(uuid, transactionId, encoded, capacity);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!saved) {
                    storeLegacyStash(uuid, stored, capacity, transactionId);
                    return;
                }
                removePendingStash(uuid, transactionId);
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.sendMessage(CC.color("&eYou have items in your stash! Type &b/pickupstash &eto collect them."));
            });
        });
    }

    private void applyStashClaim(UUID uuid, UUID claimId, List<StorageManager.StashEntry> claimed, int totalBefore) {
        Player player = Bukkit.getPlayer(uuid);
        Map<Long, String> remaining = new LinkedHashMap<>();
        int delivered = 0;
        if (player != null && player.isOnline()) {
            for (StorageManager.StashEntry entry : claimed) {
                ItemStack[] decoded = ItemStackCodec.decode(entry.itemData());
                if (decoded.length == 0 || Arrays.stream(decoded).anyMatch(value -> !stashEligible(value))) {
                    remaining.put(entry.sequenceId(), entry.itemData());
                    continue;
                }
                List<ItemStack> leftovers = new ArrayList<>();
                for (ItemStack decodedItem : decoded) {
                    ItemStack item = decodedItem.clone();
                    int before = item.getAmount();
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    int left = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
                    delivered += Math.max(0, before - left);
                    leftovers.addAll(overflow.values());
                }
                if (!leftovers.isEmpty()) remaining.put(entry.sequenceId(), ItemStackCodec.encode(leftovers.toArray(new ItemStack[0])));
            }
            player.updateInventory();
        } else for (StorageManager.StashEntry entry : claimed) remaining.put(entry.sequenceId(), entry.itemData());
        int deliveredAmount = delivered;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean finalized = plugin.getStorageManager().finishStashClaim(uuid, claimId, remaining);
            int remainingCount = plugin.getStorageManager().stashCount(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                transactionLocks.remove(uuid);
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline()) return;
                if (!finalized) {
                    online.sendMessage(CC.color("&cYour stash claim could not be finalized. No further claim will be allowed until it is recovered."));
                    return;
                }
                online.sendMessage(CC.color("&aYou picked up &e" + deliveredAmount + " &aitems from your stash."));
                if (remainingCount > 0) online.sendMessage(CC.color("&eYour inventory is full. &f" + remainingCount + " &eitems remain in your stash."));
                else if (totalBefore == 0) online.sendMessage(CC.color("&7Your stash is empty."));
            });
        });
    }

    private void pickupLegacyStash(Player player, Profile profile) {
        int before = profile.stash.stream().mapToInt(ItemStack::getAmount).sum();
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : profile.stash) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            remaining.addAll(overflow.values());
        }
        profile.stash.clear();
        profile.stash.addAll(remaining);
        int after = remaining.stream().mapToInt(ItemStack::getAmount).sum();
        markDirty(profile);
        player.sendMessage(CC.color("&aYou picked up &e" + Math.max(0, before - after) + " &aitems from your stash."));
        if (after > 0) player.sendMessage(CC.color("&eYour inventory is full. &f" + after + " &eitems remain in your stash."));
    }

    private void storeLegacyStash(UUID uuid, ItemStack item, int capacity, UUID transactionId) {
        Profile profile = profiles.get(uuid);
        if (profile == null) {
            plugin.getLogger().severe("Stash retained only in failed transaction log: tx=" + transactionId + " player=" + uuid + " reason=profile-unloaded item=" + item.getType() + " amount=" + item.getAmount());
            return;
        }
        profile.stash.add(item.clone());
        markDirty(profile);
        removePendingStash(uuid, transactionId);
        if (profile.stash.size() > capacity) plugin.getLogger().warning("Stash soft capacity exceeded: tx=" + transactionId + " player=" + uuid + " entries=" + profile.stash.size());
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) online.sendMessage(CC.color("&eYou have items in your stash! Type &b/pickupstash &eto collect them."));
    }

    private void removePendingStash(UUID uuid, UUID transactionId) {
        LinkedHashMap<UUID, ItemStack> pending = pendingStashRewards.get(uuid);
        if (pending == null) return;
        pending.remove(transactionId);
        if (pending.isEmpty()) pendingStashRewards.remove(uuid, pending);
    }

    private void retryPendingStash(UUID uuid) {
        LinkedHashMap<UUID, ItemStack> pending = pendingStashRewards.get(uuid);
        if (pending == null || pending.isEmpty()) return;
        for (Map.Entry<UUID, ItemStack> entry : new ArrayList<>(pending.entrySet())) addToStash(uuid, entry.getValue(), entry.getKey());
    }

    private void migrateLegacyStash(Profile profile) {
        if (profile == null || profile.stash.isEmpty() || !plugin.getStorageManager().isMysql()) return;
        List<String> legacy = new ArrayList<>();
        for (ItemStack item : profile.stash) if (stashEligible(item)) legacy.add(ItemStackCodec.encode(new ItemStack[]{item.clone()}));
        if (legacy.isEmpty()) return;
        int capacity = Math.max(1, settings().getInt("pit-commands.stash.capacity", 108));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean saved = true;
            for (int index = 0; index < legacy.size(); index++) {
                String encoded = legacy.get(index);
                UUID entryId = UUID.nameUUIDFromBytes(("legacy-stash:" + profile.uuid + ":" + index + ":" + encoded.hashCode()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (!plugin.getStorageManager().enqueueStash(profile.uuid, entryId, encoded, capacity)) saved = false;
            }
            if (saved) Bukkit.getScheduler().runTask(plugin, () -> {
                Profile current = profiles.get(profile.uuid);
                if (current != profile) return;
                current.stash.clear();
                markDirty(current);
                persistAsync(current);
            });
        });
    }

    private boolean stashEligible(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;
        String id = SecureItemTag.string(item, "ThePitItem").toLowerCase(Locale.ENGLISH);
        if (id.startsWith("menu_") || id.startsWith("temporary_") || id.startsWith("event_temporary_") || id.equals("pit_gold_token")) return false;
        return SecureItemTag.string(item, "TemporaryEventItem").isEmpty() && SecureItemTag.string(item, "MenuItem").isEmpty();
    }

    private boolean dropConfirm(Player player, String[] args) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null || args.length != 1 || !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off")) {
            player.sendMessage(CC.color("&c/dropconfirm <on|off>"));
            return true;
        }
        profile.dropConfirm = args[0].equalsIgnoreCase("on");
        markDirty(profile);
        player.sendMessage(CC.color(profile.dropConfirm ? "&aDrop confirmation enabled." : "&cDrop confirmation disabled."));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Profile profile = profiles.get(player.getUniqueId());
        ItemStack item = event.getItemDrop().getItemStack();
        if (profile == null || !profile.dropConfirm || !protectedItem(item)) return;
        int required = Math.max(1, settings().getInt("pit-commands.drop-confirm.presses-required", 3));
        long timeout = Math.max(1L, settings().getLong("pit-commands.drop-confirm.timeout-seconds", 5L)) * 1000L;
        String fingerprint = fingerprint(item, player.getInventory().getHeldItemSlot());
        DropState state = dropStates.get(player.getUniqueId());
        int presses = state != null && state.expiresAt > System.currentTimeMillis() && state.fingerprint.equals(fingerprint)
                ? state.presses + 1 : 1;
        if (presses < required) {
            event.setCancelled(true);
            dropStates.put(player.getUniqueId(), new DropState(fingerprint, presses, System.currentTimeMillis() + timeout));
            player.sendMessage(CC.color("&cPress drop " + (required - presses) + " more times to confirm dropping this item!"));
        } else dropStates.remove(player.getUniqueId());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        dropStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDropStateDeath(PlayerDeathEvent event) {
        dropStates.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        dropStates.remove(event.getPlayer().getUniqueId());
        if (isCareInventory(event.getInventory()) && (carePackage == null || !carePackage.unlocked)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !isCareBlock(event.getClickedBlock())) return;
        if (carePackage == null || carePackage.unlocked) return;
        event.setCancelled(true);
        clickCarePackage(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageDamage(BlockDamageEvent event) {
        if (!isCareBlock(event.getBlock())) return;
        event.setCancelled(true);
        clickCarePackage(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageBreak(BlockBreakEvent event) {
        if (isCareBlock(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageMove(InventoryMoveItemEvent event) {
        if (isCareInventory(event.getSource()) || isCareInventory(event.getDestination())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isCareBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isCareBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackagePistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isCareBlock)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackagePistonRetract(BlockPistonRetractEvent event) {
        if (isCareBlock(event.getRetractLocation().getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCarePackageFallingBlock(EntityChangeBlockEvent event) {
        CarePackageState state = carePackage;
        if (state == null || state.falling == null || !state.falling.equals(event.getEntity())) return;
        event.setCancelled(true);
        landCarePackage(state);
    }

    @EventHandler
    public void onCarePackageWorldUnload(WorldUnloadEvent event) {
        if (carePackage != null && carePackage.world == event.getWorld()) cleanupCarePackage(false);
    }

    private boolean protectedItem(ItemStack item) {
        if (item == null) return false;
        String id = SecureItemTag.string(item, "ThePitItem");
        String normalizedId = id.toLowerCase(Locale.ENGLISH).replace('-', '_');
        if (SecureItemTag.getInteger(item, "MysticLives", -1) >= 0 || SecureItemTag.getInteger(item, "mystic_lives", -1) >= 0) return true;
        if (!SecureItemTag.string(item, "MysticItemId").isEmpty() || !SecureItemTag.string(item, "mystic_item_type").isEmpty()
                || !SecureItemTag.string(item, "BuildFFAContractId").isEmpty() || SecureItemTag.getInteger(item, "RareItem", 0) == 1) return true;
        for (String configured : settings().getStringList("pit-commands.drop-confirm.protected-item-ids")) {
            String protectedId = configured.toLowerCase(Locale.ENGLISH).replace('-', '_');
            if (normalizedId.equals(protectedId) || normalizedId.endsWith(protectedId)) return true;
        }
        return !SecureItemTag.string(item, "PitBundleId").isEmpty();
    }

    private boolean show(Player player) {
        if (!supporterService.isLoaded(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYour Pit Supporter status is still loading. Try again in a moment."));
            return true;
        }
        if (!supporterEntitled(player)) {
            player.sendMessage(CC.color("&cYou need Pit Supporter to use /show!"));
            return true;
        }
        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(CC.color("&cYou must hold an item to use /show!"));
            return true;
        }
        String secureId = SecureItemTag.string(held, "ThePitItem").toLowerCase(Locale.ENGLISH);
        if (secureId.startsWith("menu_") || secureId.startsWith("temporary_") || secureId.startsWith("event_temporary_") || secureId.equals("pit_gold_token")) {
            player.sendMessage(CC.color("&cThat item cannot be shown."));
            return true;
        }
        long ready = showCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (ready > System.currentTimeMillis()) {
            openShowConfirmation(player, held);
            return true;
        }
        broadcastShow(player, held);
        showCooldowns.put(player.getUniqueId(), System.currentTimeMillis()
                + Math.max(1L, settings().getLong("pit-commands.show.cooldown-seconds", 120L)) * 1000L);
        return true;
    }

    private void openShowConfirmation(Player player, ItemStack held) {
        int price = Math.max(1, settings().getInt("pit-commands.show.bypass-price", 10000));
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.SHOW_CONFIRM);
        holder.item = held.clone();
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Bypass Show Cooldown?"));
        holder.inventory = inventory;
        fill(inventory);
        put(inventory, 11, item(Material.GOLD_INGOT, 0, "&aPay " + format(price) + " Gold",
                Arrays.asList("&7Bypass the remaining cooldown.", "", "&eClick to confirm!"), false));
        put(inventory, 15, item(Material.BARRIER, 0, "&cCancel", Collections.emptyList(), false));
        player.openInventory(inventory);
    }

    private void confirmShowBypass(Player player) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder)
                || holder.type != MenuType.SHOW_CONFIRM || holder.item == null || !transactionLocks.add(player.getUniqueId())) return;
        try {
            ItemStack current = player.getItemInHand();
            if (current == null || current.getType() == Material.AIR || !current.isSimilar(holder.item)
                    || current.getAmount() != holder.item.getAmount()) {
                player.sendMessage(CC.color("&cYour held item changed. Nothing was charged."));
                return;
            }
            int price = Math.max(1, settings().getInt("pit-commands.show.bypass-price", 10000));
            if (!plugin.getPitGoldService().take(player, price)) {
                player.sendMessage(CC.color("&cYou need &6" + format(price) + " Gold&c."));
                return;
            }
            broadcastShow(player, holder.item);
            showCooldowns.put(player.getUniqueId(), System.currentTimeMillis()
                    + Math.max(1L, settings().getLong("pit-commands.show.cooldown-seconds", 120L)) * 1000L);
            player.closeInventory();
        } finally {
            transactionLocks.remove(player.getUniqueId());
        }
    }

    private void broadcastShow(Player player, ItemStack item) {
        String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : readable(item.getType().name());
        ItemStack snapshot = item.clone();
        String prefix = plugin.getStatsManager().getFormattedLevel(player.getUniqueId()) + " "
                + decoratedName(player, false)
                + CC.color(" &eis showing off ");
        TextComponent message = new TextComponent(prefix);
        TextComponent shown = new TextComponent(display);
        try {
            shown.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                    new BaseComponent[]{new TextComponent(itemNbt(snapshot))}));
        } catch (Throwable ignored) { }
        message.addExtra(shown);
        message.addExtra(CC.color("&e!"));
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (allowsChat(target.getUniqueId(), "miscellaneous")) {
                target.spigot().sendMessage(message);
            }
        }
    }

    private boolean openEvents(Player player) {
        if (!supporter(player)) return true;
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.EVENTS);
        Inventory inventory = Bukkit.createInventory(holder, 54, CC.color("&8Upcoming Events"));
        holder.inventory = inventory;
        fill(inventory);
        if (carePackage != null) {
            put(inventory, 22, item(Material.CHEST, 0, "&eCare Package",
                    Arrays.asList("&7Status: " + carePackageStatus(),
                            "&7Location: &e" + readable(carePackage.locationId),
                            "&7Expires in: &f" + Math.max(0L, (carePackage.expiresAt - System.currentTimeMillis()) / 1000L) + "s"), true));
            player.openInventory(inventory);
            return true;
        }
        List<String> configured = settings().getStringList("pit-commands.events.configured");
        if (configured.isEmpty()) {
            put(inventory, 22, item(Material.WATCH, 0, "&eCare Package",
                    Arrays.asList("&7Minor event", "&7Next check: &e" + countdown(nextCarePackageAt)), false));
        } else {
            int slot = 0;
            for (String entry : configured) {
                if (slot >= Math.min(54, settings().getInt("pit-supporter.events.maximum-displayed", 54))) break;
                String[] parts = entry.split("\\|", 3);
                if (parts.length < 2) continue;
                long starts;
                try { starts = Long.parseLong(parts[1]); } catch (NumberFormatException exception) { continue; }
                put(inventory, slot++, item(Material.WATCH, 0, "&e" + parts[0],
                        Arrays.asList("&7Starts in: &e" + countdown(starts)), false));
            }
        }
        player.openInventory(inventory);
        return true;
    }

    private boolean unavailableSquads(Player player) {
        player.sendMessage(CC.color("&cThere is no active Squads event."));
        return true;
    }

    private boolean cool(Player player) {
        if (!plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "cool-conclave")) {
            player.sendMessage(CC.color("&cRequires the Cool Conclave Renown upgrade."));
            return true;
        }
        List<Player> values = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (loaded(target) && plugin.getStatsManager().getPrestige(target.getUniqueId()) >= 4) values.add(target);
        }
        values.sort(Comparator.comparingLong((Player value) -> plugin.getStatsManager().getLifetimeXp(value.getUniqueId())).reversed());
        player.sendMessage(CC.color("&b&lCOOL CONCLAVE"));
        if (values.isEmpty()) player.sendMessage(CC.color("&7No Prestige IV+ players are online."));
        else for (Player target : values) player.sendMessage(CC.color(plugin.getStatsManager().getFormattedLevel(target.getUniqueId())
                + " &f" + target.getName() + " &7- &b" + format(plugin.getStatsManager().getLifetimeXp(target.getUniqueId())) + " XP"));
        return true;
    }

    private boolean credit(Player player) {
        player.sendMessage(CC.color("&cYou did not help create the current event."));
        return true;
    }

    private boolean openSupporter(Player player) {
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.SUPPORTER);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Pit Supporter"));
        holder.inventory = inventory;
        fill(inventory);
        boolean entitlementLoaded = supporterService.isLoaded(player.getUniqueId());
        boolean entitled = entitlementLoaded && supporterService.ownsPitSupporter(player.getUniqueId());
        Profile profile = profiles.get(player.getUniqueId());
        String source = supporterService.persistentOwnership(player.getUniqueId()) ? "Purchased"
                : profile != null && profile.supporterGrant ? "Persistent grant"
                : profile != null && profile.permissionEntitled
                ? "Network entitlement" : "Not entitled";
        holder.actions.put(10, "star");
        holder.actions.put(12, "events");
        holder.actions.put(14, "dye");
        holder.actions.put(16, "show");
        holder.actions.put(22, "hat");
        put(inventory, 10, item(Material.NETHER_STAR, 0, "&eSupporter Star",
                supporterService.starEnabled(player.getUniqueId())
                        ? Arrays.asList("&eSupporter Star: &aEnabled", "", "&7Your &e✬ &7appears in chat", "&7and the tab list.", "", "&eClick to disable!")
                        : Arrays.asList("&eSupporter Star: &cDisabled", "", "&7Your supporter badge is hidden.", "&7Your supporter benefits remain active.", "", "&eClick to enable!"), false));
        put(inventory, 12, item(Material.WATCH, 0, "&aEvent Viewing", Arrays.asList("&7View configured upcoming events."), false));
        put(inventory, 14, item(Material.LEATHER_LEGGINGS, 0, "&dMystic Pants Dyeing",
                Arrays.asList("&7Dye held ordinary Mystic Pants.", "&7Cost: &6200 Gold"), false));
        put(inventory, 16, item(Material.NAME_TAG, 0, "&eShowoff", Arrays.asList("&7Use &e/show &7to display an item."), false));
        if (!entitlementLoaded) {
            holder.actions.clear();
            put(inventory, 22, item(Material.WATCH, 0, "&eLoading your Pit Supporter status...",
                    Arrays.asList("", "&7Your entitlement is being loaded."), false));
        } else if (entitled) {
            put(inventory, 22, item(Material.LEATHER_HELMET, 0, "&bFancier Hat",
                    Arrays.asList("&aYou own Pit Supporter!", "", "&7Supporter Star: " + (supporterService.starEnabled(player.getUniqueId()) ? "&aEnabled" : "&cDisabled"),
                            "", "&eClick to manage your settings!", "&7Entitlement: &a" + source), profile != null && profile.fancierHat));
        } else {
            holder.actions.put(22, "purchase");
            int price = Math.max(1, settings().getInt("pit-supporter.purchase-price-abelix-gold", 1000));
            put(inventory, 22, item(Material.GOLD_INGOT, 0, "&e&lPIT SUPPORTER",
                    Arrays.asList("", "&7Unlock exclusive quality-of-life", "&7features in The Pit.", "", "&7Cost: &6" + format(price) + " Abelix Gold", "", "&eClick to purchase!"), true));
        }
        player.openInventory(inventory);
        return true;
    }

    private void supporterClick(Player player, String action) {
        if (action == null) return;
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        if (action.equals("purchase")) {
            openSupporterConfirmation(player);
            return;
        }
        if (!supporter(player)) return;
        if (action.equals("star")) {
            profile.starVisible = !supporterService.starEnabled(player.getUniqueId());
            supporterService.setStarEnabled(player.getUniqueId(), profile.starVisible);
            markDirty(profile);
            plugin.getTablistManager().markRefresh(player, "SUPPORTER_STAR_TOGGLED");
            openSupporter(player);
        } else if (action.equals("events")) openEvents(player);
        else if (action.equals("dye")) openDyes(player);
        else if (action.equals("show")) {
            player.closeInventory();
            show(player);
        } else if (action.equals("hat")) toggleFancyHat(player, profile);
    }

    private void openDyes(Player player) {
        if (!ordinaryMysticPants(player, player.getItemInHand())) {
            player.sendMessage(CC.color("&cHold ordinary non-special Mystic Pants first."));
            return;
        }
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.DYES);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Mystic Pants Dyeing"));
        holder.inventory = inventory;
        fill(inventory);
        int slot = 0;
        for (Map.Entry<String, DyeColor> entry : configuredDyes().entrySet()) {
            while (slot == 8 || slot == 17) slot++;
            holder.actions.put(slot, entry.getKey());
            ItemStack icon = new ItemStack(Material.LEATHER_LEGGINGS);
            LeatherArmorMeta meta = (LeatherArmorMeta) icon.getItemMeta();
            meta.setColor(Color.fromRGB(entry.getValue().rgb));
            meta.setDisplayName(CC.color("&b" + entry.getValue().name));
            meta.setLore(Arrays.asList(CC.color("&7Cost: &6" + dyePrice(player) + " Gold"), "", CC.color("&eClick to dye!")));
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);
        }
        player.openInventory(inventory);
    }

    private void dyeClick(Player player, String id) {
        DyeColor color = configuredDyes().get(id);
        if (color == null || !ordinaryMysticPants(player, player.getItemInHand()) || !transactionLocks.add(player.getUniqueId())) return;
        try {
            int price = dyePrice(player);
            if (!plugin.getPitGoldService().take(player, price)) {
                player.sendMessage(CC.color("&cYou need &6" + format(price) + " Gold&c."));
                return;
            }
            ItemStack pants = player.getItemInHand().clone();
            LeatherArmorMeta meta = (LeatherArmorMeta) pants.getItemMeta();
            meta.setColor(Color.fromRGB(color.rgb));
            pants.setItemMeta(meta);
            player.setItemInHand(pants);
            player.closeInventory();
            player.sendMessage(CC.color("&aDyed your Mystic Pants " + color.name + "&a."));
        } finally {
            transactionLocks.remove(player.getUniqueId());
        }
    }

    private int dyePrice(Player player) {
        int base = Math.max(1, settings().getInt("pit-supporter.pants-dye.price", 200));
        int tier = plugin.getContractManager().upgradeTier(player.getUniqueId(), "scam-artist");
        return Math.max(1, (int) Math.floor(base * Math.max(0.0D, 1.0D - 0.05D * tier)));
    }

    private boolean ordinaryMysticPants(Player owner, ItemStack item) {
        if (item == null || item.getType() != Material.LEATHER_LEGGINGS || !(item.getItemMeta() instanceof LeatherArmorMeta)) return false;
        String id = SecureItemTag.string(item, "ThePitItem").toLowerCase(Locale.ENGLISH);
        if (id.contains("aqua") || id.contains("dark") || id.contains("rage") || id.contains("special")) return false;
        String itemOwner = SecureItemTag.string(item, "MysticOwner");
        if (!itemOwner.isEmpty() && !itemOwner.equals(owner.getUniqueId().toString())) return false;
        return SecureItemTag.getInteger(item, "MysticLives", -1) >= 0 || id.equals("fresh_pants") || id.startsWith("mystic_");
    }

    private void toggleFancyHat(Player player, Profile profile) {
        if (!plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "fancy-hat")) {
            player.sendMessage(CC.color("&cYou do not own Fancy Hat."));
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || !SecureItemTag.is(helmet, "fancy_hat")) {
            player.sendMessage(CC.color("&cEquip your securely identified Fancy Hat first."));
            return;
        }
        profile.fancierHat = !profile.fancierHat;
        ItemStack changed = helmet.clone();
        if (profile.fancierHat) {
            changed.addUnsafeEnchantment(Enchantment.LURE, 1);
            ItemMeta meta = changed.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            changed.setItemMeta(meta);
        } else changed.removeEnchantment(Enchantment.LURE);
        player.getInventory().setHelmet(changed);
        markDirty(profile);
        openSupporter(player);
    }

    private void applyFancyHat(Player player) {
        Profile profile = player == null ? null : profiles.get(player.getUniqueId());
        if (profile == null || !profile.fancierHat || !plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "fancy-hat")) return;
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || !SecureItemTag.is(helmet, "fancy_hat") || helmet.containsEnchantment(Enchantment.LURE)) return;
        ItemStack changed = helmet.clone();
        changed.addUnsafeEnchantment(Enchantment.LURE, 1);
        ItemMeta meta = changed.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        changed.setItemMeta(meta);
        player.getInventory().setHelmet(changed);
    }

    public String supporterStar(Player player, boolean tab) {
        Profile profile = player == null ? null : profiles.get(player.getUniqueId());
        if (profile == null || !supporterService.starEnabled(player.getUniqueId())
                || !settings().getBoolean("pit-supporter.star.enabled", true)) return "";
        if (tab && settings().getBoolean("pit-supporter.star.tab-requires-bounty", false)
                && plugin.getStatsManager().getBounty(player.getUniqueId()) <= 0) return "";
        return CC.color(settings().getString("pit-supporter.star.symbol", "&e✬")).trim();
    }

    public String decoratedName(Player player, boolean tab) {
        if (player == null) return "";
        return plugin.getPlayerNameFormatter().decoratedName(player, plugin.getNetworkProfileCache().get(player), tab);
    }

    private boolean supporter(Player player) {
        if (!supporterService.isLoaded(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYour Pit Supporter status is still loading. Try again in a moment."));
            return false;
        }
        if (supporterEntitled(player)) return true;
        player.sendMessage(CC.color("&cThis feature requires Pit Supporter. Use &e/pitsupporter &cto purchase it."));
        return false;
    }

    private boolean supporterEntitled(Player player) {
        return player != null && supporterService.ownsPitSupporter(player.getUniqueId());
    }

    public PitSupporterService supporterService() { return supporterService; }

    private void openSupporterConfirmation(Player player) {
        if (supporterEntitled(player)) {
            player.sendMessage(CC.color("&aYou already own Pit Supporter!"));
            openSupporter(player);
            return;
        }
        int price = Math.max(1, settings().getInt("pit-supporter.purchase-price-abelix-gold", 1000));
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.SUPPORTER_CONFIRM);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Confirm Pit Supporter"));
        holder.inventory = inventory;
        fill(inventory);
        put(inventory, 11, item(Material.EMERALD_BLOCK, 0, "&aConfirm Purchase", Arrays.asList("&7Cost: &6" + format(price) + " Abelix Gold", "", "&eClick to confirm!"), true));
        put(inventory, 15, item(Material.BARRIER, 0, "&cCancel", Collections.emptyList(), false));
        player.openInventory(inventory);
    }

    private void confirmSupporterPurchase(Player player) {
        UUID uuid = player.getUniqueId();
        Profile profile = profiles.get(uuid);
        if (profile == null || !plugin.getStatsManager().isProfileLoaded(uuid) || !supporterService.isLoaded(uuid)) {
            player.sendMessage(CC.color("&cYour Pit Supporter status is still loading. Try again in a moment."));
            return;
        }
        if (supporterEntitled(player)) {
            player.sendMessage(CC.color("&aYou already own Pit Supporter!"));
            openSupporter(player);
            return;
        }
        if (plugin.getAbelixHook() == null || !plugin.getAbelixHook().isEnabled()) {
            supporterFailures.put(uuid, "ABELIX_UNAVAILABLE");
            player.sendMessage(CC.color("&cThe Abelix Gold service is currently unavailable. Nothing was charged."));
            return;
        }
        SupporterPurchase purchase = new SupporterPurchase(UUID.randomUUID(), uuid,
                Math.max(1, settings().getInt("pit-supporter.purchase-price-abelix-gold", 1000)));
        if (supporterPurchases.putIfAbsent(uuid, purchase) != null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeSupporterPurchase(player, profile, purchase));
    }

    private void executeSupporterPurchase(Player player, Profile profile, SupporterPurchase purchase) {
        try {
            if (supporterService.ownsPitSupporter(purchase.player)) {
                completeAlreadyOwned(player, profile, purchase);
                return;
            }
            persistSupporterPurchase(purchase);
            purchase.balance = plugin.getAbelixHook().getGold(player);
            if (purchase.balance == null) throw new IllegalStateException("Abelix balance unavailable");
            if (purchase.balance < purchase.price) {
                purchase.failure = "INSUFFICIENT_ABELIX_GOLD";
                purchase.state = SupporterPurchaseState.FAILED;
                finishSupporterFailure(player, purchase, "&cYou need &6" + format(purchase.price) + " Abelix Gold &cto purchase Pit Supporter!", null);
                return;
            }
            purchase.deducted = plugin.getAbelixHook().takeGold(player, purchase.price);
            if (!purchase.deducted) throw new IllegalStateException("Abelix deduction failed");
            purchase.state = SupporterPurchaseState.PAYMENT_COMMITTED;
            persistSupporterPurchase(purchase);
            profile.supporterOwned = true;
            profile.starVisible = true;
            supporterService.setPurchased(purchase.player, true);
            supporterService.setStarEnabled(purchase.player, true);
            purchase.entitlement = plugin.getStorageManager().putOverride("pit-command-profile", purchase.player.toString(), profile.encode());
            if (!purchase.entitlement) {
                profile.supporterOwned = false;
                supporterService.setPurchased(purchase.player, false);
                purchase.state = SupporterPurchaseState.COMPENSATION_REQUIRED;
                purchase.compensated = plugin.getAbelixHook().addGold(player, purchase.price);
                purchase.failure = purchase.compensated ? "ENTITLEMENT_FAILED_COMPENSATED" : "COMPENSATION_REQUIRED";
                purchase.state = SupporterPurchaseState.FAILED;
                finishSupporterFailure(player, purchase, purchase.compensated ? "&cThe purchase could not be saved. Your Abelix Gold was restored." : "&cThe purchase could not be saved. Staff have been notified.", new IllegalStateException("Pit Supporter entitlement persistence failed"));
                return;
            }
            purchase.state = SupporterPurchaseState.ENTITLEMENT_COMMITTED;
            persistSupporterPurchase(purchase);
            purchase.state = SupporterPurchaseState.COMPLETED;
            persistSupporterPurchase(purchase);
            supporterFailures.remove(purchase.player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                supporterPurchases.remove(purchase.player, purchase);
                if (!player.isOnline()) return;
                plugin.getNetworkProfileCache().refresh(player);
                plugin.getTablistManager().markRefresh(player, "SUPPORTER_PURCHASED");
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder
                        && (holder.type == MenuType.SUPPORTER || holder.type == MenuType.SUPPORTER_CONFIRM)) openSupporter(player);
                player.sendMessage(CC.color("&aYou purchased &ePit Supporter&a!"));
                player.sendMessage(CC.color("&7The &e✬ Supporter Star &7has been enabled."));
            });
        } catch (Throwable failure) {
            purchase.failure = "PURCHASE_EXCEPTION";
            if (purchase.deducted && !purchase.entitlement && !purchase.compensated) {
                purchase.state = SupporterPurchaseState.COMPENSATION_REQUIRED;
                try { purchase.compensated = plugin.getAbelixHook().addGold(player, purchase.price); }
                catch (Throwable compensationFailure) { failure.addSuppressed(compensationFailure); }
            }
            profile.supporterOwned = false;
            supporterService.setPurchased(purchase.player, false);
            purchase.state = SupporterPurchaseState.FAILED;
            finishSupporterFailure(player, purchase, "&cThe Pit Supporter purchase failed. " + (purchase.compensated ? "Your Abelix Gold was restored." : "Staff have been notified."), failure);
        }
    }

    private void completeAlreadyOwned(Player player, Profile profile, SupporterPurchase purchase) {
        profile.supporterOwned = true;
        supporterService.setPurchased(purchase.player, true);
        purchase.failure = "NONE";
        purchase.result = "NO_CHANGE";
        purchase.state = SupporterPurchaseState.ALREADY_OWNED;
        boolean synchronizedProfile = plugin.getStorageManager().putOverride("pit-command-profile", purchase.player.toString(), profile.encode());
        if (!synchronizedProfile) supporterFailures.put(purchase.player, "OWNERSHIP_CACHE_PERSIST_FAILED");
        persistSupporterPurchase(purchase);
        if (synchronizedProfile) supporterFailures.remove(purchase.player);
        plugin.getLogger().info("supporter_purchase tx=" + purchase.transactionId + " player=" + purchase.player
                + " price=" + purchase.price + " deduction=false entitlement=false compensation=false state=ALREADY_OWNED result=NO_CHANGE");
        Bukkit.getScheduler().runTask(plugin, () -> {
            supporterPurchases.remove(purchase.player, purchase);
            if (!player.isOnline()) return;
            plugin.getNetworkProfileCache().refresh(player);
            plugin.getTablistManager().markRefresh(player, "SUPPORTER_OWNERSHIP_SYNCED");
            openSupporter(player);
            player.sendMessage(CC.color("&aYou already own Pit Supporter!"));
        });
    }

    private void finishSupporterFailure(Player player, SupporterPurchase purchase, String message, Throwable failure) {
        supporterFailures.put(purchase.player, purchase.failure);
        persistSupporterPurchase(purchase);
        String log = "supporter_purchase tx=" + purchase.transactionId + " player=" + purchase.player + " price=" + purchase.price
                + " balance=" + purchase.balance + " deduction=" + purchase.deducted + " entitlement=" + purchase.entitlement
                + " compensation=" + purchase.compensated + " state=" + purchase.state + " failure=" + purchase.failure;
        if (failure == null) plugin.getLogger().warning(log); else plugin.getLogger().log(Level.SEVERE, log, failure);
        Bukkit.getScheduler().runTask(plugin, () -> {
            supporterPurchases.remove(purchase.player, purchase);
            if (player.isOnline()) player.sendMessage(CC.color(message));
        });
    }

    private void persistSupporterPurchase(SupporterPurchase purchase) {
        plugin.getStorageManager().putOverride("pit-supporter-purchase", purchase.player.toString(), purchase.encode());
    }

    private boolean supporterAdmin(CommandSender sender, String[] args) {
        if (!me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.pitsupporter")) {
            sender.sendMessage(CC.color("&cYou do not have permission."));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (action.equals("reload")) {
            if (!plugin.registry().reload()) {
                sender.sendMessage(CC.color("&cPit Supporter settings could not be reloaded."));
                return true;
            }
            reload();
            sender.sendMessage(CC.color("&aPit Supporter settings reloaded."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&c/pitsupporter <grant|revoke|status> <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !loaded(target)) {
            sender.sendMessage(CC.color("&cThat player's profile is not loaded."));
            return true;
        }
        Profile profile = profiles.get(target.getUniqueId());
        if (action.equals("grant")) { profile.supporterGrant = true; supporterService.setGranted(target.getUniqueId(), true); }
        else if (action.equals("revoke")) { profile.supporterGrant = false; supporterService.setGranted(target.getUniqueId(), false); }
        else if (action.equals("status")) {
            sender.sendMessage(CC.color("&ePit Supporter for &f" + target.getName() + "&e: "
                    + (supporterEntitled(target) ? "&aActive" : "&cInactive") + " &7Grant: " + profile.supporterGrant));
            return true;
        } else {
            sender.sendMessage(CC.color("&cUnknown Pit Supporter action."));
            return true;
        }
        markDirty(profile);
        persistAsync(profile);
        plugin.getTablistManager().markRefresh(target, "SUPPORTER_GRANT_CHANGED");
        sender.sendMessage(CC.color("&aPit Supporter grant updated."));
        return true;
    }

    private boolean supporterDebug(CommandSender sender, String[] args) {
        if (!me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.supporterdebug")) {
            sender.sendMessage(CC.color("&cYou do not have permission."));
            return true;
        }
        Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
        if (target == null) {
            sender.sendMessage(CC.color("&c/pit supporter debug <player>"));
            return true;
        }
        Profile profile = profiles.get(target.getUniqueId());
        SupporterPurchase purchase = supporterPurchases.get(target.getUniqueId());
        sender.sendMessage(CC.color("&6Pit Supporter diagnostics for &f" + target.getName()));
        sender.sendMessage(CC.color("&7Profile loaded: &f" + (profile != null && plugin.getStatsManager().isProfileLoaded(target.getUniqueId()))));
        boolean authoritative = supporterService.ownsPitSupporter(target.getUniqueId());
        sender.sendMessage(CC.color("&7Entitlement loaded: &f" + supporterService.isLoaded(target.getUniqueId())));
        sender.sendMessage(CC.color("&7Normalized entitlement ID: &f" + PitSupporterService.ENTITLEMENT_ID));
        sender.sendMessage(CC.color("&7Owned: &f" + authoritative + " &7Star: &f" + supporterService.starEnabled(target.getUniqueId())));
        sender.sendMessage(CC.color("&7Cached entitlement: &f" + authoritative));
        sender.sendMessage(CC.color("&7Persistent ownership: &f" + supporterService.persistentOwnership(target.getUniqueId())));
        sender.sendMessage(CC.color("&7Legacy entitlement detected: &f" + supporterService.legacyDetected(target.getUniqueId())));
        sender.sendMessage(CC.color("&7Menu ownership result: &f" + authoritative));
        sender.sendMessage(CC.color("&7Purchase-handler ownership result: &f" + authoritative));
        sender.sendMessage(CC.color("&7Abelix available: &f" + (plugin.getAbelixHook() != null && plugin.getAbelixHook().isEnabled()) + " &7balance: &f" + plugin.getNetworkGold(target)));
        sender.sendMessage(CC.color("&7Pending purchase: &f" + (purchase == null ? "none" : purchase.transactionId + " " + purchase.state)));
        sender.sendMessage(CC.color("&7/show ownership result: &f" + authoritative));
        sender.sendMessage(CC.color("&7Chat name: &f" + decoratedName(target, false)));
        sender.sendMessage(CC.color("&7Tab name: &f" + decoratedName(target, true)));
        sender.sendMessage(CC.color("&7Last failure: &f" + supporterFailures.getOrDefault(target.getUniqueId(), "NONE")));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.getRecipients().removeIf(target -> !allowsChat(target.getUniqueId(), "player-chat"));
    }

    private boolean permanentFeature(Player player, String feature, int level) {
        if (!loaded(player)) return false;
        if (plugin.getLeaderboardManager().isFeatureUnlocked(player.getUniqueId(), feature)) return true;
        player.sendMessage(CC.color("&cRequires Level " + level + "."));
        return false;
    }

    private boolean loaded(Player player) {
        boolean value = player != null && profiles.containsKey(player.getUniqueId())
                && plugin.getStatsManager().isProfileLoaded(player.getUniqueId());
        if (!value && player != null) player.sendMessage(CC.color("&cYour The Pit profile is still loading."));
        return value;
    }

    private boolean tradeable(Player owner, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        String id = SecureItemTag.string(item, "ThePitItem");
        if (id.isEmpty()) return false;
        if (!SecureItemTag.string(item, "PitBundleOwner").isEmpty()) return false;
        for (String bound : settings().getStringList("pit-commands.trade.bound-item-ids")) {
            if (id.equalsIgnoreCase(bound)) return false;
        }
        String itemOwner = SecureItemTag.string(item, "MysticOwner");
        return itemOwner.isEmpty() || itemOwner.equals(owner.getUniqueId().toString());
    }

    private void deliverOrStash(UUID uuid, List<ItemStack> items) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) deliverOrStash(player, items);
        else for (ItemStack item : items) addToStash(uuid, item);
    }

    private void deliverOrStash(Player player, List<ItemStack> items) {
        Profile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        for (ItemStack item : items) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            for (ItemStack remainder : overflow.values()) addToStash(player.getUniqueId(), remainder);
        }
    }

    private List<ItemStack> items(Inventory inventory, List<Integer> slots) {
        List<ItemStack> values = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) values.add(item.clone());
        }
        return values;
    }

    private List<Integer> tradeSlots(boolean first) {
        List<Integer> values = first ? LEFT_SLOTS : RIGHT_SLOTS;
        int maximum = Math.max(1, Math.min(8, settings().getInt("pit-commands.trade.maximum-items-per-side", 8)));
        return values.subList(0, maximum);
    }

    private void clear(Inventory inventory, List<Integer> slots) {
        for (int slot : slots) inventory.setItem(slot, null);
    }

    private void resetDaily(Profile profile) {
        if (profile == null) return;
        int hour = Math.max(0, Math.min(23, settings().getInt("pit-commands.trade.daily-reset-hour-utc", 0)));
        String key = Instant.now().minus(hour, ChronoUnit.HOURS).atZone(ZoneOffset.UTC).toLocalDate().toString();
        if (key.equals(profile.dailyKey)) return;
        profile.dailyKey = key;
        profile.tradesToday = 0;
        profile.goldToday = 0L;
        markDirty(profile);
    }

    private void markDirty(Profile profile) {
        if (profile != null) dirty.add(profile.uuid);
    }

    private void flushDirty() {
        for (UUID uuid : new HashSet<>(dirty)) {
            Profile profile = profiles.get(uuid);
            if (profile != null && plugin.getStorageManager().putOverride("pit-command-profile", uuid.toString(), profile.encode())) {
                dirty.remove(uuid);
            }
        }
    }

    private void flushDirtyAsync() {
        Map<UUID, String> values = new LinkedHashMap<>();
        for (UUID uuid : new HashSet<>(dirty)) {
            Profile profile = profiles.get(uuid);
            if (profile != null) values.put(uuid, profile.encode());
        }
        if (values.isEmpty()) return;
        dirty.removeAll(values.keySet());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> failed = new HashSet<>();
            for (Map.Entry<UUID, String> entry : values.entrySet()) {
                if (!plugin.getStorageManager().putOverride("pit-command-profile", entry.getKey().toString(), entry.getValue())) {
                    failed.add(entry.getKey());
                }
            }
            if (!failed.isEmpty()) Bukkit.getScheduler().runTask(plugin, () -> dirty.addAll(failed));
        });
    }

    private void persistAsync(Profile profile) {
        String value = profile.encode();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getStorageManager().putOverride("pit-command-profile", profile.uuid.toString(), value));
    }

    private void tick() {
        managerTicks++;
        try {
            tickCarePackage();
        } catch (Throwable exception) {
            plugin.getLogger().severe("Care Package event failed and was cleaned up: " + exception.getMessage());
            cleanupCarePackage(false);
        }
        if (managerTicks % 20L != 0L) return;
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = profiles.get(player.getUniqueId());
            if (profile != null) {
                profile.permissionEntitled = me.outerspace.thepit.util.PermissionCompat.has(plugin, player, settings().getString(
                        "pit-supporter.entitlement-permission", "thepit.pit.supporter"));
                supporterService.setPermissionEntitled(player.getUniqueId(), profile.permissionEntitled);
            }
        }
        tradeRequests.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        for (Offer offer : new HashSet<>(offersBySender.values())) if (offer.expiresAt <= now) cancelOffer(offer, true);
        dropStates.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private void tickCarePackage() {
        long now = System.currentTimeMillis();
        CarePackageState state = carePackage;
        if (state == null) {
            if (settings().getBoolean("care-package.schedule.enabled", true) && now >= nextCarePackageAt
                    && Bukkit.getOnlinePlayers().size() >= Math.max(1, settings().getInt("care-package.schedule.minimum-online-players", 1))) {
                startCarePackage(plugin.getArenaManager().getCurrentArena(), null, false);
            }
            return;
        }
        if (!state.arenaId.equalsIgnoreCase(plugin.getArenaManager().getCurrentArena())
                || state.world == null || !state.world.isChunkLoaded(state.landing.getBlockX() >> 4, state.landing.getBlockZ() >> 4)) {
            cleanupCarePackage(false);
            return;
        }
        if (now >= state.expiresAt) {
            announceLine(settings().getString("care-package.announcements.expired", "&cThe Care Package expired."));
            cleanupCarePackage(false);
            return;
        }
        if (!state.landed) {
            if (state.falling == null || state.falling.isDead()
                    || state.falling.getLocation().getY() <= state.landing.getY() + 0.8D
                    || now >= state.landBy) landCarePackage(state);
            return;
        }
        if (state.finishPending || state.unlocked && inventoryEmpty(state.inventory)) cleanupCarePackage(true);
    }

    private String startCarePackage(String requestedArena, String requestedLocation, boolean test) {
        if (!Bukkit.isPrimaryThread()) return CC.color("&cCare Packages must be started from the server thread.");
        if (!settings().getBoolean("care-package.enabled", true)) return CC.color("&cCare Packages are disabled.");
        if (carePackage != null) return CC.color("&cA Care Package is already active.");
        String arena = PitYamlManager.normalizeArenaId(requestedArena);
        if (!arena.equalsIgnoreCase(plugin.getArenaManager().getCurrentArena())) {
            return CC.color("&cCare Packages can only run in the active arena.");
        }
        ArenaFeatureConfiguration.CarePackages configured = plugin.getYamlManager().carePackages(arena);
        if (!configured.enabled() || configured.locations().isEmpty()) {
            scheduleNextCarePackage();
            return CC.color("&cNo Care Package locations are configured for the active arena.");
        }
        List<ArenaFeatureConfiguration.CarePackageLocation> candidates = new ArrayList<>();
        for (ArenaFeatureConfiguration.CarePackageLocation value : configured.locations().values()) {
            if (requestedLocation != null && !requestedLocation.isEmpty() && !value.id().equalsIgnoreCase(requestedLocation)) continue;
            Location landing = value.location().resolve();
            if (landing != null && safeLanding(landing, arena)) candidates.add(value);
        }
        if (candidates.isEmpty()) {
            scheduleNextCarePackage();
            return CC.color("&cNo safe loaded Care Package location matched.");
        }
        ArenaFeatureConfiguration.CarePackageLocation selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        Location landing = selected.location().resolve();
        World world = landing.getWorld();
        Block block = landing.getBlock();
        Location drop = landing.clone().add(0.0D, selected.dropHeight(), 0.0D);
        FallingBlock falling = world.spawnFallingBlock(drop, Material.CHEST, (byte) 0);
        falling.setDropItem(false);
        falling.setVelocity(new org.bukkit.util.Vector(0.0D,
                settings().getDouble("care-package.falling.initial-velocity", -0.25D), 0.0D));
        long now = System.currentTimeMillis();
        long duration = Math.max(1L, settings().getLong("care-package.duration-seconds", 180L)) * 1000L;
        CarePackageState state = new CarePackageState(arena, selected.id(), world, landing, block.getType(),
                block.getData(), falling, now + duration, now + Math.max(3L,
                settings().getLong("care-package.falling.maximum-seconds", 15L)) * 1000L);
        carePackage = state;
        announceCarePackage("start", selected.id());
        refreshScoreboards();
        if (plugin.getAchievementManager() != null) {
            for (Player player : world.getPlayers()) plugin.getAchievementManager().publish(
                    new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.EVENT_STARTED,
                            1L, Map.of("event", "carepackage"), false));
        }
        return CC.color("&aStarted Care Package at &e" + selected.id() + "&a.");
    }

    private void landCarePackage(CarePackageState state) {
        if (state != carePackage || state.landed) return;
        if (state.falling != null && !state.falling.isDead()) state.falling.remove();
        state.falling = null;
        Block block = state.landing.getBlock();
        if (!safeLanding(state.landing, state.arenaId)) {
            cleanupCarePackage(false);
            return;
        }
        block.setType(Material.CHEST);
        if (!(block.getState() instanceof Chest chest)) {
            cleanupCarePackage(false);
            return;
        }
        state.inventory = chest.getBlockInventory();
        state.inventory.clear();
        populateCarePackage(state.inventory);
        state.landed = true;
        updateCarePackageHologram(state);
        announceCarePackage("landed", state.locationId);
        refreshScoreboards();
    }

    private void populateCarePackage(Inventory inventory) {
        List<ItemStack> rewards = new ArrayList<>();
        List<String> mystics = Arrays.asList("sword", "bow", "pants");
        ItemStack guaranteed = plugin.getShopManager().createMysticItem(
                mystics.get(ThreadLocalRandom.current().nextInt(mystics.size())));
        if (guaranteed == null || !plugin.getShopManager().isMysticItem(guaranteed)) {
            throw new IllegalStateException("Mystic item factory did not return a real Mystic item");
        }
        rewards.add(guaranteed);
        int additional = Math.max(3, settings().getInt("care-package.loot.additional-rewards", 5));
        List<String> pool = settings().getStringList("care-package.loot.pool");
        if (pool.isEmpty()) pool = Arrays.asList("pit-xp", "pit-gold", "diamond-armor", "diamond-sword",
                "mystic-sword", "mystic-bow", "fresh-pants");
        int added = 0;
        int attempts = 0;
        while (added < additional && attempts++ < additional * 4) {
            ItemStack reward = carePackageReward(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
            if (reward != null) {
                rewards.add(reward);
                added++;
            }
        }
        while (added++ < additional) rewards.add(carePackageReward("pit-xp"));
        Collections.shuffle(rewards);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) slots.add(slot);
        Collections.shuffle(slots);
        for (int index = 0; index < rewards.size() && index < slots.size(); index++) inventory.setItem(slots.get(index), rewards.get(index));
    }

    private ItemStack carePackageReward(String raw) {
        String id = normalize(raw);
        if (id.equals("pit-xp") || id.equals("xp")) {
            return currencyToken(Material.EXP_BOTTLE, "&b100 Pit XP", "XP",
                    Math.max(1, settings().getInt("care-package.loot.pit-xp", 100)));
        }
        if (id.equals("pit-gold") || id.equals("gold")) {
            return currencyToken(Material.GOLD_INGOT, "&6100 Pit Gold", "GOLD",
                    Math.max(1, settings().getInt("care-package.loot.pit-gold", 100)));
        }
        if (id.equals("diamond-sword")) return SecureItemTag.unbreakable(new ItemStack(Material.DIAMOND_SWORD));
        if (id.equals("diamond-armor")) {
            Material[] armor = {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
            return SecureItemTag.unbreakable(new ItemStack(armor[ThreadLocalRandom.current().nextInt(armor.length)]));
        }
        if (id.equals("mystic-sword")) return plugin.getShopManager().createMysticItem("sword");
        if (id.equals("mystic-bow")) return plugin.getShopManager().createMysticItem("bow");
        if (id.equals("fresh-pants")) return plugin.getShopManager().createMysticItem("pants");
        return null;
    }

    private ItemStack currencyToken(Material material, String name, String type, int amount) {
        ItemStack token = item(material, 0, name, Arrays.asList("&7Care Package reward", "", "&eClick to claim!"), true);
        token = SecureItemTag.string(token, "ThePitItem", "care_package_" + type.toLowerCase(Locale.ENGLISH));
        token = SecureItemTag.string(token, "CarePackageReward", type);
        token = SecureItemTag.integer(token, "CarePackageAmount", amount);
        token = SecureItemTag.string(token, "CarePackageTokenId", UUID.randomUUID().toString());
        return token;
    }

    private void clickCarePackage(Player player) {
        CarePackageState state = carePackage;
        if (player == null || state == null || !state.landed || state.unlocked
                || player.getWorld() != state.world || !state.arenaId.equalsIgnoreCase(plugin.getArenaManager().getCurrentArena())) return;
        long cooldown = Math.max(1L, settings().getLong("care-package.click-cooldown-ticks", 2L));
        long last = state.clickTicks.getOrDefault(player.getUniqueId(), Long.MIN_VALUE / 2L);
        if (managerTicks - last < cooldown) return;
        state.clickTicks.put(player.getUniqueId(), managerTicks);
        if (state.clicks >= requiredClicks()) return;
        state.clicks++;
        state.world.playEffect(state.landing, Effect.CLICK1, 0);
        if (state.clicks >= requiredClicks()) unlockCarePackage(state);
        else updateCarePackageHologram(state);
    }

    private void unlockCarePackage(CarePackageState state) {
        if (state != carePackage || state.unlocked) return;
        state.clicks = requiredClicks();
        state.unlocked = true;
        updateCarePackageHologram(state);
        announceCarePackage("unlocked", state.locationId);
        refreshScoreboards();
    }

    private void claimCarePackageReward(Player player, int slot) {
        CarePackageState state = carePackage;
        if (state == null || !state.unlocked || state.inventory == null || slot < 0 || slot >= state.inventory.getSize()) return;
        if (!profiles.containsKey(player.getUniqueId()) || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYour The Pit profile is still loading."));
            return;
        }
        ItemStack reward = state.inventory.getItem(slot);
        if (reward == null || reward.getType() == Material.AIR) return;
        state.inventory.setItem(slot, null);
        String type = SecureItemTag.string(reward, "CarePackageReward");
        int amount = Math.max(0, SecureItemTag.getInteger(reward, "CarePackageAmount", 0));
        if (type.equals("XP")) {
            plugin.getStatsManager().addXp(player, amount, StatsManager.XpSource.EVENT);
            player.sendMessage(CC.color("&b+" + amount + " Pit XP"));
        } else if (type.equals("GOLD")) {
            int awarded = plugin.getPitGoldService().award(player, amount, ThePitPlugin.GoldSource.EVENT);
            player.sendMessage(CC.color("&6+" + awarded + " Pit Gold"));
        } else {
            deliverOrStash(player, Collections.singletonList(reward));
        }
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.EVENT_SCORE_CHANGED,
                        1L, Map.of("id", "care_package_item"), false));
        if (inventoryEmpty(state.inventory)) state.finishPending = true;
    }

    private void updateCarePackageHologram(CarePackageState state) {
        if (state == null || state.landing == null || plugin.getNpcService() == null) return;
        List<String> lines;
        if (state.unlocked) lines = settings().getStringList("care-package.hologram.unlocked");
        else {
            lines = new ArrayList<>();
            for (String line : settings().getStringList("care-package.hologram.locked")) {
                lines.add(line.replace("%clicks%", String.valueOf(state.clicks))
                        .replace("%required_clicks%", String.valueOf(requiredClicks())));
            }
        }
        if (lines.isEmpty()) lines = state.unlocked
                ? Arrays.asList("&a&lCARE PACKAGE", "&eRight-click to open!")
                : Arrays.asList("&e&lCARE PACKAGE", "&f" + state.clicks + "&7/&f" + requiredClicks() + " clicks", "&eClick to unlock!");
        Location location = state.landing.clone().add(0.0D, settings().getDouble("care-package.hologram.height", 2.25D), 0.0D);
        state.hologramId = plugin.getNpcService().replaceTransientHologram(state.hologramId, location, lines);
    }

    private void announceCarePackage(String stage, String quadrant) {
        List<String> lines = settings().getStringList("care-package.announcements." + stage);
        if (lines.isEmpty()) {
            if (stage.equals("start")) lines = Arrays.asList("&e&lMINOR EVENT! &fCare Package",
                    "&7A Care Package is dropping in the &e%quadrant% &7quadrant!");
            else if (stage.equals("landed")) lines = Collections.singletonList("&eThe Care Package has landed!");
            else lines = Collections.singletonList("&a&lCARE PACKAGE UNLOCKED!");
        }
        for (String line : lines) announceLine(line.replace("%quadrant%", readable(quadrant)));
        String title = settings().getString("care-package.feedback." + stage + ".title", "");
        String subtitle = settings().getString("care-package.feedback." + stage + ".subtitle", "");
        String soundName = settings().getString("care-package.feedback." + stage + ".sound",
                stage.equals("unlocked") ? "LEVEL_UP" : "NOTE_PLING");
        World world = carePackage == null ? null : carePackage.world;
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            if (!title.isEmpty() || !subtitle.isEmpty()) plugin.getLevelUpFeedbackManager().queueTitle(player, title, subtitle, 0L);
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase(Locale.ENGLISH)), 1.0F, 1.0F);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void announceLine(String line) {
        broadcast("minor-events", CC.color(line));
    }

    private boolean safeLanding(Location location, String arenaId) {
        if (location == null || location.getWorld() == null) return false;
        me.outerspace.thepit.registry.definition.ArenaDefinition arena = plugin.getArenaManager().getArena(arenaId).orElse(null);
        if (arena == null || !arena.worldName().equalsIgnoreCase(location.getWorld().getName())
                || Bukkit.getWorld(arena.worldName()) != location.getWorld()) return false;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        Block block = location.getBlock();
        Block below = block.getRelative(0, -1, 0);
        return block.getType() == Material.AIR && below.getType() != Material.AIR && below.getType().isSolid()
                && location.getBlockY() > 1 && location.getBlockY() < location.getWorld().getMaxHeight() - 2;
    }

    private int requiredClicks() {
        return Math.max(1, settings().getInt("care-package.required-clicks", 200));
    }

    private boolean inventoryEmpty(Inventory inventory) {
        if (inventory == null) return true;
        for (ItemStack item : inventory.getContents()) if (item != null && item.getType() != Material.AIR) return false;
        return true;
    }

    private boolean isCareInventory(Inventory inventory) {
        return carePackage != null && carePackage.inventory != null && carePackage.inventory.equals(inventory);
    }

    private boolean isCareBlock(Block block) {
        CarePackageState state = carePackage;
        return block != null && state != null && state.landed && block.getWorld() == state.world
                && block.getX() == state.landing.getBlockX() && block.getY() == state.landing.getBlockY()
                && block.getZ() == state.landing.getBlockZ();
    }

    private void cleanupCarePackage(boolean emptied) {
        CarePackageState state = carePackage;
        carePackage = null;
        if (state == null) {
            scheduleNextCarePackage();
            return;
        }
        if (state.inventory != null) {
            for (Player player : new ArrayList<>(state.inventory.getViewers()).stream()
                    .filter(Player.class::isInstance).map(Player.class::cast).toList()) player.closeInventory();
            state.inventory.clear();
        }
        if (state.falling != null && !state.falling.isDead()) state.falling.remove();
        for (Entity entity : new ArrayList<>(state.carriers)) if (entity != null && !entity.isDead()) entity.remove();
        if (plugin.getNpcService() != null) plugin.getNpcService().removeTransientHologram(state.hologramId);
        Block block = state.landing == null ? null : state.landing.getBlock();
        if (block != null && block.getWorld() == state.world && block.getType() == Material.CHEST) {
            block.setType(state.originalType);
            block.setData(state.originalData);
        }
        state.clickTicks.clear();
        state.carriers.clear();
        state.inventory = null;
        scheduleNextCarePackage();
        refreshScoreboards();
        if (emptied && plugin.getAchievementManager() != null) {
            for (Player player : state.world.getPlayers()) plugin.getAchievementManager().publish(
                    new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.EVENT_FINISHED,
                            1L, Map.of("event", "carepackage"), false));
        }
    }

    private void scheduleNextCarePackage() {
        int minimum = Math.max(30, settings().getInt("care-package.schedule.minimum-delay-seconds", 900));
        int maximum = Math.max(minimum, settings().getInt("care-package.schedule.maximum-delay-seconds", 1500));
        nextCarePackageAt = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(minimum, (long) maximum + 1L) * 1000L;
    }

    private void refreshScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) plugin.getBuildScoreboardManager().update(player);
    }

    private RegistryNode settings() {
        return plugin.registry().settings();
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.STAINED_GLASS_PANE, 15, " ", Collections.emptyList(), false);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private void put(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
    }

    private ItemStack item(Material material, int data, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        if (glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material material(String value, Material fallback) {
        Material result = Material.matchMaterial(value == null ? "" : value);
        return result == null ? fallback : result;
    }

    private String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private String readable(String value) {
        String[] parts = value.toLowerCase(Locale.ENGLISH).replace('_', ' ').split(" ");
        StringBuilder output = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!output.isEmpty()) output.append(' ');
            output.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return output.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }

    private String countdown(long start) {
        long seconds = Math.max(0L, (start - System.currentTimeMillis() + 59999L) / 60000L) * 60L;
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        if (days > 0L) return days + " day" + (days == 1 ? "" : "s") + ", " + hours + " hours";
        long minutes = Math.max(1L, seconds / 60L);
        if (seconds >= 86400L) return Math.max(1L, Math.round(seconds / 3600.0D)) + " hours";
        if (minutes >= 60L) return Math.max(1L, Math.round(minutes / 60.0D)) + " hours";
        return minutes + " minutes";
    }

    private String fingerprint(ItemStack item, int slot) {
        return slot + ":" + SecureItemTag.string(item, "ThePitItem") + ":" + SecureItemTag.string(item, "PitBundleId")
                + ":" + item.getType() + ":" + item.getAmount() + ":" + item.getDurability();
    }

    private String itemNbt(ItemStack item) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);
            Class<?> craft = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            Object nms = craft.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            Class<?> compound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            Object tag = compound.getConstructor().newInstance();
            Method save = nms.getClass().getMethod("save", compound);
            return String.valueOf(save.invoke(nms, tag));
        } catch (Throwable ignored) {
            return "{id:\"" + item.getType().name().toLowerCase(Locale.ENGLISH) + "\",Count:" + item.getAmount() + "b}";
        }
    }

    private static Map<String, DyeColor> dyes() {
        Map<String, DyeColor> values = new LinkedHashMap<>();
        values.put("peace-white", new DyeColor("Peace White", 0xFFFFFF));
        values.put("business-gray", new DyeColor("Business Gray", 0xA9A9A9));
        values.put("pure-red", new DyeColor("Pure Red", 0xFF0000));
        values.put("blood-red", new DyeColor("Blood Red", 0x660000));
        values.put("solid-teal", new DyeColor("Solid Teal", 0x008080));
        values.put("deep-sky", new DyeColor("Deep Sky", 0x00BFFF));
        values.put("dark-orchid", new DyeColor("Dark Orchid", 0x9932CC));
        values.put("smooth-crimson", new DyeColor("Smooth Crimson", 0xC6143A));
        values.put("hot-pink", new DyeColor("Hot Pink", 0xFF00FF));
        values.put("ugly-classic", new DyeColor("Ugly Classic", 0xA06540));
        values.put("shadow-gray", new DyeColor("Shadow Gray", 0x606060));
        values.put("night-rider", new DyeColor("Night Rider", 0x400060));
        values.put("light-lavender", new DyeColor("Light Lavender", 0xDDAADD));
        values.put("blueberry-blues", new DyeColor("Blueberry Blues", 0x555599));
        values.put("soothing-beige", new DyeColor("Soothing Beige", 0xFFDDAA));
        values.put("not-orange", new DyeColor("Not Orange", 0xFFBB00));
        values.put("neon-green", new DyeColor("Neon Green", 0xAAE100));
        values.put("harvest-red", new DyeColor("Harvest Red", 0xEE3300));
        return Collections.unmodifiableMap(values);
    }

    private Map<String, DyeColor> configuredDyes() {
        RegistryNode section = settings().getConfigurationSection("pit-supporter.pants-dye.colors");
        if (section == null) return DYES;
        Map<String, DyeColor> values = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            String hex = section.getString(id + ".hex", "").replace("#", "");
            try {
                int rgb = Integer.parseInt(hex, 16);
                if (rgb >= 0 && rgb <= 0xFFFFFF) {
                    values.put(id, new DyeColor(section.getString(id + ".display", readable(id)), rgb));
                }
            } catch (NumberFormatException ignored) { }
        }
        return values.isEmpty() ? DYES : values;
    }

    private enum MenuType {
        CHAT, VIEW, SUPPORTER, SUPPORTER_CONFIRM, DYES, SHOW_CONFIRM, EVENTS, OFFER
    }

    private static final class MenuHolder implements InventoryHolder {
        private final UUID owner;
        private final MenuType type;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;
        private ItemStack item;
        private Offer offer;

        private MenuHolder(UUID owner, MenuType type) {
            this.owner = owner;
            this.type = type;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class TradeHolder implements InventoryHolder {
        private final UUID first;
        private final UUID second;
        private Inventory inventory;

        private TradeHolder(UUID first, UUID second) {
            this.first = first;
            this.second = second;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class TradeSession {
        private final UUID first;
        private final UUID second;
        private final Inventory inventory;
        private long firstGold;
        private long secondGold;
        private boolean firstConfirmed;
        private boolean secondConfirmed;
        private boolean completing;
        private boolean cancelled;

        private TradeSession(UUID first, UUID second, Inventory inventory) {
            this.first = first;
            this.second = second;
            this.inventory = inventory;
        }
    }

    private static final class CarePackageState {
        private final String arenaId;
        private final String locationId;
        private final World world;
        private final Location landing;
        private final Material originalType;
        private final byte originalData;
        private final long expiresAt;
        private final long landBy;
        private final Map<UUID, Long> clickTicks = new HashMap<>();
        private final Set<Entity> carriers = new HashSet<>();
        private FallingBlock falling;
        private Inventory inventory;
        private UUID hologramId;
        private int clicks;
        private boolean landed;
        private boolean unlocked;
        private boolean finishPending;

        private CarePackageState(String arenaId, String locationId, World world, Location landing,
                                 Material originalType, byte originalData, FallingBlock falling,
                                 long expiresAt, long landBy) {
            this.arenaId = arenaId;
            this.locationId = locationId;
            this.world = world;
            this.landing = landing.clone();
            this.originalType = originalType;
            this.originalData = originalData;
            this.falling = falling;
            this.expiresAt = expiresAt;
            this.landBy = landBy;
        }
    }

    private static final class Profile {
        private final UUID uuid;
        private final Set<String> disabledChat = ConcurrentHashMap.newKeySet();
        private final List<ItemStack> stash = new ArrayList<>();
        private String dailyKey = "";
        private int tradesToday;
        private long goldToday;
        private boolean dropConfirm = true;
        private volatile boolean supporterGrant;
        private volatile boolean supporterOwned;
        private volatile boolean starVisible = true;
        private boolean fancierHat;
        private volatile boolean permissionEntitled;
        private boolean legacySupporterDetected;

        private Profile(UUID uuid) {
            this.uuid = uuid;
        }

        private String encode() {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("daily-key", dailyKey);
            yaml.set("trades-today", tradesToday);
            yaml.set("gold-today", goldToday);
            yaml.set("drop-confirm", dropConfirm);
            yaml.set("supporter-grant", supporterGrant);
            yaml.set("supporter-owned", supporterOwned);
            yaml.set("star-visible", starVisible);
            yaml.set("fancier-hat", fancierHat);
            yaml.set("disabled-chat", new ArrayList<>(disabledChat));
            yaml.set("stash", stash);
            return yaml.saveToString();
        }

        private static Profile decode(UUID uuid, String stored) {
            Profile profile = new Profile(uuid);
            if (stored == null || stored.isEmpty()) return profile;
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.loadFromString(stored);
                profile.dailyKey = yaml.getString("daily-key", "");
                profile.tradesToday = Math.max(0, yaml.getInt("trades-today", 0));
                profile.goldToday = Math.max(0L, yaml.getLong("gold-today", 0L));
                profile.dropConfirm = yaml.getBoolean("drop-confirm", true);
                profile.supporterGrant = yaml.getBoolean("supporter-grant", false);
                profile.supporterOwned = yaml.getBoolean("supporter-owned", false);
                for (String legacy : Arrays.asList("pit-supporter", "pit_supporter", "PitSupporter", "PIT_SUPPORTER", "buildffa_pit_supporter", "thepit_supporter")) {
                    if (!yaml.getBoolean(legacy, false)) continue;
                    profile.supporterOwned = true;
                    profile.legacySupporterDetected = true;
                }
                profile.starVisible = yaml.getBoolean("star-visible", true);
                profile.fancierHat = yaml.getBoolean("fancier-hat", false);
                profile.disabledChat.addAll(yaml.getStringList("disabled-chat"));
                for (Object value : yaml.getList("stash", Collections.emptyList())) {
                    if (value instanceof ItemStack item) profile.stash.add(item);
                }
            } catch (Exception ignored) { }
            return profile;
        }
    }

    private record Request(UUID sender, long expiresAt) {
    }

    private record Offer(UUID sender, UUID recipient, ItemStack item, long price, long expiresAt) {
    }

    private record DropState(String fingerprint, int presses, long expiresAt) {
    }

    private enum SupporterPurchaseState {
        PENDING, PAYMENT_COMMITTED, ENTITLEMENT_COMMITTED, COMPENSATION_REQUIRED, COMPLETED, ALREADY_OWNED, FAILED
    }

    private static final class SupporterPurchase {
        private final UUID transactionId;
        private final UUID player;
        private final int price;
        private volatile SupporterPurchaseState state = SupporterPurchaseState.PENDING;
        private volatile Integer balance;
        private volatile boolean deducted;
        private volatile boolean entitlement;
        private volatile boolean compensated;
        private volatile String failure = "NONE";
        private volatile String result = "PENDING";

        private SupporterPurchase(UUID transactionId, UUID player, int price) {
            this.transactionId = transactionId;
            this.player = player;
            this.price = price;
        }

        private String encode() {
            return transactionId + "|" + player + "|" + price + "|" + state + "|" + (balance == null ? "" : balance)
                    + "|" + deducted + "|" + entitlement + "|" + compensated + "|" + failure + "|" + result;
        }
    }

    private record DyeColor(String name, int rgb) {
    }
}
