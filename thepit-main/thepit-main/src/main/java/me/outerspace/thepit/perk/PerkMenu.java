package me.outerspace.thepit.perk;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.PerkRegistry;
import me.outerspace.thepit.registry.definition.PerkDefinition;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PerkMenu implements Listener {
    private static final List<Integer> DEFAULT_CHOOSE_SLOTS = Collections.unmodifiableList(Arrays.asList(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    ));
    private final ThePitPlugin plugin;
    private final PerkManager manager;

    public PerkMenu(ThePitPlugin plugin, PerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player) {
        manager.revalidate(player);
        MenuSession session = new MenuSession(player.getUniqueId(), MenuType.MAIN, 0, null);
        Inventory inventory = Bukkit.createInventory(session, menuSize(), mainTitle());
        session.inventory(inventory);
        fill(inventory);
        java.util.Set<Integer> usedLoadoutPositions = new java.util.HashSet<>();
        for (int loadoutSlot = 1; loadoutSlot <= 4; loadoutSlot++) {
            int inventorySlot = safeLoadoutPosition(loadoutSlot, usedLoadoutPositions);
            if (inventorySlot < 0 || inventorySlot >= inventory.getSize()) continue;
            usedLoadoutPositions.add(inventorySlot);
            session.loadout(inventorySlot, loadoutSlot);
            inventory.setItem(inventorySlot, loadoutIcon(player, loadoutSlot));
        }
        if (plugin.getPassiveManager().enabled()) {
            for (PassiveDefinition definition : plugin.getPassiveManager().definitions()) {
                int position = definition.inventorySlot();
                if (position < 0 || position >= inventory.getSize() || session.loadout(position) != null) continue;
                session.passive(position, definition.id());
                inventory.setItem(position, passiveIcon(player, definition));
            }
        }
        int killstreaks = registry().getInt("menu.killstreaks.slot", 45);
        if (killstreaks >= 0 && killstreaks < inventory.getSize() && session.loadout(killstreaks) == null && session.passive(killstreaks) == null) {
            session.special(killstreaks, "killstreaks");
            inventory.setItem(killstreaks, icon(material("menu.killstreaks.material", Material.GOLD_SWORD),
                    data("menu.killstreaks.data", 0), text("menu.killstreaks.name", "&6Killstreaks"), strings("menu.killstreaks.lore")));
        }
        int back = backSlot();
        if (back >= 0 && back < inventory.getSize() && session.loadout(back) == null && session.passive(back) == null) {
            inventory.setItem(back, icon(material("menu.back.material", Material.ARROW), data("menu.back.data", 0),
                    text("menu.back.name", "&aGo Back"), strings("menu.back.lore")));
        }
        player.openInventory(inventory);
    }

    public void refreshIfOpen(Player player) {
        if (player == null || !player.isOnline()) return;
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof MenuSession session) || !session.owner().equals(player.getUniqueId())) return;
        if (session.type() == MenuType.CHOOSE) openChoose(player, session.selectedSlot());
        else open(player);
    }

    private void openChoose(Player player, int loadoutSlot) {
        manager.revalidate(player);
        if (!manager.profile(player.getUniqueId()).isSlotUnlocked(loadoutSlot)) {
            lockedSlot(player, loadoutSlot);
            return;
        }
        MenuSession session = new MenuSession(player.getUniqueId(), MenuType.CHOOSE, loadoutSlot, null);
        Inventory inventory = Bukkit.createInventory(session, chooseSize(), chooseTitle());
        session.inventory(inventory);
        fill(inventory);
        List<Integer> positions = chooseSlots();
        int index = 0;
        for (PerkDefinition definition : manager.definitionOrder()) {
            if (!definition.enabled() || index >= positions.size()) continue;
            int position = positions.get(index++);
            if (position < 0 || position >= inventory.getSize() || position == backSlot()) continue;
            session.perk(position, definition.id());
            inventory.setItem(position, chooseIcon(player, definition));
        }
        int back = backSlot();
        if (back >= 0 && back < inventory.getSize()) {
            inventory.setItem(back, icon(material("menu.back.material", Material.ARROW), data("menu.back.data", 0),
                    text("menu.back.name", "&aGo Back"), strings("menu.back.lore")));
        }
        player.openInventory(inventory);
    }

    private ItemStack loadoutIcon(Player player, int slot) {
        PerkProfile profile = manager.profile(player.getUniqueId());
        if (!profile.isSlotUnlocked(slot)) {
            String prefix = manager.requiredLevel(slot);
            List<String> lore = new ArrayList<>();
            lore.add("&7Unlock this slot by reaching:");
            lore.add(prefix);
            lore.add("");
            lore.add("&cSlot locked!");
            return icon(Material.STAINED_GLASS_PANE, (short) 14, "&cLocked Perk Slot #" + slot, lore);
        }
        String equipped = profile.equipped(slot);
        PerkDefinition definition = equipped == null ? null : manager.definition(equipped).orElse(null);
        if (definition == null) {
            return icon(Material.STAINED_GLASS_PANE, (short) 5, "&aPerk Slot #" + slot,
                    Arrays.asList("&7No perk selected.", "", "&eClick to choose a perk!"));
        }
        List<String> lore = new ArrayList<>(definition.lore());
        lore.add("");
        lore.add("&aSelected in Slot #" + slot);
        lore.add("&eClick to change this perk!");
        return icon(materialName(definition.material(), Material.PAPER), definition.data(), definition.displayName(), lore);
    }

    private ItemStack chooseIcon(Player player, PerkDefinition definition) {
        PerkProfile profile = manager.profile(player.getUniqueId());
        boolean purchased = profile.isUnlocked(definition.id());
        int equippedSlot = manager.equippedSlot(profile, definition.id());
        boolean available = manager.meets(player, definition);
        List<String> lore = new ArrayList<>(definition.lore());
        lore.add("");
        lore.add(text("menu.labels.required-level", "&7Required Level: %level%")
                .replace("%level%", plugin.getStatsManager().getLevelColorFor(definition.requiredLevel()) + "[" + definition.requiredLevel() + "]"));
        if (definition.requiredPrestige() > 0) {
            lore.add(text("menu.labels.required-prestige", "&7Required Prestige: &d%prestige%")
                    .replace("%prestige%", String.valueOf(definition.requiredPrestige())));
        }
        lore.add(text("menu.labels.purchased", "&7Purchased: %status%")
                .replace("%status%", purchased ? text("menu.labels.yes", "&aYes") : text("menu.labels.no", "&cNo")));
        if (equippedSlot > 0) {
            lore.add(text("menu.labels.equipped", "&aCurrently equipped in Slot #%slot%")
                    .replace("%slot%", String.valueOf(equippedSlot)));
        } else {
            lore.add(text("menu.labels.not-equipped", "&7Equipped: &cNo"));
        }
        lore.add("");
        if (!purchased) {
            lore.add(text("menu.labels.cost", "&6Cost: %price% Gold")
                    .replace("%price%", String.valueOf(definition.coinPrice())));
        }
        if (!available) {
            if (definition.requiredPrestige() > plugin.getStatsManager().getPrestige(player.getUniqueId())) {
                lore.add(text("menu.labels.prestige-locked", "&cRequires Prestige %prestige%.")
                        .replace("%prestige%", String.valueOf(definition.requiredPrestige())));
            }
            lore.add(text("menu.labels.perk-locked", "&cPerk locked!"));
        } else if (purchased) {
            lore.add(text("menu.labels.select", "&eClick to select!"));
        } else {
            lore.add(text("menu.labels.purchase", "&eClick to purchase!"));
        }
        return icon(materialName(definition.material(), Material.PAPER), definition.data(), definition.displayName(), lore);
    }

    private ItemStack passiveIcon(Player player, PassiveDefinition definition) {
        int current = plugin.getPassiveManager().tier(player.getUniqueId(), definition.id());
        List<String> lore = new ArrayList<>();
        lore.add("&7Tier: &e" + plugin.getPassiveManager().roman(current));
        lore.add("");
        lore.add(effect(definition, current, false));
        if (current >= 6) {
            lore.add("");
            lore.add("&aMaximum tier reached!");
            return icon(definition.material(), definition.data(), definition.displayName(), lore);
        }
        PassiveDefinition.Tier next = definition.tier(current + 1);
        lore.add("");
        lore.add(effect(definition, current + 1, true));
        lore.add("&7Cost: &6" + String.format(java.util.Locale.US, "%,d", next.price()) + " Gold");
        lore.add("&7Requires: &bLevel " + next.requiredLevel());
        if (!next.requiredRenownUpgrade().isEmpty()) lore.add("&7Requires: &dOne Upper");
        lore.add("");
        if (plugin.getStatsManager().getLevel(player.getUniqueId()) < next.requiredLevel()) {
            lore.add("&cRequires Level " + next.requiredLevel());
        } else if (!next.requiredRenownUpgrade().isEmpty()
                && !plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), next.requiredRenownUpgrade())) {
            lore.add("&cRequires One Upper");
            lore.add("&7Unlocked from the Renown Shop");
            lore.add("&7at Prestige XIV.");
        } else if (plugin.getPassiveManager().isBusy(player.getUniqueId())) {
            lore.add("&ePurchase in progress...");
        } else {
            int gold = plugin.getPitGoldService().balance(player.getUniqueId());
            lore.add(gold >= next.price() ? "&eClick to upgrade!" : "&cNot enough Gold!");
        }
        return icon(definition.material(), definition.data(), definition.displayName(), lore);
    }

    private String effect(PassiveDefinition definition, int tier, boolean next) {
        String prefix = next ? "&7Next tier: " : "&7";
        if (definition.id().equals("xp-boost")) return prefix + "&a+" + percent(definition.valuePerTier() * tier) + "% XP &7from most sources.";
        if (definition.id().equals("gold-boost")) return prefix + "&a+" + percent(definition.valuePerTier() * tier) + "% Gold &7from eligible sources.";
        if (definition.id().equals("melee-damage")) return prefix + "&a+" + percent(definition.valuePerTier() * tier) + "% melee damage";
        if (definition.id().equals("bow-damage")) return prefix + "&a+" + percent(definition.valuePerTier() * tier) + "% bow damage";
        if (definition.id().equals("damage-reduction")) return prefix + "&a-" + percent(definition.valuePerTier() * tier) + "% bow damage taken";
        if (definition.id().equals("build-battler")) return prefix + "&a+" + percent(definition.valuePerTier() * tier) + "% block lifetime";
        return prefix + "&a+" + definition.bonusGold() + " Gold &7and &b+" + definition.bonusXp() + " XP &7for " + tier + " kill" + (tier == 1 ? "" : "s") + " each life.";
    }

    private int percent(double value) {
        return (int) Math.round(value * 100.0D);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuSession session)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!session.owner().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK
                || event.isShiftClick() || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) {
            player.updateInventory();
            return;
        }
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        if (session.type() == MenuType.MAIN) {
            Integer loadoutSlot = session.loadout(event.getRawSlot());
            if (loadoutSlot == null) {
                String special = session.special(event.getRawSlot());
                if ("killstreaks".equals(special)) {
                    plugin.getKillstreakManager().openKillstreaks(player);
                    return;
                }
                String passive = session.passive(event.getRawSlot());
                if (passive == null) {
                    if (event.getRawSlot() == backSlot()) player.closeInventory();
                    return;
                }
                PassiveManager.PurchaseResult result = plugin.getPassiveManager().purchase(player, passive);
                if (result != PassiveManager.PurchaseResult.STARTED) sendPassiveResult(player, result, passive);
                return;
            }
            manager.revalidate(player);
            if (!manager.profile(player.getUniqueId()).isSlotUnlocked(loadoutSlot)) {
                lockedSlot(player, loadoutSlot);
                return;
            }
            openChoose(player, loadoutSlot);
            return;
        }
        if (session.type() != MenuType.CHOOSE) return;
        if (event.getRawSlot() == backSlot()) {
            open(player);
            return;
        }
        String perkId = session.perk(event.getRawSlot());
        if (perkId == null) return;
        manager.revalidate(player);
        PerkDefinition definition = manager.definition(perkId).orElse(null);
        if (definition == null || !definition.enabled()) {
            failure(player);
            openChoose(player, session.selectedSlot());
            return;
        }
        if (!manager.meets(player, definition)) {
            failure(player);
            player.sendMessage(CC.color(text("menu.messages.perk-locked", "&cThis perk is locked.")));
            return;
        }
        PerkProfile profile = manager.profile(player.getUniqueId());
        PerkManager.Result result = profile.isUnlocked(perkId)
                ? manager.equip(player, session.selectedSlot(), perkId)
                : manager.purchaseAndEquip(player, session.selectedSlot(), perkId);
        sendResult(player, result);
        if (result == PerkManager.Result.SUCCESS) open(player);
        else openChoose(player, session.selectedSlot());
    }

    private void sendPassiveResult(Player player, PassiveManager.PurchaseResult result, String id) {
        PassiveDefinition definition = plugin.getPassiveManager().definition(id);
        int current = plugin.getPassiveManager().tier(player.getUniqueId(), id);
        PassiveDefinition.Tier next = definition == null || current >= 6 ? null : definition.tier(current + 1);
        String message = switch (result) {
            case MAXIMUM -> "&aMaximum tier reached!";
            case LEVEL -> "&cRequires Level " + (next == null ? "?" : next.requiredLevel());
            case ONE_UPPER -> "&cRequires One Upper";
            case INSUFFICIENT_GOLD -> "&cYou do not have enough Pit Gold.";
            case BUSY -> "&eA passive purchase is already in progress.";
            case DISABLED -> "&cPassive upgrades are disabled.";
            default -> "&cThat passive upgrade is unavailable.";
        };
        failure(player);
        player.sendMessage(CC.color(message));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuSession) event.setCancelled(true);
    }

    private void lockedSlot(Player player, int slot) {
        String prefix = manager.requiredLevel(slot);
        failure(player);
        player.sendMessage(CC.color(text("menu.messages.slot-locked", "&cReach %required_level% &cto unlock this perk slot.")
                .replace("%required_level%", prefix)));
    }

    private void sendResult(Player player, PerkManager.Result result) {
        String path;
        String fallback;
        switch (result) {
            case SUCCESS -> {
                path = "menu.messages.success";
                fallback = "&aYour perk loadout was updated.";
                success(player);
            }
            case LOCKED -> {
                path = "menu.messages.perk-locked";
                fallback = "&cThis perk is locked.";
                failure(player);
            }
            case NOT_UNLOCKED -> {
                path = "menu.messages.not-purchased";
                fallback = "&cPurchase that perk before equipping it.";
                failure(player);
            }
            case SLOT_LOCKED -> {
                path = "menu.messages.slot-unavailable";
                fallback = "&cThat perk slot is locked.";
                failure(player);
            }
            case INSUFFICIENT_COINS -> {
                path = "menu.messages.insufficient-coins";
                fallback = "&cYou do not have enough Gold.";
                failure(player);
            }
            case STORAGE_UNAVAILABLE -> {
                path = "menu.messages.storage-unavailable";
                fallback = "&cPerk storage is unavailable. Nothing was changed.";
                failure(player);
            }
            default -> {
                path = "menu.messages.not-found";
                fallback = "&cThat perk is unavailable.";
                failure(player);
            }
        }
        player.sendMessage(CC.color(text(path, fallback)));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(material("menu.filler.material", Material.STAINED_GLASS_PANE),
                data("menu.filler.data", 15), text("menu.filler.name", " "), Collections.<String>emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack icon(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private void success(Player player) {
        play(player, text("menu.sounds.success", "ORB_PICKUP"), 1.0F);
    }

    private void failure(Player player) {
        play(player, text("menu.sounds.failure", "NOTE_BASS"), 0.8F);
    }

    private void play(Player player, String soundName, float pitch) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0F, pitch);
        } catch (Throwable ignored) {
        }
    }

    private int menuSize() {
        return size(registry().getInt("menu.size", 54));
    }

    private int chooseSize() {
        return size(registry().getInt("menu.choose-size", 54));
    }

    private int size(int configured) {
        int bounded = Math.max(9, Math.min(54, configured));
        return bounded - bounded % 9;
    }

    private int slotInventoryPosition(int slot) {
        return registry().getInt("loadout-slots." + slot + ".inventory-slot", defaultLoadoutPosition(slot));
    }

    private int safeLoadoutPosition(int slot, java.util.Set<Integer> used) {
        int configured = slotInventoryPosition(slot);
        java.util.Set<Integer> passiveSlots = new java.util.HashSet<>();
        if (plugin.getPassiveManager() != null) {
            for (PassiveDefinition definition : plugin.getPassiveManager().definitions()) passiveSlots.add(definition.inventorySlot());
        }
        if (configured >= 0 && configured < menuSize() && !used.contains(configured) && !passiveSlots.contains(configured) && configured != backSlot()) return configured;
        for (int candidate : new int[]{11, 13, 15, 17, 10, 12, 14, 16}) {
            if (candidate < menuSize() && !used.contains(candidate) && !passiveSlots.contains(candidate) && candidate != backSlot()) return candidate;
        }
        return -1;
    }

    private int defaultLoadoutPosition(int slot) {
        int[] positions = {20, 22, 24, 31};
        return slot >= 1 && slot <= positions.length ? positions[slot - 1] : -1;
    }

    private List<Integer> chooseSlots() {
        List<Integer> configured = registry().getIntegerList("menu.choose-slots");
        return configured.isEmpty() ? DEFAULT_CHOOSE_SLOTS : configured;
    }

    private int backSlot() {
        return registry().getInt("menu.back.slot", 49);
    }

    private String mainTitle() {
        return CC.color(text("menu.title", "&8Perks"));
    }

    private String chooseTitle() {
        return CC.color(text("menu.choose-title", "Choose a perk"));
    }

    private String text(String path, String fallback) {
        return registry().getString(path, fallback);
    }

    private List<String> strings(String path) {
        return registry().getStringList(path);
    }

    private short data(String path, int fallback) {
        return (short) registry().getInt(path, fallback);
    }

    private Material material(String path, Material fallback) {
        return materialName(registry().getString(path, fallback.name()), fallback);
    }

    private Material materialName(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    private PerkRegistry registry() {
        return plugin.registry().perks();
    }

    private enum MenuType {
        MAIN,
        CHOOSE
    }

    private static final class MenuSession implements InventoryHolder {
        private final UUID owner;
        private final MenuType type;
        private final int selectedSlot;
        private final String perkId;
        private final Map<Integer, Integer> loadoutSlots = new LinkedHashMap<>();
        private final Map<Integer, String> perks = new LinkedHashMap<>();
        private final Map<Integer, String> passives = new LinkedHashMap<>();
        private final Map<Integer, String> specials = new LinkedHashMap<>();
        private Inventory inventory;

        private MenuSession(UUID owner, MenuType type, int selectedSlot, String perkId) {
            this.owner = owner;
            this.type = type;
            this.selectedSlot = selectedSlot;
            this.perkId = perkId;
        }

        public Inventory getInventory() {
            return inventory;
        }

        private void inventory(Inventory inventory) {
            this.inventory = inventory;
        }

        private UUID owner() {
            return owner;
        }

        private MenuType type() {
            return type;
        }

        private int selectedSlot() {
            return selectedSlot;
        }

        private String perkId() {
            return perkId;
        }

        private void loadout(int inventorySlot, int loadoutSlot) {
            loadoutSlots.put(inventorySlot, loadoutSlot);
        }

        private Integer loadout(int inventorySlot) {
            return loadoutSlots.get(inventorySlot);
        }

        private void perk(int inventorySlot, String id) {
            perks.put(inventorySlot, id);
        }

        private String perk(int inventorySlot) {
            return perks.get(inventorySlot);
        }

        private void passive(int inventorySlot, String id) {
            passives.put(inventorySlot, id);
        }

        private String passive(int inventorySlot) {
            return passives.get(inventorySlot);
        }

        private void special(int inventorySlot, String id) {
            specials.put(inventorySlot, id);
        }

        private String special(int inventorySlot) {
            return specials.get(inventorySlot);
        }
    }
}
