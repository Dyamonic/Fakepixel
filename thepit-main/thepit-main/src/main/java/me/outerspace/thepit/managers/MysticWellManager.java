package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.ItemStackCodec;
import me.outerspace.thepit.util.PitGoldFormatter;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class MysticWellManager implements Listener {
    private static final int ITEM_SLOT = 11;
    private static final int SACRIFICE_SLOT = 15;
    private static final int UPGRADE_SLOT = 22;
    private static final int CLOSE_SLOT = 26;
    private static final String RECOVERY_NAMESPACE = "mystic-well-recovery";
    private final ThePitPlugin plugin;
    private final Set<String> wells = ConcurrentHashMap.newKeySet();
    private final Set<String> enderChests = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> transactions = ConcurrentHashMap.newKeySet();
    private final Map<String, Cost> costs = new HashMap<>();

    public MysticWellManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        registerCosts();
    }

    public void start() {
        loadRegisteredBlocks();
        Bukkit.getScheduler().runTaskLater(plugin, this::discoverInitialUtilityBlocks, 20L);
    }

    public void shutdown() {
        for (Session session : new ArrayList<>(sessions.values())) closeSession(Bukkit.getPlayer(session.owner), session.sessionId, MysticWellCloseReason.SERVER_DISABLE);
        sessions.clear();
        transactions.clear();
    }

    public boolean isProtectedLocation(Location location, int radius) {
        if (location == null || location.getWorld() == null) return false;
        int distance = Math.max(0, radius);
        for (int x = -distance; x <= distance; x++) {
            for (int y = -distance; y <= distance; y++) {
                for (int z = -distance; z <= distance; z++) {
                    String candidate = key(location.clone().add(x, y, z));
                    if (wells.contains(candidate) || enderChests.contains(candidate)) return true;
                }
            }
        }
        return false;
    }

    private void registerCosts() {
        costs.put("mystic_sword:1", new Cost(1000, ""));
        costs.put("mystic_sword:2", new Cost(4000, ""));
        costs.put("mystic_sword:3", new Cost(8000, "standard_pants"));
        costs.put("mystic_bow:1", new Cost(1000, ""));
        costs.put("mystic_bow:2", new Cost(4000, ""));
        costs.put("mystic_bow:3", new Cost(8000, "standard_pants"));
        costs.put("fresh_pants:1", new Cost(1000, ""));
        costs.put("fresh_pants:2", new Cost(4000, ""));
        costs.put("fresh_pants:3", new Cost(8000, "same"));
        costs.put("fresh_aqua_pants:1", new Cost(5000, ""));
        costs.put("dark_pants:1", new Cost(10000, ""));
        costs.put("dark_pants:2", new Cost(60000, "same"));
        costs.put("sewer_pants:1", new Cost(150, ""));
        costs.put("rage_pants:1", new Cost(1000, ""));
        costs.put("rage_pants:2", new Cost(4000, ""));
        costs.put("rage_pants:3", new Cost(8000, "same"));
    }

    private void loadRegisteredBlocks() {
        String raw = plugin.getStorageManager().getOverride("pit-utility-blocks", "global").orElse("");
        for (String part : raw.split(";")) {
            if (part.startsWith("W|")) wells.add(part.substring(2));
            else if (part.startsWith("E|")) enderChests.add(part.substring(2));
        }
    }

    private void discoverInitialUtilityBlocks() {
        Location spawn = plugin.getArenaManager().getCurrentSpawn();
        if (spawn == null || spawn.getWorld() == null) return;
        boolean changed = false;
        int radius = 24;
        for (int x = -radius; x <= radius; x++) for (int y = -8; y <= 12; y++) for (int z = -radius; z <= radius; z++) {
            Block block = spawn.getWorld().getBlockAt(spawn.getBlockX() + x, spawn.getBlockY() + y, spawn.getBlockZ() + z);
            if (!plugin.getArenaManager().isInCurrentSpawnProtection(block.getLocation())) continue;
            if (block.getType() == Material.ENCHANTMENT_TABLE) changed |= wells.add(key(block.getLocation()));
            else if (block.getType() == Material.ENDER_CHEST) changed |= enderChests.add(key(block.getLocation()));
        }
        if (changed) saveRegisteredBlocks();
    }

    private void saveRegisteredBlocks() {
        List<String> values = new ArrayList<>();
        for (String value : wells) values.add("W|" + value);
        for (String value : enderChests) values.add("E|" + value);
        plugin.getStorageManager().putOverride("pit-utility-blocks", "global", String.join(";", values));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        String location = key(event.getClickedBlock().getLocation());
        if (enderChests.contains(location)) {
            event.setCancelled(true);
            if (plugin.getStatsManager().getLevel(player.getUniqueId()) < 15) {
                player.sendMessage(CC.color("&cYou must be level 15 to use the Ender Chest!"));
                return;
            }
            player.openInventory(player.getEnderChest());
            return;
        }
        if (!wells.contains(location)) return;
        event.setCancelled(true);
        if (plugin.getRenownShopManager() == null || !plugin.getRenownShopManager().owns(player.getUniqueId(), "mysticism")) {
            player.sendMessage(CC.color("&cYou must unlock Mysticism to use the Mystic Well!"));
            return;
        }
        open(player);
    }

    private void open(Player player) {
        recoverStoredItems(player);
        Session existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            if (isGenuinelyActive(player, existing)) {
                player.sendMessage(CC.color("&cYou already have a Mystic Well session open."));
                return;
            }
            closeSession(player, existing.sessionId, MysticWellCloseReason.STALE_SESSION);
        }
        UUID sessionId = UUID.randomUUID();
        MysticWellInventoryHolder holder = new MysticWellInventoryHolder(sessionId, player.getUniqueId());
        Inventory inventory;
        try {
            inventory = Bukkit.createInventory(holder, 27, CC.color("&8Mystic Well"));
            holder.inventory = inventory;
            Session session = new Session(sessionId, player.getUniqueId(), inventory);
            redraw(session);
            player.openInventory(inventory);
            if (player.getOpenInventory().getTopInventory() != inventory) throw new IllegalStateException("inventory did not open");
            sessions.put(player.getUniqueId(), session);
        } catch (Throwable failure) {
            sessions.remove(player.getUniqueId());
            player.sendMessage(CC.color("&cThe Mystic Well could not be opened. Please try again."));
        }
    }

    private boolean isGenuinelyActive(Player player, Session session) {
        MysticWellSessionState state = session.state.get();
        if (state != MysticWellSessionState.OPEN && state != MysticWellSessionState.PROCESSING && state != MysticWellSessionState.CLOSING) return false;
        if (!player.isOnline()) return false;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != session.inventory) return false;
        if (!(top.getHolder() instanceof MysticWellInventoryHolder holder)) return false;
        return holder.playerId.equals(player.getUniqueId()) && holder.sessionId.equals(session.sessionId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MysticWellInventoryHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !holder.playerId.equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Session session = activeSession(player, holder, top);
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        if (session.state.get() == MysticWellSessionState.PROCESSING) {
            event.setCancelled(true);
            return;
        }
        int raw = event.getRawSlot();
        int topSize = top.getSize();
        if (raw >= 0 && raw < topSize) {
            event.setCancelled(true);
            if (raw == ITEM_SLOT) handleDirectSlotClick(player, session, ITEM_SLOT, false, event);
            else if (raw == SACRIFICE_SLOT) handleDirectSlotClick(player, session, SACRIFICE_SLOT, true, event);
            else if (raw == UPGRADE_SLOT) upgrade(player, session);
            else if (raw == CLOSE_SLOT) player.closeInventory();
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            handleShiftClick(player, session, event);
            return;
        }
        String clickName = event.getClick().name();
        if (clickName.contains("DOUBLE") || clickName.contains("NUMBER") || clickName.contains("DROP")) event.setCancelled(true);
    }

    private Session activeSession(Player player, MysticWellInventoryHolder holder, Inventory top) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return null;
        if (!session.sessionId.equals(holder.sessionId) || !session.owner.equals(holder.playerId) || session.inventory != top) return null;
        MysticWellSessionState state = session.state.get();
        return state == MysticWellSessionState.OPEN || state == MysticWellSessionState.PROCESSING ? session : null;
    }

    private void handleDirectSlotClick(Player player, Session session, int slot, boolean sacrificeSlot, InventoryClickEvent event) {
        String clickName = event.getClick().name();
        if (event.isShiftClick() || clickName.contains("NUMBER") || clickName.contains("DOUBLE") || clickName.contains("DROP")) return;
        ItemStack cursor = clone(player.getItemOnCursor());
        ItemStack current = clone(session.inventory.getItem(slot));
        if (air(cursor)) {
            if (air(current)) return;
            session.inventory.setItem(slot, null);
            player.setItemOnCursor(current);
            redraw(session);
            return;
        }
        if (cursor.getAmount() > cursor.getMaxStackSize()) return;
        if (sacrificeSlot) {
            if (!sacrificeRequired(session) || !validCurrentSacrifice(session, cursor)) {
                player.sendMessage(CC.color("&cThat item is not a valid sacrifice for this upgrade!"));
                return;
            }
        } else if (!validMystic(cursor)) {
            player.sendMessage(CC.color("&cYou can only place a Mystic item in the Mystic Well!"));
            return;
        }
        session.inventory.setItem(slot, cursor);
        player.setItemOnCursor(air(current) ? new ItemStack(Material.AIR) : current);
        redraw(session);
    }

    private void handleShiftClick(Player player, Session session, InventoryClickEvent event) {
        ItemStack source = clone(event.getCurrentItem());
        if (air(source)) return;
        if (source.getAmount() > source.getMaxStackSize()) return;
        int destination = -1;
        if (air(session.inventory.getItem(ITEM_SLOT)) && validMystic(source)) destination = ITEM_SLOT;
        else if (!air(session.inventory.getItem(ITEM_SLOT)) && air(session.inventory.getItem(SACRIFICE_SLOT)) && sacrificeRequired(session) && validCurrentSacrifice(session, source)) destination = SACRIFICE_SLOT;
        if (destination < 0) {
            if (!validMystic(source)) player.sendMessage(CC.color("&cYou can only place a Mystic item in the Mystic Well!"));
            return;
        }
        session.inventory.setItem(destination, source);
        event.setCurrentItem(null);
        redraw(session);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MysticWellInventoryHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player) || !holder.playerId.equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Session session = activeSession(player, holder, top);
        if (session == null || session.state.get() == MysticWellSessionState.PROCESSING) {
            event.setCancelled(true);
            return;
        }
        Set<Integer> topSlots = new HashSet<>();
        for (Integer raw : event.getRawSlots()) if (raw >= 0 && raw < top.getSize()) topSlots.add(raw);
        if (topSlots.isEmpty()) return;
        if (topSlots.size() != 1) {
            event.setCancelled(true);
            return;
        }
        int destination = topSlots.iterator().next();
        ItemStack dragged = clone(event.getOldCursor());
        if (destination == ITEM_SLOT) {
            if (!validMystic(dragged) || !air(session.inventory.getItem(ITEM_SLOT))) {
                event.setCancelled(true);
                if (!validMystic(dragged)) player.sendMessage(CC.color("&cYou can only place a Mystic item in the Mystic Well!"));
                return;
            }
        } else if (destination == SACRIFICE_SLOT) {
            if (!sacrificeRequired(session) || !validCurrentSacrifice(session, dragged) || !air(session.inventory.getItem(SACRIFICE_SLOT))) {
                event.setCancelled(true);
                return;
            }
        } else {
            event.setCancelled(true);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Session current = sessions.get(player.getUniqueId());
            if (current != session || current.state.get() != MysticWellSessionState.OPEN) return;
            redraw(current);
            player.updateInventory();
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MysticWellInventoryHolder holder)) return;
        closeSession(player, holder.sessionId, MysticWellCloseReason.INVENTORY_CLOSE);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        closePlayerSession(event.getPlayer(), MysticWellCloseReason.DISCONNECT);
        transactions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        closePlayerSession(event.getPlayer(), MysticWellCloseReason.WORLD_CHANGE);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        closePlayerSession(event.getEntity(), MysticWellCloseReason.DEATH);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
        transactions.remove(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recoverStoredItems(event.getPlayer()), 2L);
    }

    private void closePlayerSession(Player player, MysticWellCloseReason reason) {
        Session session = sessions.get(player.getUniqueId());
        if (session != null) closeSession(player, session.sessionId, reason);
    }

    public MysticWellCloseResult closeSession(Player player, UUID sessionId, MysticWellCloseReason reason) {
        if (sessionId == null) return MysticWellCloseResult.NOT_FOUND;
        UUID playerId = player == null ? null : player.getUniqueId();
        Session session = playerId == null ? findSession(sessionId) : sessions.get(playerId);
        if (session == null || !session.sessionId.equals(sessionId)) return MysticWellCloseResult.NOT_FOUND;
        MysticWellSessionState current = session.state.get();
        if (current == MysticWellSessionState.CLOSED || current == MysticWellSessionState.FAILED) {
            sessions.remove(session.owner, session);
            return MysticWellCloseResult.ALREADY_CLOSED;
        }
        if (!session.state.compareAndSet(MysticWellSessionState.OPEN, MysticWellSessionState.CLOSING)
                && !session.state.compareAndSet(MysticWellSessionState.PROCESSING, MysticWellSessionState.CLOSING)) {
            return session.state.get() == MysticWellSessionState.CLOSING ? MysticWellCloseResult.ALREADY_CLOSING : MysticWellCloseResult.ALREADY_CLOSED;
        }
        try {
            ItemStack input = takeSessionItem(session, ITEM_SLOT);
            ItemStack sacrifice = takeSessionItem(session, SACRIFICE_SLOT);
            returnOrStore(session.owner, player, input, sacrifice);
            session.state.set(MysticWellSessionState.CLOSED);
            return MysticWellCloseResult.CLOSED;
        } catch (Throwable failure) {
            session.state.set(MysticWellSessionState.FAILED);
            return MysticWellCloseResult.FAILED;
        } finally {
            sessions.remove(session.owner, session);
            transactions.remove(session.owner);
        }
    }

    private Session findSession(UUID sessionId) {
        for (Session session : sessions.values()) if (session.sessionId.equals(sessionId)) return session;
        return null;
    }

    private ItemStack takeSessionItem(Session session, int slot) {
        ItemStack item = clone(session.inventory.getItem(slot));
        session.inventory.setItem(slot, null);
        return item;
    }

    private void returnOrStore(UUID owner, Player player, ItemStack... items) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : items) if (!air(item)) remaining.add(item.clone());
        if (remaining.isEmpty()) return;
        if (player != null && player.isOnline()) {
            List<ItemStack> overflow = new ArrayList<>();
            for (ItemStack item : remaining) overflow.addAll(player.getInventory().addItem(item).values());
            player.updateInventory();
            if (!overflow.isEmpty()) persistRecovery(owner, overflow);
        } else persistRecovery(owner, remaining);
    }

    private void persistRecovery(UUID owner, List<ItemStack> items) {
        if (items.isEmpty()) return;
        List<ItemStack> combined = new ArrayList<>();
        String existing = plugin.getStorageManager().getOverride(RECOVERY_NAMESPACE, owner.toString()).orElse("");
        Collections.addAll(combined, ItemStackCodec.decode(existing));
        for (ItemStack item : items) if (!air(item)) combined.add(item.clone());
        plugin.getStorageManager().putOverride(RECOVERY_NAMESPACE, owner.toString(), ItemStackCodec.encode(combined.toArray(new ItemStack[0])));
    }

    private void recoverStoredItems(Player player) {
        String key = player.getUniqueId().toString();
        String encoded = plugin.getStorageManager().getOverride(RECOVERY_NAMESPACE, key).orElse("");
        if (encoded.isEmpty()) return;
        ItemStack[] stored = ItemStackCodec.decode(encoded);
        if (stored.length == 0) {
            plugin.getStorageManager().putOverride(RECOVERY_NAMESPACE, key, "");
            return;
        }
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : stored) {
            if (air(item)) continue;
            remaining.addAll(player.getInventory().addItem(item.clone()).values());
        }
        plugin.getStorageManager().putOverride(RECOVERY_NAMESPACE, key, remaining.isEmpty() ? "" : ItemStackCodec.encode(remaining.toArray(new ItemStack[0])));
        player.updateInventory();
    }

    private void upgrade(Player player, Session session) {
        UUID uuid = player.getUniqueId();
        if (!session.state.compareAndSet(MysticWellSessionState.OPEN, MysticWellSessionState.PROCESSING)) return;
        if (!transactions.add(uuid)) {
            session.state.compareAndSet(MysticWellSessionState.PROCESSING, MysticWellSessionState.OPEN);
            return;
        }
        if (!plugin.getPitGoldService().lock(uuid)) {
            transactions.remove(uuid);
            session.state.compareAndSet(MysticWellSessionState.PROCESSING, MysticWellSessionState.OPEN);
            return;
        }
        ItemStack original = clone(session.inventory.getItem(ITEM_SLOT));
        ItemStack sacrifice = clone(session.inventory.getItem(SACRIFICE_SLOT));
        int previousBalance = plugin.getPitGoldService().balance(uuid);
        boolean charged = false;
        boolean success = false;
        try {
            if (!player.isOnline() || !validMystic(original)) throw new IllegalStateException("invalid mystic item");
            if (!plugin.getRenownShopManager().owns(uuid, "mysticism")) throw new IllegalStateException("mysticism missing");
            String type = type(original);
            int current = tier(original);
            int next = current + 1;
            Cost cost = costs.get(type + ":" + next);
            if (cost == null) throw new IllegalStateException("maximum tier reached");
            if (!validSacrifice(type, cost.sacrifice, sacrifice)) throw new IllegalStateException("invalid sacrifice");
            if (!plugin.getPitGoldService().has(uuid, cost.gold)) throw new IllegalStateException("insufficient Pit Gold");
            ItemStack upgraded = applyUpgrade(original.clone(), type, next, uuid);
            if (upgraded == null) throw new IllegalStateException("no valid enchantment result");
            if (!plugin.getPitGoldService().take(player, cost.gold)) throw new IllegalStateException("charge failed");
            charged = true;
            session.inventory.setItem(ITEM_SLOT, upgraded);
            if (!cost.sacrifice.isEmpty()) session.inventory.setItem(SACRIFICE_SLOT, null);
            player.sendMessage(CC.color("&aMystic item upgraded to Tier " + roman(next) + " for ") + PitGoldFormatter.formatPitGold(cost.gold) + CC.color("&a."));
            if (plugin.getProgressManager() != null) plugin.getProgressManager().recordProgress(player,
                    ProgressManager.ObjectiveType.MYSTIC_ITEM_ENCHANTED, type, 1L,
                    new ProgressManager.ProgressContext(type, "mystic:" + uuid + ":" + System.nanoTime(), java.util.Map.of("tier", next)));
            success = true;
        } catch (Throwable failure) {
            session.inventory.setItem(ITEM_SLOT, original);
            session.inventory.setItem(SACRIFICE_SLOT, sacrifice);
            if (charged) plugin.getPitGoldService().set(player, previousBalance);
            player.sendMessage(CC.color(charged
                    ? "&cMystic upgrade failed. Your Gold was restored."
                    : "&cMystic upgrade failed: " + failure.getMessage() + "."));
        } finally {
            plugin.getPitGoldService().unlock(uuid);
            transactions.remove(uuid);
            if (success) {
                session.state.set(MysticWellSessionState.OPEN);
                Bukkit.getScheduler().runTask(plugin, player::closeInventory);
            } else {
                session.state.compareAndSet(MysticWellSessionState.PROCESSING, MysticWellSessionState.OPEN);
                if (sessions.get(uuid) == session) redraw(session);
            }
        }
    }

    private ItemStack applyUpgrade(ItemStack item, String type, int tier, UUID owner) {
        String[] pool = type.contains("bow") ? new String[]{"fletching","sprint_drain","telebow"}
                : type.contains("sword") ? new String[]{"sharp","gold_boost","lifesteal"}
                : new String[]{"damage_reduction","gold_bump","mirror"};
        String enchant = pool[Math.floorMod(Objects.hash(SecureItemTag.string(item, "MysticItemId"), tier), pool.length)];
        String existing = SecureItemTag.string(item, "mystic_enchants");
        String encoded = existing.isEmpty() ? enchant + ":1" : existing + "," + enchant + ":1";
        item = SecureItemTag.string(item, "mystic_item_type", type);
        item = SecureItemTag.integer(item, "mystic_tier", tier);
        item = SecureItemTag.string(item, "mystic_enchants", encoded);
        item = SecureItemTag.integer(item, "mystic_tokens", Math.min(4, tier));
        item = SecureItemTag.string(item, "mystic_owner", owner.toString());
        item = SecureItemTag.integer(item, "MysticTier", tier);
        item = SecureItemTag.string(item, "MysticOwner", owner.toString());
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> org.bukkit.ChatColor.stripColor(line).startsWith("Tier:") || org.bukkit.ChatColor.stripColor(line).startsWith("Mystic Enchants:"));
        lore.add(0, CC.color("&7Tier: &d" + roman(tier)));
        lore.add(CC.color("&7Mystic Enchants: &d" + encoded.replace('_', ' ')));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void redraw(Session session) {
        if (session.state.get() == MysticWellSessionState.CLOSED || session.state.get() == MysticWellSessionState.FAILED) return;
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < session.inventory.getSize(); i++) if (i != ITEM_SLOT && i != SACRIFICE_SLOT) session.inventory.setItem(i, filler);
        ItemStack item = session.inventory.getItem(ITEM_SLOT);
        String type = validMystic(item) ? type(item) : "";
        int next = validMystic(item) ? tier(item) + 1 : 0;
        Cost cost = costs.get(type + ":" + next);
        List<String> lore = new ArrayList<>();
        lore.add(validMystic(item) ? "&7Current tier: &d" + (next == 1 ? "Fresh" : roman(next - 1)) : "&cInsert a Mystic item");
        if (cost != null) lore.add("&7Upgrade cost: " + PitGoldFormatter.formatPitGold(cost.gold));
        if (cost != null && !cost.sacrifice.isEmpty()) lore.add("&7Sacrifice required: &e" + cost.sacrifice);
        session.inventory.setItem(UPGRADE_SLOT, named(cost == null ? Material.BARRIER : Material.ANVIL, (short) 0,
                cost == null ? "&cCannot Upgrade" : "&aUpgrade Mystic Item", lore));
        session.inventory.setItem(CLOSE_SLOT, named(Material.BARRIER, (short) 0, "&cClose", Collections.emptyList()));
    }

    private boolean sacrificeRequired(Session session) {
        ItemStack input = session.inventory.getItem(ITEM_SLOT);
        if (!validMystic(input)) return false;
        Cost cost = costs.get(type(input) + ":" + (tier(input) + 1));
        return cost != null && !cost.sacrifice.isEmpty();
    }

    private boolean validCurrentSacrifice(Session session, ItemStack candidate) {
        ItemStack input = session.inventory.getItem(ITEM_SLOT);
        if (!validMystic(input)) return false;
        String inputType = type(input);
        Cost cost = costs.get(inputType + ":" + (tier(input) + 1));
        return cost != null && validSacrifice(inputType, cost.sacrifice, candidate);
    }

    private boolean validMystic(ItemStack item) {
        if (air(item)) return false;
        String type = type(item);
        return costs.keySet().stream().anyMatch(key -> key.startsWith(type + ":"));
    }

    private String type(ItemStack item) {
        String tagged = SecureItemTag.string(item, "mystic_item_type");
        if (!tagged.isEmpty()) return tagged;
        String pitType = SecureItemTag.string(item, "pit_item_type");
        if (pitType.equalsIgnoreCase("MYSTIC_SWORD")) return "mystic_sword";
        if (pitType.equalsIgnoreCase("MYSTIC_BOW")) return "mystic_bow";
        if (pitType.equalsIgnoreCase("MYSTIC_PANTS")) {
            String pantsType = SecureItemTag.string(item, "mystic_pants_type");
            if (pantsType.equalsIgnoreCase("AQUA")) return "fresh_aqua_pants";
            if (pantsType.equalsIgnoreCase("DARK")) return "dark_pants";
            if (pantsType.equalsIgnoreCase("SEWER")) return "sewer_pants";
            if (pantsType.equalsIgnoreCase("RAGE")) return "rage_pants";
            return "fresh_pants";
        }
        return SecureItemTag.string(item, "ThePitItem");
    }

    private int tier(ItemStack item) {
        int value = SecureItemTag.getInteger(item, "mystic_tier", -1);
        return value >= 0 ? value : Math.max(0, SecureItemTag.getInteger(item, "MysticTier", 0));
    }

    private boolean validSacrifice(String type, String requirement, ItemStack sacrifice) {
        if (requirement.isEmpty()) return true;
        if (!validMystic(sacrifice)) return false;
        String other = type(sacrifice);
        if (requirement.equals("same")) return other.equals(type);
        return requirement.equals("standard_pants") && other.equals("fresh_pants");
    }

    private ItemStack named(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private String key(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private boolean air(ItemStack item) { return item == null || item.getType() == Material.AIR; }
    private ItemStack clone(ItemStack item) { return air(item) ? null : item.clone(); }
    private String roman(int tier) { return tier == 1 ? "I" : tier == 2 ? "II" : tier == 3 ? "III" : String.valueOf(tier); }

    public enum MysticWellSessionState {
        OPEN,
        PROCESSING,
        CLOSING,
        CLOSED,
        FAILED
    }

    public enum MysticWellCloseReason {
        INVENTORY_CLOSE,
        DISCONNECT,
        WORLD_CHANGE,
        DEATH,
        SERVER_DISABLE,
        STALE_SESSION
    }

    public enum MysticWellCloseResult {
        CLOSED,
        ALREADY_CLOSING,
        ALREADY_CLOSED,
        NOT_FOUND,
        FAILED
    }

    private record Cost(int gold, String sacrifice) {}

    private static final class Session {
        private final UUID sessionId;
        private final UUID owner;
        private final Inventory inventory;
        private final AtomicReference<MysticWellSessionState> state = new AtomicReference<>(MysticWellSessionState.OPEN);

        private Session(UUID sessionId, UUID owner, Inventory inventory) {
            this.sessionId = sessionId;
            this.owner = owner;
            this.inventory = inventory;
        }
    }

    public static final class MysticWellInventoryHolder implements InventoryHolder {
        private final UUID sessionId;
        private final UUID playerId;
        private Inventory inventory;

        private MysticWellInventoryHolder(UUID sessionId, UUID playerId) {
            this.sessionId = sessionId;
            this.playerId = playerId;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
