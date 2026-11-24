package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.CollectionManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.SkillManager;
import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.managers.FishingMinigameManager;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatService;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.registries.LavaFishingRegistry;
import club.abelix.games.fakevival.registries.definitions.LavaFishingLootDefinition;
import club.abelix.games.fakevival.registries.definitions.LavaFishingRewardDefinition;
import club.abelix.games.fakevival.config.defaults.CustomFishDefaults80;
import club.abelix.games.fakevival.util.CC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class FishingGearListener implements Listener {
    private static final int MAX_SEA_CREATURES_PER_PLAYER = 3;
    private static final int MAX_SEA_CREATURES_GLOBAL = 40;
    private static final double BASE_VANILLA_FISH_CHANCE = 0.48D;
    private static final double BASE_TREASURE_CHANCE = 0.07D;
    private static final Set<String> BAIT_IDS = Set.of(
            "simple_bait", "worm_bait", "spicy_bait", "deep_sea_bait", "treasure_bait",
            "monster_bait", "lucky_bait", "crystal_bait", "royal_bait", "void_bait",
            "chum_bait", "nightcrawler_bait"
    );

    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final LavaFishingRegistry lavaFishing;
    private final SkillManager skillManager;
    private final CollectionManager collectionManager;
    private final MMOStatService mmoStatService;
    private final ZoneManager zoneManager;
    private final EconomyManager economyManager;
    private final NamespacedKey seaCreatureKey;
    private final NamespacedKey seaCreatureOwnerKey;
    private final NamespacedKey seaCreatureLevelKey;
    private final NamespacedKey seaCreatureNameKey;
    private final Map<UUID, Set<UUID>> activeSeaCreatures = new HashMap<>();
    private final FishingMinigameManager minigame;

    public FishingGearListener(JavaPlugin plugin, CustomItemManager customItemManager, LavaFishingRegistry lavaFishing,
                               SkillManager skillManager, CollectionManager collectionManager, MMOStatService mmoStatService,
                               ZoneManager zoneManager, EconomyManager economyManager, DataManager dataManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.lavaFishing = lavaFishing;
        this.skillManager = skillManager;
        this.collectionManager = collectionManager;
        this.mmoStatService = mmoStatService;
        this.zoneManager = zoneManager;
        this.economyManager = economyManager;
        this.minigame = new FishingMinigameManager(plugin, dataManager);
        this.seaCreatureKey = new NamespacedKey(plugin, "fishing_sea_creature");
        this.seaCreatureOwnerKey = new NamespacedKey(plugin, "fishing_sea_creature_owner");
        this.seaCreatureLevelKey = new NamespacedKey(plugin, "fishing_sea_creature_level");
        this.seaCreatureNameKey = new NamespacedKey(plugin, "fishing_sea_creature_name");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupSeaCreatures, 200L, 200L);
    }

    public FishingMinigameManager getMinigame() { return minigame; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = fishingRod(player, event.getHand());

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (isFishingRod(rod) && customItemManager.meetsItemRequirements(player, rod, true)) {
                applyFishingSpeed(player, event.getHook(), peekBait(player));
            }
            return;
        }

        if (event.getState() == PlayerFishEvent.State.REEL_IN && isLavaHook(event)) {
            giveLavaCatch(player);
            event.getHook().remove();
            event.setCancelled(true);
            return;
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) return;
        if (minigame.isActive(player.getUniqueId())) {
            event.setCancelled(true);
            caught.remove();
            player.sendMessage(CC.color("&cFinish your current fishing minigame first."));
            return;
        }
        if (!isFishingRod(rod) || !customItemManager.meetsItemRequirements(player, rod, true)) {
            event.setCancelled(true);
            caught.remove();
            return;
        }

        String baitId = consumeBait(player, rod);
        MMOStatMap stats = mmoStatService == null ? customItemManager.getMmoStats(player, rod) : mmoStatService.getTotalStats(player);
        if (baitId.equals("lucky_bait")) stats.add(MMOStatType.MAGIC_FIND, 5.0D);
        ZoneManager.ZoneSnapshot zone = zoneManager == null ? null : zoneManager.getSnapshot(player);
        int fishingLevel = skillManager.getLevel(player, "fishing");
        event.setExpToDrop(0);

        double treasureChance = Math.clamp(BASE_TREASURE_CHANCE + percent(stats, MMOStatType.TREASURE_CHANCE)
                + baitTreasureChance(baitId), 0.0D, 0.45D);
        double roll = ThreadLocalRandom.current().nextDouble();

        if (roll < treasureChance) {
            caught.remove();
            Treasure treasure = rollTreasure(player, fishingLevel, stats);
            int xp = fishingXp(18, stats);
            if (minigame.enabled()) {
                event.setCancelled(true);
                ItemStack preview = treasure.item() == null ? new ItemStack(Material.GOLD_NUGGET) : treasure.item().clone();
                startMinigame(player, event, rod, new FishingMinigameManager.PendingReward(preview, treasure.name(), "RARE", "NORMAL", xp,
                        () -> completePendingCatch(player, treasure.item(), treasure.coins(), treasure.name(), xp, false, true)));
                return;
            }
            if (treasure.coins() > 0.0D) {
                economyManager.giveCoins(player.getUniqueId(), treasure.coins(), "fishing treasure");
            } else if (treasure.item() != null) {
                giveItem(player, treasure.item());
                addCollectionProgress(player, treasure.item());
            }
            skillManager.addXp(player, "fishing", xp);
            player.sendMessage(CC.color("&6You found treasure: &f" + treasure.name() + "&6!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.4F);
            return;
        }

        if (roll < treasureChance + BASE_VANILLA_FISH_CHANCE) {
            ItemStack vanillaCatch = caught.getItemStack();
            if (minigame.enabled()) {
                event.setCancelled(true); caught.remove();
                startMinigame(player, event, rod, new FishingMinigameManager.PendingReward(vanillaCatch.clone(), itemName(vanillaCatch), "COMMON", "NORMAL", fishingXp(8, stats),
                        () -> completePendingCatch(player, vanillaCatch, 0, itemName(vanillaCatch), fishingXp(8, stats), false, false)));
                return;
            }
            addCollectionProgress(player, vanillaCatch);
            int xp = fishingXp(8, stats);
            skillManager.addXp(player, "fishing", xp);
            actionbar(player, "&bYou caught &f" + itemName(vanillaCatch) + "&b! &7(+" + xp + " Fishing XP)");
            return;
        }

        ItemStack result = rollCustomFish(player);
        int baseXp = catchBaseXp(customItemManager.getItemId(result));
        int xp = fishingXp(baseXp, stats);
        if (minigame.enabled()) {
            event.setCancelled(true); caught.remove();
            String rarity = fishPdc(result, "fish_rarity", "COMMON");
            String size = fishPdc(result, "fish_size_class", "NORMAL");
            startMinigame(player, event, rod, new FishingMinigameManager.PendingReward(result.clone(), itemName(result), rarity, size, xp,
                    () -> completePendingCatch(player, result, 0, itemName(result), xp, true, false)));
            return;
        }
        caught.setItemStack(result);
        addCollectionProgress(player, result);
        skillManager.addXp(player, "fishing", xp);
        sendCustomFishCatch(player, result, xp);
    }

    private void startMinigame(Player player, PlayerFishEvent event, ItemStack rod, FishingMinigameManager.PendingReward reward) {
        Location hook = event.getHook() == null ? player.getLocation() : event.getHook().getLocation();
        if (!minigame.start(player, reward, hook, rod)) player.sendMessage(CC.color("&cThe fishing minigame could not start."));
    }

    private void completePendingCatch(Player player, ItemStack item, double coins, String name, int xp, boolean customFish, boolean treasure) {
        if (!player.isOnline()) return;
        if (coins > 0) economyManager.giveCoins(player.getUniqueId(), coins, "fishing treasure");
        if (item != null) {
            giveItem(player, item.clone());
            if (customItemManager.getItemId(item) == null) collectionManager.addProgress(player, item.getType(), Math.max(1, item.getAmount()));
            else addCollectionProgress(player, item);
        }
        skillManager.addXp(player, "fishing", xp);
        if (plugin instanceof club.abelix.games.fakevival.Game game) game.getEventManager().recordFishingCatch(player);
        actionbar(player, "&7(+" + xp + " Fishing XP)");
        if (treasure) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, .7F, 1.4F);
        if (customFish && item != null && item.hasItemMeta()) {
            String id = customItemManager.getItemId(item);
            int length = item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "fish_length_cm"), PersistentDataType.INTEGER, 0);
            plugin.getLogger().info("[SMP Fishing] Custom fish caught player=" + player.getName() + " fish=" + id + " length=" + length + "cm");
        }
    }

    private String fishPdc(ItemStack item, String key, String fallback) {
        if (item == null || !item.hasItemMeta()) return fallback;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, key), PersistentDataType.STRING, fallback);
    }

    private ItemStack rollCustomFish(Player player) {
        List<CustomFishDefaults80.Fish> eligible = CustomFishDefaults80.fish().stream()
                .filter(fish -> conditionMatches(player, fish.condition())).toList();
        double total = eligible.stream().mapToDouble(this::fishWeight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(1.0D, total));
        CustomFishDefaults80.Fish selected = eligible.getFirst();
        for (CustomFishDefaults80.Fish fish : eligible) {
            roll -= fishWeight(fish);
            if (roll <= 0.0D) { selected = fish; break; }
        }
        ItemStack item = customItemManager.createItem(selected.id(), 1);
        int length = ThreadLocalRandom.current().nextInt(selected.minCm(), selected.maxCm() + 1);
        String size = sizeClass(length);
        long finalPrice = Math.round(selected.basePrice() * sizeMultiplier(size) * rarityMultiplier(selected.rarity()));
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "fish_type"), PersistentDataType.STRING, selected.id());
        pdc.set(new NamespacedKey(plugin, "fish_length_cm"), PersistentDataType.INTEGER, length);
        pdc.set(new NamespacedKey(plugin, "fish_size_class"), PersistentDataType.STRING, size);
        pdc.set(new NamespacedKey(plugin, "fish_rarity"), PersistentDataType.STRING, selected.rarity());
        pdc.set(new NamespacedKey(plugin, "base_sell_price"), PersistentDataType.DOUBLE, (double) selected.basePrice());
        pdc.set(new NamespacedKey(plugin, "final_sell_price"), PersistentDataType.DOUBLE, (double) finalPrice);
        pdc.set(new NamespacedKey(plugin, "caught_by"), PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(new NamespacedKey(plugin, "caught_at"), PersistentDataType.LONG, System.currentTimeMillis());
        meta.setLore(List.of(CC.color("&7Length: &b" + length + "cm"), CC.color("&7Size: " + sizeDisplay(size)),
                CC.color("&7Sell Price: &6$" + finalPrice), "", CC.color("&8Caught from fishing."), "",
                CC.color(rarityColor(selected.rarity()) + selected.rarity())));
        item.setItemMeta(meta);
        return item;
    }

    private boolean conditionMatches(Player player, String condition) {
        if (condition == null || condition.isBlank()) return true;
        long time = player.getWorld().getTime();
        String biome = player.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase(Locale.ROOT);
        return switch (condition) {
            case "NIGHT" -> time >= 13000L && time <= 23000L;
            case "MORNING" -> time <= 3000L;
            case "NIGHT_RARE" -> time >= 13000L && time <= 23000L && ThreadLocalRandom.current().nextDouble() < 0.08D;
            default -> biome.contains(condition);
        };
    }

    private double fishWeight(CustomFishDefaults80.Fish fish) { return switch (fish.rarity()) {case "MYTHIC"->0.08D;case "LEGENDARY"->0.35D;case "EPIC"->1.5D;case "RARE"->5.0D;case "UNCOMMON"->16.0D;default->45.0D;}; }
    private String sizeClass(int cm){if(cm<=15)return "TINY";if(cm<=30)return "SMALL";if(cm<=60)return "NORMAL";if(cm<=100)return "BIG";if(cm<=160)return "HUGE";return "GIANT";}
    private double sizeMultiplier(String size){return switch(size){case "TINY"->0.60D;case "SMALL"->0.85D;case "BIG"->1.35D;case "HUGE"->1.75D;case "GIANT"->2.50D;default->1.0D;};}
    private double rarityMultiplier(String rarity){return switch(rarity){case "UNCOMMON"->1.35D;case "RARE"->2.0D;case "EPIC"->3.5D;case "LEGENDARY"->7.5D;case "MYTHIC"->15.0D;default->1.0D;};}
    private String sizeDisplay(String size){return switch(size){case "TINY"->"&7Tiny";case "SMALL"->"&fSmall";case "BIG"->"&bBig";case "HUGE"->"&5Huge";case "GIANT"->"&6Giant";default->"&aNormal";};}
    private String rarityColor(String rarity){return switch(rarity){case "UNCOMMON"->"&a";case "RARE"->"&9";case "EPIC"->"&5";case "LEGENDARY"->"&6";case "MYTHIC"->"&d";default->"&f";};}
    private void sendCustomFishCatch(Player player,ItemStack fish,int xp){ItemMeta meta=fish.getItemMeta();var pdc=meta.getPersistentDataContainer();int cm=pdc.getOrDefault(new NamespacedKey(plugin,"fish_length_cm"),PersistentDataType.INTEGER,0);double price=pdc.getOrDefault(new NamespacedKey(plugin,"final_sell_price"),PersistentDataType.DOUBLE,0D);String size=pdc.getOrDefault(new NamespacedKey(plugin,"fish_size_class"),PersistentDataType.STRING,"NORMAL");String rarity=pdc.getOrDefault(new NamespacedKey(plugin,"fish_rarity"),PersistentDataType.STRING,"COMMON");String name=meta.hasDisplayName()?meta.getDisplayName():itemName(fish);if(size.equals("GIANT"))player.sendMessage(CC.color("&6&lGIANT FISH! &7"+name+" &7measured &b"+cm+"cm&7!"));else if(Set.of("RARE","EPIC","LEGENDARY","MYTHIC").contains(rarity))player.sendMessage(CC.color("&b&lRARE CATCH! &7You caught "+name+" &7("+cm+"cm)&7 worth &6$"+Math.round(price)+"&7!"));else player.sendMessage(CC.color("&aYou caught "+name+" &7("+cm+"cm)&a!"));actionbar(player,"&7(+"+xp+" Fishing XP)");}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isBait(customItemManager.getItemId(event.getItem()))) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cFishing bait is consumed automatically while using a fishing rod."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBaitUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (!isBait(customItemManager.getItemId(event.getItem()))) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cFishing bait cannot be eaten or used by hand."));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSeaCreatureDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || seaCreatureId(living).isBlank()) return;
        if (isEngineMob(living)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> updateSeaCreatureName(living));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSeaCreatureHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || seaCreatureId(living).isBlank()) return;
        if (isEngineMob(living)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> updateSeaCreatureName(living));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSeaCreatureDeath(EntityDeathEvent event) {
        LivingEntity living = event.getEntity();
        String creatureId = seaCreatureId(living);
        if (creatureId.isBlank()) return;

        UUID ownerId = seaCreatureOwner(living);
        removeTrackedCreature(ownerId, living.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player owner = ownerId == null ? living.getKiller() : plugin.getServer().getPlayer(ownerId);
        if (owner == null) owner = living.getKiller();
        if (owner == null) return;

        int level = living.getPersistentDataContainer().getOrDefault(seaCreatureLevelKey, PersistentDataType.INTEGER, 1);
        MMOStatMap stats = mmoStatService == null ? MMOStatMap.empty() : mmoStatService.getTotalStats(owner);
        List<ItemStack> drops = seaCreatureDrops(creatureId, level, stats);
        event.getDrops().addAll(drops);
        for (ItemStack drop : drops) addCollectionProgress(owner, drop);
        skillManager.addXp(owner, "fishing", fishingXp(Math.max(8, level * 2), stats));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeOwnedSeaCreatures(event.getPlayer().getUniqueId());
    }

    private void applyFishingSpeed(Player player, FishHook hook, String baitId) {
        MMOStatMap stats = mmoStatService == null ? customItemManager.getMmoStats(player, player.getInventory().getItemInMainHand()) : mmoStatService.getTotalStats(player);
        double speed = Math.max(0.0D, stats.get(MMOStatType.FISHING_SPEED)) + baitFishingSpeed(baitId);
        double multiplier = 1.0D - Math.min(0.70D, speed / 100.0D);
        int minimum = Math.max(40, (int) Math.round(100 * multiplier));
        int maximum = Math.max(minimum + 20, (int) Math.round(600 * multiplier));
        hook.setWaitTime(minimum, maximum);
    }

    private ItemStack fishingRod(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
    }

    private boolean isFishingRod(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getType() != Material.FISHING_ROD) return false;
        String id = customItemManager.getItemId(item);
        return id == null || customItemManager.getMmoItemType(item) == MMOItemType.FISHING_ROD;
    }

    private String peekBait(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            String id = normalize(customItemManager.getItemId(item));
            if (isBait(id)) return id;
        }
        return "";
    }

    private String consumeBait(Player player, ItemStack rod) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            String id = normalize(customItemManager.getItemId(item));
            if (!isBait(id)) continue;
            int saver = plugin instanceof club.abelix.games.fakevival.Game game
                    ? game.getCustomEnchantManager().effectLevel(rod, "bait_saver") : 0;
            if (saver > 0 && ThreadLocalRandom.current().nextDouble() < saver * 0.05D) return id;
            if (item.getAmount() <= 1) contents[slot] = null;
            else item.setAmount(item.getAmount() - 1);
            player.getInventory().setStorageContents(contents);
            return id;
        }
        return "";
    }

    private boolean isBait(String id) {
        String key = normalize(id);
        if (BAIT_IDS.contains(key)) return true;
        if (key.isBlank() || !customItemManager.itemExists(key)) return false;
        MMOItemType type = customItemManager.getMmoItemType(customItemManager.createItem(key, 1));
        return type == MMOItemType.FISHING_BAIT || type == MMOItemType.BAIT;
    }

    private int fortuneAmount(MMOStatMap stats, String baitId) {
        double fortune = Math.max(0.0D, stats.get(MMOStatType.FISHING_FORTUNE));
        if (baitId.equals("worm_bait") || baitId.equals("nightcrawler_bait")) fortune += 12.0D;
        int extra = Math.min(2, (int) (fortune / 100.0D));
        double remainder = (fortune % 100.0D) / 100.0D;
        if (extra < 2 && ThreadLocalRandom.current().nextDouble() < remainder) extra++;
        return 1 + extra;
    }

    private ItemStack rollFishingMaterial(int level, ZoneManager.ZoneSnapshot zone, String baitId) {
        String zoneName = zone == null ? "" : normalize(zone.name());
        List<WeightedCatch> pool = new ArrayList<>();
        pool.add(new WeightedCatch("raw_river_fish", 40.0D));
        pool.add(new WeightedCatch("seaweed_bundle", 18.0D));
        pool.add(new WeightedCatch("silver_fish_scale", 17.0D));
        pool.add(new WeightedCatch("glowing_roe", level >= 5 ? 8.0D : 3.0D));
        pool.add(new WeightedCatch("coral_fragment", level >= 8 ? 6.0D : 1.0D));
        pool.add(new WeightedCatch("golden_fish_scale", level >= 10 ? 5.0D : 0.5D));
        pool.add(new WeightedCatch("pearl_fragment", level >= 12 ? 3.0D : 0.2D));
        pool.add(new WeightedCatch("deep_sea_fin", level >= 15 ? 2.5D + rareFishBonus(baitId) : 0.1D));
        pool.add(new WeightedCatch("ancient_fish_bone", level >= 25 ? 0.8D : 0.0D));
        pool.add(new WeightedCatch("enchanted_kelp", level >= 8 ? 3.0D : 0.5D));
        pool.add(new WeightedCatch("river_fish_scale", 16.0D));
        pool.add(new WeightedCatch("salmon_fin", 11.0D));
        pool.add(new WeightedCatch("puffer_spike", level >= 5 ? 4.0D : 1.0D));
        pool.add(new WeightedCatch("tropical_fish_gem", level >= 12 ? 1.5D : 0.1D));
        pool.add(new WeightedCatch("ancient_pearl_scale", level >= 25 ? 0.25D : 0.0D));
        if (zoneName.contains("crystal")) pool.add(new WeightedCatch("crystal_fish_shard", baitId.equals("crystal_bait") ? 14.0D : 5.0D));
        if (zoneName.contains("royal")) pool.add(new WeightedCatch("royal_fish_scale", baitId.equals("royal_bait") ? 10.0D : 3.0D));
        if (zoneName.contains("void") && baitId.equals("void_bait")) pool.add(new WeightedCatch("voidwater_scale", 9.0D));
        if (zoneName.contains("storm")) pool.add(new WeightedCatch("stormscale_fin", level >= 30 ? 2.0D : 0.2D));
        return customItemManager.createItem(weightedCatch(pool), 1);
    }

    private Treasure rollTreasure(Player player, int fishingLevel, MMOStatMap stats) {
        double magicFind = Math.max(0.0D, stats.get(MMOStatType.MAGIC_FIND));
        double roll = ThreadLocalRandom.current().nextDouble(100.0D);
        if (roll < 40.0D) {
            double coins = Math.min(500.0D, 20.0D + fishingLevel * 5.0D + ThreadLocalRandom.current().nextInt(10, 51));
            return new Treasure(null, Math.round(coins), economyManager.format(coins) + " Coins");
        }
        if (roll < 62.0D) return treasureItem("treasure_shell", 1);
        if (roll < 78.0D) return treasureItem("pearl_fragment", 1);
        if (roll < 90.0D) return treasureItem("enchanted_kelp", ThreadLocalRandom.current().nextInt(1, 4));
        if (roll < Math.min(98.0D, 95.0D + magicFind * 0.05D)) return treasureItem("treasure_bait", ThreadLocalRandom.current().nextInt(2, 5));
        return treasureItem("golden_fish_scale", ThreadLocalRandom.current().nextInt(1, 3));
    }

    private Treasure treasureItem(String id, int amount) {
        ItemStack item = customItemManager.createItem(id, amount);
        return new Treasure(item, 0.0D, itemName(item));
    }


    private LivingEntity spawnOptimizedSeaCreature(SeaCreature creature, Location spawn, int level) {
        double health = (20.0D + level * 5.0D) * (creature.boss() ? 5.0D : 1.0D);
        double damage = (2.0D + level * 0.35D) * (creature.boss() ? 1.5D : 1.0D);
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSmpMobEngine() != null) {
            return game.getSmpMobEngine().spawnMob("sea_creature_" + creature.id(), spawn, creature.type(), creature.name(), health, damage, level, java.util.List.of(), true);
        }
        Entity spawned = spawn.getWorld().spawnEntity(spawn, creature.type());
        return spawned instanceof LivingEntity living ? living : null;
    }

    private boolean spawnSeaCreature(Player player, Location hookLocation, int fishingLevel, ZoneManager.ZoneSnapshot zone,
                                     String baitId, MMOStatMap stats) {
        Set<UUID> active = activeSeaCreatures.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        active.removeIf(uuid -> plugin.getServer().getEntity(uuid) == null);
        if (active.size() >= MAX_SEA_CREATURES_PER_PLAYER) return false;
        int globalActive = activeSeaCreatures.values().stream().mapToInt(Set::size).sum();
        if (globalActive >= MAX_SEA_CREATURES_GLOBAL) return false;

        SeaCreature creature = selectSeaCreature(fishingLevel, baitId, stats);
        int level = seaCreatureLevel(fishingLevel, zone, baitId, stats);
        Location spawn = hookLocation.clone().add(0.0D, 1.0D, 0.0D);
        LivingEntity living = spawnOptimizedSeaCreature(creature, spawn, level);
        if (living == null) {
            return false;
        }
        if (living instanceof Ageable ageable) ageable.setAdult();
        if (living instanceof Slime slime) slime.setSize(Math.min(4, 1 + level / 12));
        living.setRemoveWhenFarAway(true);
        living.setPersistent(false);
        living.getPersistentDataContainer().set(seaCreatureKey, PersistentDataType.STRING, creature.id());
        living.getPersistentDataContainer().set(seaCreatureOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        living.getPersistentDataContainer().set(seaCreatureLevelKey, PersistentDataType.INTEGER, level);
        living.getPersistentDataContainer().set(seaCreatureNameKey, PersistentDataType.STRING, creature.name());
        applySeaCreatureStats(living, level, creature.boss());
        if (living instanceof Mob mob) mob.setTarget(player);
        active.add(living.getUniqueId());
        updateSeaCreatureName(living);
        player.sendMessage(CC.color("&cA wild " + creature.name() + " appeared!"));
        player.playSound(player.getLocation(), Sound.ENTITY_DROWNED_AMBIENT_WATER, 0.8F, 1.2F);
        return true;
    }

    private SeaCreature selectSeaCreature(int fishingLevel, String baitId, MMOStatMap stats) {
        List<SeaCreature> available = new ArrayList<>();
        for (SeaCreature creature : SEA_CREATURES) {
            if (fishingLevel >= creature.minimumFishingLevel()) available.add(creature);
        }
        double magicFind = Math.max(0.0D, stats.get(MMOStatType.MAGIC_FIND));
        double total = 0.0D;
        for (SeaCreature creature : available) total += adjustedCreatureWeight(creature, baitId, magicFind);
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(1.0D, total));
        double cursor = 0.0D;
        for (SeaCreature creature : available) {
            cursor += adjustedCreatureWeight(creature, baitId, magicFind);
            if (roll <= cursor) return creature;
        }
        return available.getFirst();
    }

    private double adjustedCreatureWeight(SeaCreature creature, String baitId, double magicFind) {
        double weight = creature.weight();
        if (baitId.equals("monster_bait") && creature.minimumFishingLevel() >= 10) weight *= 1.5D;
        if (creature.boss()) weight *= 1.0D + Math.min(0.5D, magicFind / 200.0D);
        return weight;
    }

    private int seaCreatureLevel(int fishingLevel, ZoneManager.ZoneSnapshot zone, String baitId, MMOStatMap stats) {
        int minimum;
        int maximum;
        if (fishingLevel <= 5) { minimum = 1; maximum = 5; }
        else if (fishingLevel <= 15) { minimum = 6; maximum = 15; }
        else if (fishingLevel <= 30) { minimum = 16; maximum = 30; }
        else { minimum = 31; maximum = 50; }
        int zoneBonus = zone == null ? 0 : Math.min(3, Math.max(0, zone.level() - fishingLevel) / 5);
        int baitBonus = baitId.equals("monster_bait") || baitId.equals("void_bait") ? 1 : 0;
        int gearBonus = Math.min(2, (int) Math.floor(Math.max(0.0D, stats.get(MMOStatType.FISHING_SPEED)) / 25.0D));
        return Math.clamp(fishingLevel + ThreadLocalRandom.current().nextInt(-2, 3) + zoneBonus + baitBonus + gearBonus, minimum, maximum);
    }

    private void applySeaCreatureStats(LivingEntity living, int level, boolean boss) {
        double health = (20.0D + level * 5.0D) * (boss ? 5.0D : 1.0D);
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.min(1024.0D, health));
            living.setHealth(Math.min(health, maxHealth.getValue()));
        }
        AttributeInstance attack = living.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue((2.0D + level * 0.35D) * (boss ? 1.5D : 1.0D));
        updateSeaCreatureName(living);
    }

    private void updateSeaCreatureName(LivingEntity living) {
        if (living == null || !living.isValid()) return;
        String id = seaCreatureId(living);
        if (id.isBlank()) return;
        int level = living.getPersistentDataContainer().getOrDefault(seaCreatureLevelKey, PersistentDataType.INTEGER, 1);
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? living.getMaxHealth() : maxHealth.getValue();
        String fallback = SEA_CREATURES.stream().filter(creature -> creature.id().equals(id)).map(SeaCreature::name).findFirst().orElse("Sea Creature");
        String name = living.getPersistentDataContainer().getOrDefault(seaCreatureNameKey, PersistentDataType.STRING, fallback);
        if (name == null || name.isBlank()) name = "Sea Creature";
        String format = "&8[&7Lv.%level%&8] &c%name% &a%health%&7/&a%max_health%&c❤";
        living.setCustomName(CC.color(format
                .replace("%level%", String.valueOf(Math.max(1, level)))
                .replace("%name%", name)
                .replace("%health%", formatHealth(Math.max(0.0D, living.getHealth())))
                .replace("%max_health%", formatHealth(Math.max(0.0D, max)))));
        living.setCustomNameVisible(true);
    }

    private String formatHealth(double health) {
        if (!Double.isFinite(health)) return "0";
        return String.valueOf(Math.max(0L, Math.round(health)));
    }

    private List<ItemStack> seaCreatureDrops(String creatureId, int level, MMOStatMap stats) {
        List<ItemStack> drops = new ArrayList<>();
        String material = switch (creatureId) {
            case "river_slime", "drowned_fisherman", "angry_pufferfish" -> "silver_fish_scale";
            case "sea_zombie", "coral_guardian" -> "coral_fragment";
            case "deep_sea_hunter" -> "deep_sea_fin";
            case "crystal_crab" -> "crystal_fish_shard";
            case "treasure_mimic" -> "treasure_shell";
            case "royal_angler" -> "royal_fish_scale";
            case "void_serpent" -> "voidwater_scale";
            case "storm_eel" -> "stormscale_fin";
            case "ancient_leviathan" -> "ancient_fish_bone";
            default -> "raw_river_fish";
        };
        int amount = 1 + Math.min(2, level / 20);
        double fortune = Math.max(0.0D, stats.get(MMOStatType.FISHING_FORTUNE));
        if (ThreadLocalRandom.current().nextDouble(100.0D) < Math.min(75.0D, fortune)) amount++;
        drops.add(customItemManager.createItem(material, amount));
        if (creatureId.equals("ancient_leviathan")) drops.add(customItemManager.createItem("treasure_shell", 1));
        return drops;
    }

    private void cleanupSeaCreatures() {
        for (UUID ownerId : List.copyOf(activeSeaCreatures.keySet())) {
            Player owner = plugin.getServer().getPlayer(ownerId);
            Set<UUID> creatures = activeSeaCreatures.get(ownerId);
            if (creatures == null) continue;
            for (UUID creatureId : Set.copyOf(creatures)) {
                Entity entity = plugin.getServer().getEntity(creatureId);
                boolean remove = entity == null || !entity.isValid() || owner == null || !owner.isOnline()
                        || entity.getWorld() != owner.getWorld() || entity.getLocation().distanceSquared(owner.getLocation()) > 4096.0D;
                if (remove) {
                    if (entity != null) entity.remove();
                    creatures.remove(creatureId);
                } else if (entity instanceof LivingEntity living) {
                    updateSeaCreatureName(living);
                }
            }
            if (creatures.isEmpty()) activeSeaCreatures.remove(ownerId);
        }
    }

    private void removeOwnedSeaCreatures(UUID ownerId) {
        Set<UUID> creatures = activeSeaCreatures.remove(ownerId);
        if (creatures == null) return;
        for (UUID creatureId : creatures) {
            Entity entity = plugin.getServer().getEntity(creatureId);
            if (entity != null) entity.remove();
        }
    }

    private void removeTrackedCreature(UUID ownerId, UUID creatureId) {
        if (ownerId == null) return;
        Set<UUID> creatures = activeSeaCreatures.get(ownerId);
        if (creatures == null) return;
        creatures.remove(creatureId);
        if (creatures.isEmpty()) activeSeaCreatures.remove(ownerId);
    }

    private String seaCreatureId(LivingEntity entity) {
        return normalize(entity.getPersistentDataContainer().get(seaCreatureKey, PersistentDataType.STRING));
    }

    private UUID seaCreatureOwner(LivingEntity entity) {
        String raw = entity.getPersistentDataContainer().get(seaCreatureOwnerKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ignored) { return null; }
    }

    private void addCollectionProgress(Player player, ItemStack item) {
        String id = customItemManager.getItemId(item);
        if (id == null) return;
        collectionManager.addCustomItemProgress(player, id, Math.max(1, item.getAmount()));
        if (!"CUSTOM_FISH".equals(customItemManager.getCustomItemType(item)) || !item.hasItemMeta()) return;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String rarity = pdc.getOrDefault(new NamespacedKey(plugin,"fish_rarity"),PersistentDataType.STRING,"COMMON");
        String size = pdc.getOrDefault(new NamespacedKey(plugin,"fish_size_class"),PersistentDataType.STRING,"NORMAL");
        if (Set.of("RARE","EPIC","LEGENDARY","MYTHIC").contains(rarity)) collectionManager.addCustomItemProgress(player,"rare_fish_marker",1);
        if (size.equals("GIANT")) collectionManager.addCustomItemProgress(player,"giant_fish_marker",1);
        boolean biomeFish=CustomFishDefaults80.fish().stream().anyMatch(f->f.id().equals(id)&&!f.condition().isBlank());
        if(biomeFish)collectionManager.addCustomItemProgress(player,"biome_fish_marker",1);
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private int fishingXp(int base, MMOStatMap stats) {
        double boost = Math.max(0.0D, stats.get(MMOStatType.FISHING_XP_BOOST));
        return Math.max(1, (int) Math.round(base * (1.0D + boost / 100.0D)));
    }

    private int catchBaseXp(String itemId) {
        return switch (normalize(itemId)) {
            case "raw_river_fish", "seaweed_bundle" -> 8;
            case "silver_fish_scale", "glowing_roe", "enchanted_kelp" -> 12;
            case "golden_fish_scale", "coral_fragment" -> 18;
            case "deep_sea_fin", "pearl_fragment", "crystal_fish_shard" -> 24;
            case "royal_fish_scale", "voidwater_scale", "stormscale_fin", "ancient_fish_bone" -> 32;
            default -> 10;
        };
    }

    private double percent(MMOStatMap stats, MMOStatType type) {
        return Math.max(0.0D, stats.get(type)) / 100.0D;
    }

    private double baitFishingSpeed(String id) {
        return switch (id) { case "simple_bait" -> 10.0D; case "chum_bait" -> 6.0D; default -> 0.0D; };
    }

    private double baitTreasureChance(String id) {
        return switch (id) {
            case "treasure_bait" -> 0.14D;
            case "monster_bait" -> 0.10D;
            case "cosmic_bait" -> 0.10D;
            default -> 0.0D;
        };
    }

    private double rareFishBonus(String id) {
        return switch (id) {
            case "spicy_bait" -> 6.0D;
            case "deep_sea_bait" -> 5.0D;
            case "royal_bait" -> 3.0D;
            default -> 0.0D;
        };
    }

    private String weightedCatch(List<WeightedCatch> catches) {
        double total = catches.stream().mapToDouble(WeightedCatch::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(1.0D, total));
        double cursor = 0.0D;
        for (WeightedCatch entry : catches) {
            cursor += entry.weight();
            if (roll <= cursor) return entry.id();
        }
        return catches.getFirst().id();
    }

    private ItemStack oldBoot() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color("&7Old Boot"));
            meta.setLore(List.of(CC.color("&8Fishing Junk")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String itemName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String stripped = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (stripped != null && !stripped.isBlank()) return stripped + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
        }
        return item == null ? "Nothing" : pretty(item.getType().name()) + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
    }

    private void actionbar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(CC.color(message)));
    }

    private boolean isLavaHook(PlayerFishEvent event) {
        if (!lavaFishing.catchDefinition().enabled()) return false;
        Material block = event.getHook().getLocation().getBlock().getType();
        Material below = event.getHook().getLocation().subtract(0.0D, 0.35D, 0.0D).getBlock().getType();
        return block == Material.LAVA || below == Material.LAVA;
    }

    private void giveLavaCatch(Player player) {
        List<LavaFishingRewardDefinition> customItems = lavaFishing.customRewards();
        if (!customItems.isEmpty() && ThreadLocalRandom.current().nextDouble(100.0D) < lavaFishing.catchDefinition().customItemChancePercent()) {
            LavaFishingRewardDefinition reward = weightedCustomReward(customItems);
            if (customItemManager.itemExists(reward.customItemId())) {
                giveItem(player, customItemManager.createItem(reward.customItemId(), 1));
                player.sendMessage(CC.color(lavaFishing.catchDefinition().message()));
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.8F, 1.2F);
                skillManager.addXp(player, "fishing", 12);
                return;
            }
        }
        giveItem(player, randomLavaDrop());
        player.sendMessage(CC.color(lavaFishing.catchDefinition().message()));
        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.8F, 1.2F);
        skillManager.addXp(player, "fishing", 10);
    }

    private ItemStack randomLavaDrop() {
        LavaFishingLootDefinition definition = weightedLoot();
        int amount = definition.minAmount() >= definition.maxAmount() ? definition.minAmount()
                : ThreadLocalRandom.current().nextInt(definition.minAmount(), definition.maxAmount() + 1);
        ItemStack item = new ItemStack(definition.material(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && lavaFishing.catchDefinition().renameDrops()) {
            meta.setDisplayName(CC.color(definition.displayName()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private LavaFishingRewardDefinition weightedCustomReward(List<LavaFishingRewardDefinition> rewards) {
        double total = rewards.stream().mapToDouble(LavaFishingRewardDefinition::chancePercent).sum();
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(total, 1.0D));
        double cursor = 0.0D;
        for (LavaFishingRewardDefinition reward : rewards) {
            cursor += reward.chancePercent();
            if (roll <= cursor) return reward;
        }
        return rewards.getLast();
    }

    private LavaFishingLootDefinition weightedLoot() {
        List<LavaFishingLootDefinition> loot = lavaFishing.loot();
        double total = loot.stream().mapToDouble(LavaFishingLootDefinition::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(Math.max(total, 1.0D));
        double cursor = 0.0D;
        for (LavaFishingLootDefinition definition : loot) {
            cursor += definition.weight();
            if (roll <= cursor) return definition;
        }
        return loot.getLast();
    }

    private String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String id) {
        StringBuilder builder = new StringBuilder();
        for (String part : normalize(id).split("_")) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private boolean isEngineMob(org.bukkit.entity.Entity entity) {
        return plugin instanceof club.abelix.games.fakevival.Game game
                && game.getSmpMobEngine() != null
                && game.getSmpMobEngine().isEngineMob(entity);
    }

    private record WeightedCatch(String id, double weight) {}
    private record Treasure(ItemStack item, double coins, String name) {}
    private record SeaCreature(String id, String name, EntityType type, int minimumFishingLevel, double weight, boolean boss) {}

    private static final List<SeaCreature> SEA_CREATURES = List.of(
            new SeaCreature("drowned_fisherman", "Drowned Fisherman", EntityType.DROWNED, 1, 24.0D, false),
            new SeaCreature("river_slime", "River Slime", EntityType.SLIME, 1, 22.0D, false),
            new SeaCreature("angry_pufferfish", "Angry Pufferfish", EntityType.PUFFERFISH, 3, 16.0D, false),
            new SeaCreature("sea_zombie", "Sea Zombie", EntityType.ZOMBIE, 6, 14.0D, false),
            new SeaCreature("coral_guardian", "Coral Guardian", EntityType.GUARDIAN, 10, 10.0D, false),
            new SeaCreature("deep_sea_hunter", "Deep Sea Hunter", EntityType.DROWNED, 15, 7.0D, false),
            new SeaCreature("crystal_crab", "Crystal Crab", EntityType.SPIDER, 18, 5.0D, false),
            new SeaCreature("treasure_mimic", "Treasure Mimic", EntityType.HUSK, 20, 4.0D, false),
            new SeaCreature("royal_angler", "Royal Angler", EntityType.DROWNED, 24, 3.0D, false),
            new SeaCreature("void_serpent", "Void Serpent", EntityType.ENDERMITE, 28, 2.0D, false),
            new SeaCreature("storm_eel", "Storm Eel", EntityType.GUARDIAN, 32, 1.4D, false),
            new SeaCreature("ancient_leviathan", "Ancient Leviathan", EntityType.ELDER_GUARDIAN, 38, 0.35D, true)
    );
}
