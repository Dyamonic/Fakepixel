package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.RegistryNode;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class RenownShopManager implements Listener {
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private final ThePitPlugin plugin;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> impatientActive = ConcurrentHashMap.newKeySet();

    public RenownShopManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int requestedPage) {
        if (!canOpen(player, true)) return;
        List<Definition> definitions = definitions();
        int[] contentSlots = contentSlots();
        int pages = Math.max(1, (definitions.size() + contentSlots.length - 1) / contentSlots.length);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Holder holder = new Holder(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(holder, size(), CC.color(text("title", "Renown Shop")));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(slot("status-slot", 4), status(player, page, pages));
        int start = page * contentSlots.length;
        for (int index = 0; index < contentSlots.length && start + index < definitions.size(); index++) {
            Definition definition = definitions.get(start + index);
            int inventorySlot = contentSlots[index];
            inventory.setItem(inventorySlot, icon(player, definition));
            holder.actions.put(inventorySlot, definition.id);
        }
        if (page > 0) inventory.setItem(slot("previous-page-slot", 45), item(
                material(text("items.previous-page.material", "ARROW"), Material.ARROW), (short) 0,
                text("items.previous-page.name", "&ePrevious Page"), strings("items.previous-page.lore", "&7Go to page %page%.", page)));
        if (page + 1 < pages) inventory.setItem(slot("next-page-slot", 53), item(
                material(text("items.next-page.material", "ARROW"), Material.ARROW), (short) 0,
                text("items.next-page.name", "&eNext Page"), strings("items.next-page.lore", "&7Go to page %page%.", page + 2)));
        inventory.setItem(slot("back-slot", 49), item(material(text("items.back.material", "ARROW"), Material.ARROW), (short) 0,
                text("items.back.name", "&aGo Back"), strings("items.back.lore", "&7Return to the Prestige menu.", 0)));
        player.openInventory(inventory);
    }

    public boolean canOpen(Player player, boolean message) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) {
            if (player != null && message) player.sendMessage(CC.color(text("messages.profile-loading", "&cYour profile is still loading.")));
            return false;
        }
        if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < 1) {
            if (message) player.sendMessage(CC.color(text("messages.prestige-required", "&cYou must reach Prestige I to use the Renown Shop.")));
            return false;
        }
        return true;
    }

    public int tier(UUID uuid, String id) {
        Definition definition = definition(id);
        if (definition == null) return 0;
        int current = 0;
        for (Tier tier : definition.tiers) {
            if (plugin.getStatsManager().hasRenownUpgrade(uuid, definition.token(tier.number))) current = tier.number;
            else break;
        }
        return current;
    }

    public boolean owns(UUID uuid, String id) {
        Definition definition = definition(id);
        return definition != null && tier(uuid, definition.id) >= definition.tiers.size();
    }

    public double effect(String id, String key, double fallback) {
        return plugin.registry().settings().getDouble("renown-shop.entries." + id + ".effects." + key, fallback);
    }

    public int effect(String id, String key, int fallback) {
        return plugin.registry().settings().getInt("renown-shop.entries." + id + ".effects." + key, fallback);
    }

    public int discountedPrice(UUID uuid, int basePrice) {
        int tier = tier(uuid, "scam-artist");
        double discount = Math.max(0.0D, Math.min(1.0D,
                effect("scam-artist", "discount-per-tier", 0.05D) * tier));
        return Math.max(1, (int) Math.floor(Math.max(0, basePrice) * (1.0D - discount)));
    }

    public boolean isToggleEnabled(UUID uuid, String id) {
        return toggleEnabled(uuid, id);
    }

    public Set<String> enabledUpgradeIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Definition definition : definitions()) {
            for (Tier tier : definition.tiers) ids.add(definition.token(tier.number));
        }
        return ids;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
        if (event.isShiftClick() || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK
                || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (event.getRawSlot() == slot("previous-page-slot", 45) && holder.page > 0) {
            open(player, holder.page - 1);
            return;
        }
        if (event.getRawSlot() == slot("next-page-slot", 53)) {
            open(player, holder.page + 1);
            return;
        }
        if (event.getRawSlot() == slot("back-slot", 49)) {
            plugin.getPrestigeManager().open(player);
            return;
        }
        String id = holder.actions.get(event.getRawSlot());
        if (id != null && event.isRightClick() && toggleable(id) && owns(player.getUniqueId(), id)) {
            toggle(player, id);
            open(player, holder.page);
        } else if (id != null) purchase(player, id, holder.page);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        processing.remove(event.getPlayer().getUniqueId());
        impatientActive.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean active = owns(uuid, "impatient") && toggleEnabled(uuid, "impatient")
                && plugin.getArenaManager().isInCurrentSpawnProtection(event.getTo());
        if (active) {
            int level = Math.max(1, effect("impatient", "speed-level", 2)) - 1;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, level), true);
            impatientActive.add(uuid);
        } else if (impatientActive.remove(uuid)) {
            PotionEffect speed = player.getActivePotionEffects().stream()
                    .filter(value -> value.getType().equals(PotionEffectType.SPEED)).findFirst().orElse(null);
            if (speed != null && speed.getAmplifier() == Math.max(1, effect("impatient", "speed-level", 2)) - 1
                    && speed.getDuration() <= 40) player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!owns(player.getUniqueId(), "yummy")) return;
        ItemStack consumed = event.getItem();
        if (consumed == null || consumed.getType() != Material.GOLDEN_APPLE
                && !SecureItemTag.is(consumed, "golden_head")) return;
        int gold = Math.max(0, effect("yummy", "gold", 6));
        if (gold > 0) plugin.getPitGoldService().award(player, gold, ThePitPlugin.GoldSource.EVENT);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        int tier = tier(player.getUniqueId(), "divine-intervention");
        if (tier <= 0) return;
        double chance = Math.max(0.0D, Math.min(1.0D,
                effect("divine-intervention", "chance-per-tier", 0.05D) * tier));
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        player.sendMessage(CC.color(text("messages.divine-intervention", "&eDivine Intervention protected your inventory!")));
    }

    private void purchase(Player player, String id, int page) {
        if (!canOpen(player, true)) return;
        UUID uuid = player.getUniqueId();
        if (plugin.getPrestigeManager().isProcessing(uuid) || !processing.add(uuid)) {
            player.sendMessage(CC.color(text("messages.processing", "&eYour Renown purchase is already being processed.")));
            return;
        }
        Definition definition = definition(id);
        if (definition == null) {
            processing.remove(uuid);
            return;
        }
        int current = tier(uuid, id);
        if (current >= definition.tiers.size()) {
            processing.remove(uuid);
            player.sendMessage(CC.color(text("messages.already-purchased", "&aYou already purchased this upgrade.")));
            return;
        }
        Tier next = definition.tiers.get(current);
        int requiredPrestige = Math.max(definition.requiredPrestige, next.requiredPrestige);
        String previousToken = current == 0 ? "" : definition.token(current);
        String nextToken = definition.token(next.number);
        String playerName = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StatsManager.RenownPurchaseResult result = plugin.getStatsManager().persistRenownPurchase(
                    uuid, playerName, requiredPrestige, next.price, previousToken, nextToken);
            Bukkit.getScheduler().runTask(plugin, () -> finishPurchase(player, definition, next, result, page));
        });
    }

    private void finishPurchase(Player player, Definition definition, Tier tier,
                                StatsManager.RenownPurchaseResult result, int page) {
        UUID uuid = player.getUniqueId();
        try {
            plugin.getStatsManager().synchronizeRenownPurchase(uuid, result);
            if (result.status() == StatsManager.RenownPurchaseStatus.SUCCESS) {
                plugin.getStatsManager().applyRenownPurchase(uuid, result);
                applyUnlock(player, definition);
                player.sendMessage(CC.color(text("messages.purchase-success", "&aPurchased %upgrade%&a tier %tier% for &e%cost% Renown&a.")
                        .replace("%upgrade%", definition.name).replace("%tier%", String.valueOf(tier.number))
                        .replace("%cost%", format(tier.price))));
                sound(player, "sounds.purchase-success", "LEVEL_UP");
            } else {
                player.sendMessage(CC.color(failure(result.status())));
                sound(player, "sounds.purchase-failure", "VILLAGER_NO");
            }
        } finally {
            processing.remove(uuid);
            if (player.isOnline()) open(player, page);
        }
    }

    private void applyUnlock(Player player, Definition definition) {
        if (player == null || !player.isOnline()) return;
        if (definition.type.equals("PERK_UNLOCK") || definition.id.equals("extra-perk-slot") || definition.id.equals("the-way")) {
            plugin.getPerkManager().revalidate(player);
        }
        if (definition.id.equals("extra-hearts") || definition.id.equals("thick")) plugin.getPerkManager().applyHealth(player);
        plugin.getBuildScoreboardManager().update(player);
    }

    private ItemStack icon(Player player, Definition definition) {
        int current = tier(player.getUniqueId(), definition.id);
        boolean purchased = current >= definition.tiers.size();
        Tier next = purchased ? definition.tiers.get(definition.tiers.size() - 1) : definition.tiers.get(current);
        int requiredPrestige = Math.max(definition.requiredPrestige, next.requiredPrestige);
        List<String> lore = new ArrayList<>();
        lore.addAll(definition.description);
        lore.add("");
        lore.add("&7Type: &f" + readable(definition.type));
        lore.add("&7Current tier: &e" + current + "&7/&e" + definition.tiers.size());
        lore.add("&7Required prestige: &ePrestige " + roman(requiredPrestige));
        lore.add("&7Your Renown: &e" + format(plugin.getStatsManager().getRenown(player.getUniqueId())));
        if (toggleable(definition.id) && purchased) {
            lore.add("&7Enabled: " + (toggleEnabled(player.getUniqueId(), definition.id) ? "&aYes" : "&cNo"));
            lore.add("&eRight-click to toggle!");
        }
        lore.add("");
        if (purchased) {
            lore.add("&aPurchased!");
        } else if (definition.tiers.size() > 1) {
            lore.add("&7Tier: &e" + current + "&7/&e" + definition.tiers.size());
            lore.add("&7Next tier cost: &e" + format(next.price) + " Renown");
            if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < requiredPrestige) {
                lore.add("&7Requires: &ePrestige " + roman(requiredPrestige));
                lore.add("&cPrestige requirement not met!");
            } else if (plugin.getStatsManager().getRenown(player.getUniqueId()) < next.price) {
                lore.add("&cNot enough Renown!");
            } else {
                lore.add("&eClick to purchase!");
            }
        } else {
            lore.add("&7Cost: &e" + format(next.price) + " Renown");
            if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < requiredPrestige) {
                lore.add("&7Requires: &ePrestige " + roman(requiredPrestige));
                lore.add("&cPrestige requirement not met!");
            } else if (plugin.getStatsManager().getRenown(player.getUniqueId()) < next.price) {
                lore.add("&cNot enough Renown!");
            } else {
                lore.add("&eClick to purchase!");
            }
        }
        return item(definition.material, definition.data, definition.name, lore);
    }

    private ItemStack status(Player player, int page, int pages) {
        return item(material(text("items.status.material", "DIAMOND"), Material.DIAMOND), (short) 0,
                text("items.status.name", "&e&lRenown Shop"), strings("items.status.lore",
                        "&7Your Renown: &e%renown%|&7Prestige: &e%prestige%|&7Page: &f%page%&7/&f%pages%",
                        page + 1, pages, player));
    }

    private List<Definition> definitions() {
        List<Definition> definitions = new ArrayList<>();
        for (String id : plugin.registry().settings().keys("renown-shop.entries")) {
            RegistryNode node = plugin.registry().settings().getConfigurationSection("renown-shop.entries." + id);
            if (node == null || !node.getBoolean("enabled", true)) continue;
            String type = node.getString("type", "PERMANENT_UPGRADE").toUpperCase(Locale.ENGLISH);
            String name = node.getString("name", readable(id));
            Material material = material(node.getString("material", "NETHER_STAR"), Material.NETHER_STAR);
            short data = (short) node.getInt("data", 0);
            int prestige = Math.max(1, node.getInt("required-prestige", 1));
            String purchaseId = node.getString("purchase-id", id);
            List<String> description = new ArrayList<>(node.getStringList("description"));
            List<Tier> tiers = new ArrayList<>();
            RegistryNode tierSection = node.getConfigurationSection("tiers");
            if (tierSection != null) {
                for (String rawTier : tierSection.keys()) {
                    RegistryNode tier = tierSection.getConfigurationSection(rawTier);
                    if (tier == null) continue;
                    try {
                        int number = Integer.parseInt(rawTier);
                        tiers.add(new Tier(number, Math.max(0L, tier.getLong("cost", 0L)),
                                Math.max(prestige, tier.getInt("required-prestige", prestige))));
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (tiers.isEmpty()) tiers.add(new Tier(1, Math.max(0L, node.getLong("cost", 0L)), prestige));
            tiers.sort(Comparator.comparingInt(value -> value.number));
            definitions.add(new Definition(id, purchaseId, name, type, material, data, prestige, description, tiers));
        }
        definitions.sort(Comparator.comparingInt((Definition value) -> value.requiredPrestige));
        return definitions;
    }

    private Definition definition(String id) {
        for (Definition definition : definitions()) if (definition.id.equals(id)) return definition;
        return null;
    }

    private String failure(StatsManager.RenownPurchaseStatus status) {
        if (status == StatsManager.RenownPurchaseStatus.PRESTIGE_LOCKED) return text("messages.prestige-locked", "&cPrestige requirement not met!");
        if (status == StatsManager.RenownPurchaseStatus.INSUFFICIENT_RENOWN) return text("messages.insufficient-renown", "&cNot enough Renown!");
        if (status == StatsManager.RenownPurchaseStatus.ALREADY_PURCHASED) return text("messages.already-purchased", "&aPurchased!");
        if (status == StatsManager.RenownPurchaseStatus.TIER_MISMATCH) return text("messages.tier-mismatch", "&cTiers must be purchased in order.");
        if (status == StatsManager.RenownPurchaseStatus.PROFILE_NOT_LOADED) return text("messages.profile-loading", "&cYour profile is still loading.");
        if (status == StatsManager.RenownPurchaseStatus.CONFLICT) return text("messages.conflict", "&cYour profile changed on another server. Please try again.");
        return text("messages.purchase-failed", "&cThe Renown purchase failed. Nothing was changed.");
    }

    private boolean toggleable(String id) {
        return id.equals("autobuy") || id.equals("impatient") || id.equals("apollo");
    }

    private boolean toggleEnabled(UUID uuid, String id) {
        if (id.equals("autobuy")) return plugin.getShopManager().isAutobuyEnabled(uuid);
        return plugin.getStatsManager().renownToggleEnabled(uuid, id,
                plugin.registry().settings().getBoolean("renown-shop.toggles." + id + "-default", true));
    }

    private void toggle(Player player, String id) {
        boolean enabled = !toggleEnabled(player.getUniqueId(), id);
        if (id.equals("autobuy")) plugin.getShopManager().setAutobuyEnabled(player, enabled);
        else plugin.getStatsManager().setRenownToggle(player, id, enabled);
        player.sendMessage(CC.color("&e" + readable(id) + " " + (enabled ? "&aenabled" : "&cdisabled") + "&e."));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(material(text("items.filler.material", "STAINED_GLASS_PANE"), Material.STAINED_GLASS_PANE),
                (short) plugin.registry().settings().getInt("renown-shop.items.filler.data", 15), " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> strings(String path, String fallback, int page) {
        return strings(path, fallback, page, 0, null);
    }

    private List<String> strings(String path, String fallback, int page, int pages, Player player) {
        List<String> configured = plugin.registry().settings().getStringList("renown-shop." + path);
        List<String> source = configured.isEmpty() ? java.util.Arrays.asList(fallback.split("\\|", -1)) : configured;
        List<String> output = new ArrayList<>();
        for (String line : source) output.add(line
                .replace("%page%", String.valueOf(page))
                .replace("%pages%", String.valueOf(pages))
                .replace("%renown%", player == null ? "0" : format(plugin.getStatsManager().getRenown(player.getUniqueId())))
                .replace("%prestige%", player == null ? "0" : roman(plugin.getStatsManager().getPrestige(player.getUniqueId()))));
        return output;
    }

    private String text(String path, String fallback) {
        return plugin.registry().settings().getString("renown-shop." + path, fallback);
    }

    private int slot(String path, int fallback) {
        int value = plugin.registry().settings().getInt("renown-shop.menu." + path, fallback);
        return Math.max(0, Math.min(size() - 1, value));
    }

    private int size() {
        int configured = plugin.registry().settings().getInt("renown-shop.size", 54);
        int bounded = Math.max(9, Math.min(54, configured));
        return bounded - bounded % 9;
    }

    private int[] contentSlots() {
        List<Integer> configured = plugin.registry().settings().getIntegerList("renown-shop.menu.content-slots");
        if (configured.isEmpty()) return CONTENT_SLOTS;
        int[] slots = configured.stream().filter(value -> value >= 0 && value < size()).distinct().mapToInt(Integer::intValue).toArray();
        return slots.length == 0 ? CONTENT_SLOTS : slots;
    }

    private Material material(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase(Locale.ENGLISH)); }
        catch (Exception ignored) { return fallback; }
    }

    private void sound(Player player, String path, String fallback) {
        String configured = plugin.registry().settings().getString("renown-shop." + path, fallback);
        try { player.playSound(player.getLocation(), Sound.valueOf(configured.toUpperCase(Locale.ENGLISH)), 1.0F, 1.0F); }
        catch (Exception ignored) { }
    }

    private String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private String readable(String value) {
        StringBuilder output = new StringBuilder();
        for (String word : value.toLowerCase(Locale.ENGLISH).replace('_', '-').split("-")) {
            if (word.isEmpty()) continue;
            if (output.length() > 0) output.append(' ');
            output.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return output.toString();
    }

    private String roman(int value) {
        if (value <= 0) return "0";
        int[] values = {50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            while (value >= values[index]) {
                output.append(symbols[index]);
                value -= values[index];
            }
        }
        return output.toString();
    }

    private static final class Definition {
        private final String id;
        private final String purchaseId;
        private final String name;
        private final String type;
        private final Material material;
        private final short data;
        private final int requiredPrestige;
        private final List<String> description;
        private final List<Tier> tiers;

        private Definition(String id, String purchaseId, String name, String type, Material material, short data,
                           int requiredPrestige, List<String> description, List<Tier> tiers) {
            this.id = id;
            this.purchaseId = purchaseId;
            this.name = name;
            this.type = type;
            this.material = material;
            this.data = data;
            this.requiredPrestige = requiredPrestige;
            this.description = description;
            this.tiers = tiers;
        }

        private String token(int tier) {
            return tiers.size() == 1 ? purchaseId : purchaseId + "-" + tier;
        }
    }

    private record Tier(int number, long price, int requiredPrestige) {}

    private static final class Holder implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        private Holder(UUID owner, int page) {
            this.owner = owner;
            this.page = page;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
