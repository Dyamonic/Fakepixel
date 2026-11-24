package club.abelix.games.fakevival.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import club.abelix.games.fakevival.config.defaults.CustomEnchantDefaults100;
import club.abelix.games.fakevival.config.defaults.CustomEnchantUnlockDefaults;
import club.abelix.games.fakevival.mmo.MMOEnchantStorage;
import club.abelix.games.fakevival.mmo.MMOLoreBuilder;
import club.abelix.games.fakevival.mmo.MMOStatStorage;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class CustomEnchantManager implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey bookKey;
    private final NamespacedKey bookLevelKey;
    private final NamespacedKey customItemKey;
    private final NamespacedKey customRarityKey;
    private final NamespacedKey itemRarityKey;
    private final NamespacedKey abilityKey;
    private final MMOEnchantStorage mmoEnchantStorage;
    private final MMOStatStorage mmoStatStorage;
    private final MMOLoreBuilder mmoLoreBuilder = new MMOLoreBuilder();
    private final Random random = new Random();
    private final Map<String, CustomEnchant> enchants = new LinkedHashMap<>();
    private final Map<String, CustomEnchantDefaults100.Definition> requestedDefinitions = new LinkedHashMap<>();
    private final List<CustomEnchantDefaults100.InvalidDefinition> invalidRequestedDefinitions = new ArrayList<>();
    private final Map<UUID, Map<String, Integer>> projectileEnchants = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> projectileOrigins = new HashMap<>();
    private final Map<UUID, HitSequence> hitSequences = new HashMap<>();

    public CustomEnchantManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bookKey = new NamespacedKey(plugin, "custom_enchant_book");
        this.bookLevelKey = new NamespacedKey(plugin, "custom_enchant_book_level");
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.customRarityKey = new NamespacedKey(plugin, "custom_item_rarity");
        this.itemRarityKey = new NamespacedKey(plugin, "item_rarity");
        this.abilityKey = new NamespacedKey(plugin, "custom_item_ability");
        this.mmoEnchantStorage = new MMOEnchantStorage(plugin);
        this.mmoStatStorage = new MMOStatStorage(plugin);
        registerDefaults();
        registerRequestedDefaults();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::applyPassiveEffects, 40L, 40L);
    }

    public ItemStack createBook(String id, int level) {
        CustomEnchant enchant = get(id);
        if (enchant == null) enchant = get("sharpfang");
        int safeLevel = Math.clamp(level, 1, enchant.maxLevel());
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(enchant.color() + enchant.name() + " " + roman(safeLevel) + " &7Book"));
            meta.setLore(CC.colorList(List.of(
                    "&9" + enchant.name() + " " + roman(safeLevel),
                    "&7" + description(enchant.id()),
                    "",
                    "&7Type: &f" + pretty(enchant.type()),
                    "&7Max Level: &e" + enchant.maxLevel(),
                    "",
                    "&eRight-click while holding the",
                    "&etarget item in your offhand.",
                    "&8Custom Enchant: " + enchant.id()
            )));
            meta.getPersistentDataContainer().set(bookKey, PersistentDataType.STRING, enchant.id());
            meta.getPersistentDataContainer().set(bookLevelKey, PersistentDataType.INTEGER, safeLevel);
            item.setItemMeta(meta);
        }
        return item;
    }

    public Material displayMaterial(String id) {
        CustomEnchant enchant = get(id);
        if (enchant == null) return Material.ENCHANTED_BOOK;
        return switch (enchant.type()) {
            case "weapon" -> Material.DIAMOND_SWORD;
            case "armor" -> Material.DIAMOND_CHESTPLATE;
            case "tool" -> Material.DIAMOND_PICKAXE;
            case "bow" -> Material.BOW;
            case "fishing" -> Material.FISHING_ROD;
            case "magic" -> Material.BLAZE_ROD;
            case "accessory" -> Material.AMETHYST_SHARD;
            case "utility" -> Material.ANVIL;
            default -> Material.ENCHANTED_BOOK;
        };
    }

    public String displayName(String id, int level) {
        CustomEnchant enchant = get(id);
        if (enchant == null) return "&dCustom Enchant Book";
        return enchant.color() + enchant.name() + " " + roman(Math.clamp(level, 1, enchant.maxLevel()));
    }

    public CustomEnchant get(String id) {
        return enchants.get(normalize(id));
    }

    public List<String> ids() {
        return new ArrayList<>(enchants.keySet());
    }

    public List<CustomEnchant> all() {
        return new ArrayList<>(enchants.values());
    }

    public List<CustomEnchant> byType(String type) {
        String normalized = normalize(type);
        return enchants.values().stream()
                .filter(enchant -> enchant.type().equals(normalized))
                .sorted(Comparator.comparing(CustomEnchant::name))
                .toList();
    }

    public boolean canApply(ItemStack item, String id) {
        CustomEnchant enchant = get(id);
        return enchant != null && appliesTo(item, enchant);
    }

    public String description(String id) {
        CustomEnchantDefaults100.Definition definition = requestedDefinitions.get(normalize(id));
        return definition == null ? "Enhances compatible equipment through the custom enchant system." : definition.description();
    }

    public CustomEnchantDefaults100.Definition requestedDefinition(String id) {
        return requestedDefinitions.get(normalize(id));
    }

    public Set<String> requestedIds() {
        return Set.copyOf(requestedDefinitions.keySet());
    }

    public CustomEnchantUnlockDefaults.UnlockRequirement unlockRequirement(String id) {
        CustomEnchant enchant = get(id);
        if (enchant == null) return CustomEnchantUnlockDefaults.requirement(id, "COMMON");
        CustomEnchantDefaults100.Definition definition = requestedDefinitions.get(enchant.id());
        return CustomEnchantUnlockDefaults.requirement(enchant.id(), definition == null ? inferredRarity(enchant) : definition.rarity());
    }

    public ItemStack createUnlockBook(String id) {
        CustomEnchant enchant = get(id);
        if (enchant == null) return null;
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(CC.color("&9" + enchant.name() + " Enchantment Manual"));
        meta.setLore(CC.colorList(List.of("&7Study this manual to permanently learn", "&7the &9" + enchant.name() + " &7enchantment.", "", "&eRight-click to learn.")));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING, "ENCHANT_UNLOCK_BOOK");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "unlock_enchant_id"), PersistentDataType.STRING, enchant.id());
        item.setItemMeta(meta);
        return item;
    }

    public RequestedValidation validateRequestedBatch() {
        int registered = 0;
        int metadataWarnings = 0;
        int handlerWarnings = 0;
        int statWarnings = 0;
        int disabled = 0;
        Set<String> seen = new HashSet<>();
        for (CustomEnchantDefaults100.Definition definition : CustomEnchantDefaults100.definitions()) {
            CustomEnchant enchant = get(definition.id());
            if (enchant != null) registered++;
            if (!seen.add(definition.id()) || enchant == null || definition.maxLevel() < 1
                    || definition.allowedItemTypes().isEmpty() || definition.description().isBlank()
                    || definition.rarity().isBlank() || definition.trigger().isBlank()) metadataWarnings++;
            if (!definition.enabled()) disabled++;
            if (definition.enabled() && definition.effect().isBlank() && definition.statBonuses().isEmpty()) handlerWarnings++;
            if (!definition.statBonuses().isEmpty()) {
                Map<MMOStatType, Double> maximum = statBonusesFor(definition.id(), definition.maxLevel());
                for (Map.Entry<MMOStatType, Double> entry : definition.statBonuses().entrySet()) {
                    double expected = entry.getValue() * definition.maxLevel();
                    if (Math.abs(maximum.getOrDefault(entry.getKey(), 0.0D) - expected) > 0.0001D) statWarnings++;
                }
            }
        }
        int skipped = Math.max(invalidRequestedDefinitions.size(), CustomEnchantDefaults100.sourceDefinitionCount() - requestedDefinitions.size());
        return new RequestedValidation(registered, metadataWarnings, handlerWarnings, statWarnings, disabled,
                invalidRequestedDefinitions.size(), skipped, List.copyOf(invalidRequestedDefinitions),
                invalidRequestedDefinitions.isEmpty() && requestedDefinitions.size() == 100 && seen.size() == 100);
    }

    public Map<String, Integer> appliedEnchants(ItemStack item) {
        return new LinkedHashMap<>(getEnchants(item));
    }

    public int effectLevel(ItemStack item, String id) {
        CustomEnchant enchant = get(id);
        if (enchant == null) return 0;
        return Math.min(enchant.maxLevel(), getEnchants(item).getOrDefault(enchant.id(), 0));
    }

    public double abilityCooldownMultiplier(ItemStack item) {
        return Math.max(0.75D, 1.0D - effectLevel(item, "cooldown_flow") * 0.01D);
    }

    public double abilityManaCostMultiplier(ItemStack item) {
        return Math.max(0.75D, 1.0D - effectLevel(item, "arcane_focus") * 0.01D);
    }

    public double abilityPowerMultiplier(ItemStack item, String element, boolean healing) {
        double multiplier = healing ? 1.0D + effectLevel(item, "healing_light") * 0.05D : 1.0D;
        String normalized = normalize(element);
        if (normalized.contains("frost") || normalized.contains("ice")) multiplier += effectLevel(item, "frost_magic") * 0.04D;
        if (normalized.contains("fire") || normalized.contains("flame")) multiplier += effectLevel(item, "fire_magic") * 0.04D;
        if (normalized.contains("shadow") || normalized.contains("dark") || normalized.contains("void")) multiplier += effectLevel(item, "shadow_magic") * 0.04D;
        return multiplier;
    }
    
    public int applyAllCompatibleMaxEnchants(ItemStack item, boolean rebuildLore) {
        if (item == null || item.getType().isAir() || isLoreRewriteBlocked(item)) return 0;
        Map<String, Integer> current = getEnchants(item);
        int changed = 0;
        for (CustomEnchant enchant : enchants.values()) {
            String id = enchant.id();
            if (id.contains("curse") || !appliesTo(item, enchant) || conflictWithMap(current, id) != null) continue;
            if (current.getOrDefault(id, 0) >= enchant.maxLevel()) continue;
            current.put(id, enchant.maxLevel());
            changed++;
        }
        if (changed > 0) writeEnchants(item, current, rebuildLore);
        return changed;
    }

    public Map<String,Integer> modifierEnchants(ItemStack item){return new LinkedHashMap<>(enchantmentsFromBookOrItem(item));}
    public boolean isEnchantModifier(ItemStack item){return !modifierEnchants(item).isEmpty();}

    public String plainName(String id) {
        CustomEnchant enchant = get(id);
        return enchant == null ? pretty(id) : enchant.name();
    }

    public MMOStatMap statBonuses(ItemStack item) {
        MMOStatMap result = MMOStatMap.empty();
        for (Map.Entry<String, Integer> entry : getEnchants(item).entrySet()) {
            for (Map.Entry<MMOStatType, Double> bonus : statBonusesFor(entry.getKey(), entry.getValue()).entrySet()) {
                double value = bonus.getValue();
                if (Double.isFinite(value) && value > 0.0D) result.add(bonus.getKey(), value);
            }
        }
        return result;
    }

    public Map<MMOStatType, Double> statBonusesFor(String id, int level) {
        CustomEnchant enchant = get(id);
        if (enchant == null || level <= 0 || enchant.statBonusesPerLevel().isEmpty()) return Map.of();
        Map<MMOStatType, Double> result = new LinkedHashMap<>();
        enchant.statBonusesPerLevel().forEach((type, value) -> result.put(type, value * Math.min(level, enchant.maxLevel())));
        return result;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseBook(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack book = event.getHand() == EquipmentSlot.HAND ? mainHand : offHand;
        String enchantId = getBookEnchant(book);
        if (enchantId == null) return;
        event.setCancelled(true);

        ItemStack target = event.getHand() == EquipmentSlot.HAND ? offHand : mainHand;
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(CC.color("&cHold the custom enchant book in one hand and the target item in the other."));
            return;
        }
        int level = getBookLevel(book);
        ApplyResult result = applyEnchant(target, enchantId, level);
        if (!result.success()) {
            player.sendMessage(CC.color(result.message()));
            return;
        }
        consumeOne(book);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.15F);
        player.sendMessage(CC.color("&dCustom Enchants &8> &aApplied &f" + result.message() + "&a."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantTable(EnchantItemEvent event) {
        if (isMmoCustomItem(event.getItem())) {
            Map<String, Integer> converted = vanillaEnchants(event.getEnchantsToAdd());
            event.getEnchantsToAdd().clear();
            if (converted.isEmpty()) {
                cleanVanillaEnchants(event.getItem());
                return;
            }
            Map<String, Integer> current = getEnchants(event.getItem());
            converted.forEach((id, level) -> {
                CustomEnchant enchant = get(id);
                if (enchant != null && appliesTo(event.getItem(), enchant)) {
                    current.merge(enchant.id(), Math.clamp(level, 1, enchant.maxLevel()), Math::max);
                }
            });
            writeEnchants(event.getItem(), current);
            event.getEnchanter().sendMessage(CC.color("&dCustom Enchants &8> &fThe table added custom enchant lore to your item."));
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("custom-enchants.enchant-table.enabled", true)) return;
        double chance = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("custom-enchants.enchant-table.chance", 35.0D), 0.0D, 100.0D);
        if (random.nextDouble(100.0D) > chance) return;
        List<CustomEnchant> possible = enchants.values().stream()
                .filter(enchant -> appliesTo(event.getItem(), enchant))
                .sorted(Comparator.comparing(CustomEnchant::id))
                .toList();
        if (possible.isEmpty()) return;
        CustomEnchant picked = possible.get(random.nextInt(possible.size()));
        int maxRoll = Math.clamp((event.getExpLevelCost() / 10) + 1, 1, picked.maxLevel());
        ApplyResult result = applyEnchant(event.getItem(), picked.id(), 1 + random.nextInt(maxRoll));
        if (result.success()) event.getEnchanter().sendMessage(CC.color("&dCustom Enchants &8> &fThe table added &d" + result.message() + "&f."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (first == null || second == null) return;
        if (isMmoCustomItem(first)) {
            Map<String, Integer> additions = enchantmentsFromBookOrItem(second);
            if (additions.isEmpty()) return;
            ItemStack result = first.clone();
            Map<String, Integer> current = getEnchants(result);
            additions.forEach((id, level) -> {
                CustomEnchant enchant = get(id);
                if (enchant != null && appliesTo(result, enchant) && conflictWithMap(current, enchant.id()) == null) {
                    current.merge(enchant.id(), Math.clamp(level, 1, enchant.maxLevel()), Math::max);
                }
            });
            writeEnchants(result, current);
            event.setResult(result);
            return;
        }
        if (isCustomEnchantBook(first) && isCustomEnchantBook(second)) {
            String firstId = getBookEnchant(first);
            String secondId = getBookEnchant(second);
            if (firstId == null || !firstId.equals(secondId)) return;
            CustomEnchant enchant = get(firstId);
            if (enchant == null) return;
            int nextLevel = Math.min(enchant.maxLevel(), Math.max(getBookLevel(first), getBookLevel(second)) + 1);
            event.setResult(createBook(enchant.id(), nextLevel));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDealDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Player player;
        Map<String, Integer> enchants;
        boolean ranged;
        if (event.getDamager() instanceof Player direct) {
            player = direct;
            enchants = getEnchants(player.getInventory().getItemInMainHand());
            ranged = false;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            player = shooter;
            enchants = projectileEnchants.remove(projectile.getUniqueId());
            if (enchants == null) enchants = getEnchants(player.getInventory().getItemInMainHand());
            ranged = true;
        } else {
            return;
        }
        if (enchants.isEmpty()) return;

        if (ranged) {
            applyRangedHit(event, target, enchants);
            projectileOrigins.remove(event.getDamager().getUniqueId());
            return;
        }
        addDamage(event, enchants, "sharpfang", 0.55D);
        addDamage(event, enchants, "sharpness", 0.55D);
        double targetHealth = combatHealth(target);
        double targetMaxHealth = combatMaxHealth(target);
        addDamage(event, enchants, "giant_slayer", Math.min(4.0D, targetHealth * 0.025D));
        addDamage(event, enchants, "dragonbane", target.getType().name().contains("DRAGON") ? 3.0D : 0.0D);
        addDamage(event, enchants, "enderbane", target.getType().name().contains("END") ? 2.0D : 0.0D);
        addDamage(event, enchants, "smitebound", isUndead(target) ? 1.2D : 0.0D);
        addDamage(event, enchants, "smite", isUndead(target) ? 1.2D : 0.0D);
        addDamage(event, enchants, "execute", targetHealth <= targetMaxHealth * 0.35D ? 1.5D : 0.0D);
        addDamage(event, enchants, "first_strike", player.getAttackCooldown() >= 0.95F ? 0.9D : 0.0D);
        addDamage(event, enchants, "critical", player.getFallDistance() > 0.0F ? 0.8D : 0.0D);

        chanceEffect(enchants, "venom", 0.07D, level -> target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, Math.max(0, level - 1))));
        chanceEffect(enchants, "frostbite", 0.06D, level -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, Math.max(0, level - 1))));
        chanceEffect(enchants, "witherbrand", 0.045D, level -> target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 50, Math.max(0, level - 1))));
        chanceEffect(enchants, "lifesteal", 0.05D, level -> player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + level)));
        chanceEffect(enchants, "vampirism", 0.04D, level -> player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + level)));
        chanceEffect(enchants, "thunderstrike", 0.025D, level -> target.getWorld().strikeLightningEffect(target.getLocation()));
        chanceEffect(enchants, "stun", 0.04D, level -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 5)));
        multiplyDamage(event, enchants, "executioner", targetHealth <= targetMaxHealth * 0.35D ? 0.03D : 0.0D);
        int firstStrike = enchants.getOrDefault("first_strike_custom", 0);
        HitSequence sequence = hitSequences.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        boolean first = sequence == null || !sequence.target().equals(target.getUniqueId()) || now - sequence.lastHit() > 10000L;
        if (firstStrike > 0 && first) event.setDamage(event.getDamage() * (1.0D + firstStrike * 0.08D));
        int combo = enchants.getOrDefault("combo_edge", 0);
        int chain = sequence != null && sequence.target().equals(target.getUniqueId()) && now - sequence.lastHit() <= 4000L ? sequence.count() + 1 : 1;
        if (combo > 0 && chain > 1) event.setDamage(event.getDamage() * (1.0D + Math.min(5, chain - 1) * combo * 0.01D));
        hitSequences.put(player.getUniqueId(), new HitSequence(target.getUniqueId(), now, chain));
        multiplyDamage(event, enchants, "final_wound", targetHealth <= targetMaxHealth * 0.20D ? 0.05D : 0.0D);
        multiplyDamage(event, enchants, "lifebreaker", targetHealth >= targetMaxHealth * 0.80D ? 0.02D : 0.0D);
        multiplyDamage(event, enchants, "armor_piercer", 0.02D);
        multiplyDamage(event, enchants, "ragebound", player.getHealth() <= player.getMaxHealth() * 0.50D ? 0.02D : 0.0D);
        multiplyDamage(event, enchants, "dragon_rend", isBoss(target) ? 0.04D : 0.0D);
        multiplyDamage(event, enchants, "holy_strike", isUndead(target) ? 0.05D : 0.0D);
        multiplyDamage(event, enchants, "beast_slayer", target instanceof Animals ? 0.05D : 0.0D);
        multiplyDamage(event, enchants, "dungeon_bane", isDungeon(target) ? 0.04D : 0.0D);
        int blood = enchants.getOrDefault("blood_drinker", 0);
        if (blood > 0) player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + event.getDamage() * blood * 0.01D));
        int venom = enchants.getOrDefault("venom_tip", 0);
        if (venom > 0 && target instanceof Monster) target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, venom * 20, 0));
        int frost = enchants.getOrDefault("frost_cut", 0);
        if (frost > 0 && target instanceof Monster) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, frost * 20, 0));
        chanceEffect(enchants, "thunder_edge", 0.02D, level -> {
            target.getWorld().strikeLightningEffect(target.getLocation());
            event.setDamage(event.getDamage() + level * 2.0D);
        });
        chanceEffect(enchants, "shadow_slash", 0.03D, level -> event.setDamage(event.getDamage() * (1.0D + level * 0.08D)));
        int cleave = enchants.getOrDefault("royal_cleave", 0);
        if (cleave > 0) {
            double cleaveDamage = event.getDamage() * (0.15D + cleave * 0.05D);
            for (LivingEntity nearby : target.getLocation().getNearbyLivingEntities(3.0D, entity -> entity instanceof Monster && !entity.equals(target))) {
                nearby.damage(cleaveDamage, player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Projectile projectile)) return;
        trackBowProjectile(event.getBow(), projectile);
    }

    public void trackBowProjectile(ItemStack bow, Projectile projectile) {
        if (bow == null || projectile == null) return;
        Map<String, Integer> values = getEnchants(bow);
        if (values.isEmpty()) return;
        projectileEnchants.put(projectile.getUniqueId(), Map.copyOf(values));
        projectileOrigins.put(projectile.getUniqueId(), projectile.getLocation().clone());
        int quick = values.getOrDefault("quick_draw", 0);
        if (quick > 0) projectile.setVelocity(projectile.getVelocity().multiply(1.0D + quick * 0.03D));
        int piercing = values.getOrDefault("piercing_shot", 0);
        if (piercing > 0 && projectile instanceof AbstractArrow arrow) arrow.setPierceLevel(Math.min(127, piercing / 2));
    }

    private void applyRangedHit(EntityDamageByEntityEvent event, LivingEntity target, Map<String, Integer> enchants) {
        addDamage(event, enchants, "arrow_force", 5.0D);
        int snipe = enchants.getOrDefault("snipe_custom", 0);
        if (snipe > 0) {
            org.bukkit.Location origin = projectileOrigins.get(event.getDamager().getUniqueId());
            if (origin != null && origin.getWorld().equals(target.getWorld())) {
                int steps = Math.min(10, (int) (origin.distance(target.getLocation()) / 10.0D));
                event.setDamage(event.getDamage() * (1.0D + steps * snipe * 0.04D));
            }
        }
        int burn = enchants.getOrDefault("burning_arrow", 0);
        if (burn > 0 && target instanceof Monster) target.setFireTicks(burn * 20);
        int frost = enchants.getOrDefault("frost_arrow", 0);
        if (frost > 0 && target instanceof Monster) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, frost * 20, 0));
        chanceEffect(enchants, "storm_arrow", 0.04D, level -> {
            target.getWorld().strikeLightningEffect(target.getLocation());
            for (LivingEntity nearby : target.getLocation().getNearbyLivingEntities(4.0D, entity -> entity instanceof Monster && !entity.equals(target))) {
                nearby.damage(event.getDamage() * 0.15D * level);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Map<String, Integer> enchants = equippedArmorEnchants(player);
        if (enchants.isEmpty()) return;

        reduceDamage(event, enchants, "guard", 0.035D);
        reduceDamage(event, enchants, "protection", 0.035D);
        reduceDamage(event, enchants, "stone_skin", 0.03D);
        reduceDamage(event, enchants, "fireguard", event.getCause().name().contains("FIRE") || event.getCause() == EntityDamageEvent.DamageCause.LAVA ? 0.12D : 0.0D);
        reduceDamage(event, enchants, "blastguard", event.getCause().name().contains("EXPLOSION") ? 0.15D : 0.0D);
        reduceDamage(event, enchants, "featherweight", event.getCause() == EntityDamageEvent.DamageCause.FALL ? 0.18D : 0.0D);
        reduceDamage(event, enchants, "fire_ward", event.getCause().name().contains("FIRE") || event.getCause() == EntityDamageEvent.DamageCause.LAVA ? 0.05D : 0.0D);
        reduceDamage(event, enchants, "poison_ward", event.getCause().name().contains("POISON") ? 0.05D : 0.0D);
        reduceDamage(event, enchants, "feather_step", event.getCause() == EntityDamageEvent.DamageCause.FALL ? 0.08D : 0.0D);
        reduceDamage(event, enchants, "safe_landing", event.getCause() == EntityDamageEvent.DamageCause.FALL ? 0.05D : 0.0D);
        if (enchants.containsKey("last_stand") && player.getHealth() <= player.getMaxHealth() * 0.3D) reduceDamage(event, enchants, "last_stand", 0.08D);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Map<String, Integer> values = equippedArmorEnchants(player);
        int guard = values.getOrDefault("battle_guard", 0);
        if (guard > 0 && event.getDamager() instanceof Monster) event.setDamage(event.getDamage() * Math.max(0.0D, 1.0D - guard * 0.02D));
        int thorns = values.getOrDefault("thorns_custom", 0);
        if (thorns > 0 && event.getDamager() instanceof LivingEntity attacker) attacker.damage(event.getDamage() * thorns * 0.01D, player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Map<String, Integer> enchants = getEnchants(tool);
        if (enchants.isEmpty()) return;
        if (enchants.getOrDefault("replant_touch", 0) > 0 && event.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable crop
                && crop.getAge() >= crop.getMaximumAge()) {
            Block block = event.getBlock();
            Material cropType = block.getType();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!block.getType().isAir()) return;
                block.setType(cropType);
                if (block.getBlockData() instanceof org.bukkit.block.data.Ageable replanted) {
                    replanted.setAge(0);
                    block.setBlockData(replanted);
                }
            });
        }
        int xpBoost = enchants.getOrDefault("prospector", 0) + enchants.getOrDefault("scholar", 0) + enchants.getOrDefault("experience", 0);
        if (xpBoost > 0) event.setExpToDrop(event.getExpToDrop() + xpBoost);
        int fortune = enchants.getOrDefault("fortune", 0);
        if (fortune > 0 && random.nextDouble() < Math.min(0.75D, fortune * 0.08D)) {
            for (ItemStack drop : event.getBlock().getDrops(tool, event.getPlayer())) {
                if (drop == null || drop.getType().isAir()) continue;
                ItemStack extra = drop.clone();
                extra.setAmount(Math.max(1, drop.getAmount()));
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D), extra);
            }
        }
        if (enchants.containsKey("autosmelt") || enchants.containsKey("smelting_touch")) autoSmelt(event.getBlock());
        int bonus = enchants.getOrDefault("gemstone_touch", 0) + enchants.getOrDefault("compacting_force", 0)
                + enchants.getOrDefault("root_finder", 0) + enchants.getOrDefault("seed_saver", 0)
                + enchants.getOrDefault("golden_harvest", 0) + enchants.getOrDefault("treasure_vein", 0);
        if (bonus > 0 && random.nextDouble() < Math.min(0.50D, bonus * 0.01D)) {
            for (ItemStack drop : event.getBlock().getDrops(tool, event.getPlayer())) {
                if (!drop.getType().isAir()) event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
            }
        }
        int smelt = enchants.getOrDefault("smelters_touch", 0);
        if (smelt > 0 && random.nextDouble() < smelt / 3.0D) autoSmelt(event.getBlock());
        event.setExpToDrop(event.getExpToDrop() + enchants.getOrDefault("cave_blessing", 0) * 2 + enchants.getOrDefault("crop_wisdom", 0) * 2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof org.bukkit.entity.Item caught)) return;
        Map<String, Integer> values = getEnchants(event.getPlayer().getInventory().getItemInMainHand());
        int bounty = values.getOrDefault("ocean_bounty", 0);
        if (bounty > 0 && random.nextDouble() < bounty * 0.01D) {
            caught.getWorld().dropItemNaturally(caught.getLocation(), caught.getItemStack().clone());
        }
        int progress = values.getOrDefault("quick_reel", 0);
        if (progress > 0) event.setExpToDrop(event.getExpToDrop() + progress * 2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Map<String, Integer> enchants = getEnchants(killer.getInventory().getItemInMainHand());
        int looting = enchants.getOrDefault("looting", 0);
        if (looting <= 0 || event.getDrops().isEmpty()) return;
        if (random.nextDouble() > Math.min(0.60D, looting * 0.08D)) return;
        ItemStack drop = event.getDrops().get(random.nextInt(event.getDrops().size()));
        if (drop != null && !drop.getType().isAir()) event.getDrops().add(drop.clone());
    }

    public ApplyResult applyEnchant(ItemStack target, String id, int level) {
        if (isLoreRewriteBlocked(target)) {
            ensureDeveloperNotebookSpecialLine(target);
            return new ApplyResult(false, "&cThat item cannot be enchanted.");
        }
        CustomEnchant enchant = get(id);
        if (enchant == null) return new ApplyResult(false, "&cUnknown custom enchant.");
        if (!appliesTo(target, enchant)) return new ApplyResult(false, "&cThat enchant cannot be applied to this item.");
        String conflict = conflict(target, enchant.id());
        if (conflict != null) return new ApplyResult(false, "&cThis enchant conflicts with " + plainName(conflict) + ".");
        Map<String, Integer> current = getEnchants(target);
        int next = Math.max(current.getOrDefault(enchant.id(), 0), Math.clamp(level, 1, enchant.maxLevel()));
        current.put(enchant.id(), next);
        writeEnchants(target, current);
        return new ApplyResult(true, enchant.name() + " " + roman(next));
    }

    public ApplyResult setEnchantLevel(ItemStack target, String id, int level) {
        if (isLoreRewriteBlocked(target)) {
            ensureDeveloperNotebookSpecialLine(target);
            return new ApplyResult(false, "&cThat item cannot be enchanted.");
        }
        CustomEnchant enchant = get(id);
        if (enchant == null) return new ApplyResult(false, "&cUnknown custom enchant.");
        if (!appliesTo(target, enchant)) return new ApplyResult(false, "&cThat enchantment cannot be applied to this item type.");
        Map<String, Integer> current = getEnchants(target);
        if (level <= 0) {
            current.remove(enchant.id());
            writeEnchants(target, current);
            return new ApplyResult(true, enchant.name() + " removed");
        }
        String conflict = conflict(target, enchant.id());
        if (conflict != null && !current.containsKey(enchant.id())) {
            return new ApplyResult(false, "&cThis enchant conflicts with " + plainName(conflict) + ".");
        }
        int safeLevel = Math.clamp(level, 1, enchant.maxLevel());
        current.put(enchant.id(), safeLevel);
        writeEnchants(target, current);
        return new ApplyResult(true, enchant.name() + " " + roman(safeLevel));
    }

    public boolean cleanVanillaEnchants(ItemStack item) {
        if (isLoreRewriteBlocked(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return false;
        }
        if (!isMmoCustomItem(item)) return false;
        Map<String, Integer> current = getEnchants(item);
        Map<String, Integer> vanilla = mmoEnchantStorage.readVanillaEnchants(item);
        vanilla.forEach((id, level) -> {
            CustomEnchant enchant = get(id);
            if (enchant != null && appliesTo(item, enchant)) current.merge(enchant.id(), Math.clamp(level, 1, enchant.maxLevel()), Math::max);
        });
        boolean removed = mmoEnchantStorage.removeVanillaEnchants(item);
        if (removed || !vanilla.isEmpty()) writeEnchants(item, current);
        return removed || !vanilla.isEmpty();
    }

    private boolean appliesTo(ItemStack item, CustomEnchant enchant) {
        if (item == null || item.getType() == Material.AIR || isEnchantTargetBlocked(item)) return false;
        CustomEnchantDefaults100.Definition definition = requestedDefinitions.get(enchant.id());
        if (definition != null && !definition.enabled()) return false;
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return true;
        if (definition != null) return definition.allowedItemTypes().stream().anyMatch(type -> matchesItemType(item, type));
        String material = item.getType().name();
        club.abelix.games.fakevival.mmo.MMOItemType itemType = mmoStatStorage.readItemType(item);
        return switch (enchant.type()) {
            case "weapon" -> material.endsWith("_SWORD") || material.endsWith("_AXE") || material.equals("MACE") || material.equals("TRIDENT")
                    || switch (itemType) { case WEAPON, SWORD, KATANA, DAGGER, SPEAR, AXE, STAFF, WAND -> true; default -> false; };
            case "armor" -> material.endsWith("_HELMET") || material.endsWith("_CHESTPLATE") || material.endsWith("_LEGGINGS") || material.endsWith("_BOOTS")
                    || itemType == club.abelix.games.fakevival.mmo.MMOItemType.ARMOR || itemType == club.abelix.games.fakevival.mmo.MMOItemType.SHIELD;
            case "tool" -> material.endsWith("_PICKAXE") || material.endsWith("_AXE") || material.endsWith("_SHOVEL") || material.endsWith("_HOE")
                    || itemType == club.abelix.games.fakevival.mmo.MMOItemType.TOOL || itemType == club.abelix.games.fakevival.mmo.MMOItemType.FARMING_EQUIPMENT;
            case "bow" -> material.equals("BOW") || material.equals("CROSSBOW") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.BOW;
            case "fishing" -> material.equals("FISHING_ROD") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.FISHING_ROD || itemType == club.abelix.games.fakevival.mmo.MMOItemType.ROD;
            case "accessory" -> switch (itemType) { case ACCESSORY, RING, NECKLACE, TRINKET -> true; default -> false; };
            case "all" -> switch (itemType) { case MATERIAL, FOOD, POTION, CONSUMABLE, FISHING_BAIT, BAIT -> false; default -> true; };
            default -> false;
        };
    }

    private boolean matchesItemType(ItemStack item, String allowed) {
        String material = item.getType().name();
        ItemMeta meta = item.getItemMeta();
        String raw = meta == null ? "" : Objects.requireNonNullElse(meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING), "").toUpperCase(Locale.ROOT);
        club.abelix.games.fakevival.mmo.MMOItemType itemType = mmoStatStorage.readItemType(item);
        return switch (allowed) {
            case "SWORD" -> material.endsWith("_SWORD") || raw.equals("SWORD") || raw.equals("GREATSWORD") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.SWORD;
            case "KATANA" -> raw.equals("KATANA") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.KATANA;
            case "DAGGER" -> raw.equals("DAGGER") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.DAGGER;
            case "BOW", "SHORTBOW", "LONGBOW" -> material.equals("BOW") || raw.equals(allowed) || itemType == club.abelix.games.fakevival.mmo.MMOItemType.BOW;
            case "CROSSBOW" -> material.equals("CROSSBOW") || raw.equals("CROSSBOW") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.CROSSBOW;
            case "FISHING_ROD" -> material.equals("FISHING_ROD") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.FISHING_ROD || itemType == club.abelix.games.fakevival.mmo.MMOItemType.ROD;
            case "PICKAXE" -> material.endsWith("_PICKAXE") || raw.equals("PICKAXE");
            case "DRILL" -> raw.equals("DRILL");
            case "AXE" -> material.endsWith("_AXE") || raw.equals("AXE") || raw.equals("FORAGING_AXE") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.AXE;
            case "SHOVEL" -> material.endsWith("_SHOVEL") || raw.equals("SHOVEL");
            case "HOE", "FARMING_HOE" -> material.endsWith("_HOE") || raw.equals("HOE") || raw.equals("FARMING_HOE") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.FARMING_EQUIPMENT;
            case "ARMOR" -> isArmorMaterial(material) || itemType == club.abelix.games.fakevival.mmo.MMOItemType.ARMOR;
            case "HELMET" -> material.endsWith("_HELMET") || raw.equals("HELMET");
            case "CHESTPLATE" -> material.endsWith("_CHESTPLATE") || raw.equals("CHESTPLATE");
            case "LEGGINGS" -> material.endsWith("_LEGGINGS") || raw.equals("LEGGINGS");
            case "BOOTS" -> material.endsWith("_BOOTS") || raw.equals("BOOTS");
            case "WAND" -> raw.equals("WAND") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.WAND;
            case "STAFF" -> raw.equals("STAFF") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.STAFF;
            case "ACCESSORY" -> switch (itemType) {
                case ACCESSORY, RING, NECKLACE, TRINKET, CHARM, TOTEM -> true;
                default -> raw.equals("ACCESSORY");
            };
            case "UTILITY" -> raw.equals("UTILITY") || itemType == club.abelix.games.fakevival.mmo.MMOItemType.UTILITY;
            default -> false;
        };
    }

    private boolean isArmorMaterial(String material) {
        return material.endsWith("_HELMET") || material.endsWith("_CHESTPLATE") || material.endsWith("_LEGGINGS") || material.endsWith("_BOOTS");
    }

    public boolean isEnchantTargetBlocked(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return false;
        if (isLoreRewriteBlocked(item)) return true;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        String id = Objects.requireNonNullElse(pdc.get(customItemKey, PersistentDataType.STRING), "").toLowerCase(Locale.ROOT);
        String raw = Objects.requireNonNullElse(pdc.get(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING), "").toUpperCase(Locale.ROOT);
        if (id.startsWith("admin_") || id.contains("_test_") || id.endsWith("_preview")) return true;
        Set<String> blocked = Set.of("MATERIAL", "COLLECTION_MATERIAL", "RECIPE_MATERIAL", "COOKING_INGREDIENT",
                "FOOD", "CUSTOM_FOOD", "FISH", "CUSTOM_FISH", "FISHING_BAIT", "BAIT", "TOKEN", "EVENT_TOKEN",
                "COSMETIC", "FURNITURE", "QUEST_ITEM", "DECORATION", "COMPANION", "CURRENCY", "KEY", "LOOT_BOX",
                "POTION", "CONSUMABLE", "PREVIEW", "ADMIN", "TEST");
        if (blocked.contains(raw) || raw.endsWith("_MATERIAL") || raw.endsWith("_INGREDIENT")) return true;
        for (String flag : List.of("test_item", "admin_only", "quest_item", "cosmetic_item", "furniture_item", "preview")) {
            if (Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, flag), PersistentDataType.BYTE))) return true;
        }
        return false;
    }

    public String conflict(ItemStack item, String id) {
        String normalized = normalize(id);
        Map<String, Integer> current = getEnchants(item);
        if ((normalized.equals("sharpness") || normalized.equals("sharpfang")) && (current.containsKey("smite") || current.containsKey("smitebound"))) return current.containsKey("smite") ? "smite" : "smitebound";
        if ((normalized.equals("smite") || normalized.equals("smitebound")) && (current.containsKey("sharpness") || current.containsKey("sharpfang"))) return current.containsKey("sharpness") ? "sharpness" : "sharpfang";
        if ((normalized.equals("fortune") || normalized.equals("smelting_touch")) && current.containsKey("silk_touch")) return "Silk Touch";
        if (normalized.equals("silk_touch") && (current.containsKey("fortune") || current.containsKey("smelting_touch"))) {
            return current.containsKey("fortune") ? "Fortune" : "Smelting Touch";
        }
        String requested = requestedConflict(current, normalized);
        if (requested != null) return requested;
        return null;
    }

    private Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        mmoEnchantStorage.read(item).forEach((id, level) -> {
            if (enchants.containsKey(id)) result.put(id, level);
        });
        return result;
    }

    private void writeEnchants(ItemStack item, Map<String, Integer> values) {
        writeEnchants(item, values, true);
    }

    private void writeEnchants(ItemStack item, Map<String, Integer> values, boolean rebuildLore) {
        if (isLoreRewriteBlocked(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        Map<String, Integer> cleanValues = new LinkedHashMap<>();
        values.forEach((id, level) -> {
            CustomEnchant enchant = get(id);
            if (enchant != null && level != null && level > 0) cleanValues.put(enchant.id(), Math.clamp(level, 1, enchant.maxLevel()));
        });
        mmoEnchantStorage.read(item).forEach((id, level) -> {
            if (!enchants.containsKey(id) && level != null && level > 0) cleanValues.put(id, level);
        });
        item.setItemMeta(meta);
        mmoEnchantStorage.write(item, cleanValues);
        mmoEnchantStorage.removeVanillaEnchants(item);
        meta = item.getItemMeta();
        if (meta == null) return;
        if (!cleanValues.isEmpty()) meta.setEnchantmentGlintOverride(true);
        applyMmoTooltipHiding(meta);
        if (!rebuildLore) {
            item.setItemMeta(meta);
            return;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        String originalName = originalItemName(item, meta);
        String rarity = itemRarity(meta);
        meta.setDisplayName(mmoLoreBuilder.displayName(originalName, rarity));
        MMOStatMap displayedStats = mmoStatStorage.read(item);
        displayedStats.addAll(statBonuses(item));
        meta.setLore(mmoLoreBuilder.rebuild(lore, originalName, rarity, displayedStats, enchantLoreLines(cleanValues), abilityName(meta, lore), abilityManaCost(meta, lore)));
        item.setItemMeta(meta);
    }

    private boolean isLoreRewriteBlocked(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(customItemKey, PersistentDataType.STRING);
        String type = pdc.get(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING);
        if ("developer_notebook".equalsIgnoreCase(id) || "SPECIAL".equalsIgnoreCase(type)) return true;
        for (String flag : List.of("developer_notebook", "special_item", "enchant_blocked", "shop_preview_item", "preview_item", "admin_item")) {
            if (Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, flag), PersistentDataType.BYTE))) return true;
        }
        return false;
    }

    private void ensureDeveloperNotebookSpecialLine(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> {
            String plain = org.bukkit.ChatColor.stripColor(line);
            return plain != null && plain.equalsIgnoreCase("SPECIAL");
        });
        while (!lore.isEmpty() && lore.getLast().isBlank()) lore.removeLast();
        lore.add("");
        lore.add(CC.color("&c&lSPECIAL"));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void applyMmoTooltipHiding(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        addItemFlagIfPresent(meta, "HIDE_ADDITIONAL_TOOLTIP");
        addItemFlagIfPresent(meta, "HIDE_DYE");
        addItemFlagIfPresent(meta, "HIDE_ARMOR_TRIM");
        invokeMetaBoolean(meta, "setHideTooltip", false);
        invokeMetaBoolean(meta, "setHideAdditionalTooltip", true);
        invokeMetaBoolean(meta, "setHideUnbreakable", true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addItemFlagIfPresent(ItemMeta meta, String name) {
        try {
            meta.addItemFlags((ItemFlag) Enum.valueOf((Class<Enum>) (Class) ItemFlag.class, name));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void invokeMetaBoolean(ItemMeta meta, String methodName, boolean value) {
        try {
            meta.getClass().getMethod(methodName, boolean.class).invoke(meta, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Map<String, String> enchantLoreLines(Map<String, Integer> values) {
        Map<String, String> lines = new LinkedHashMap<>();
        values.entrySet().stream()
                .filter(entry -> enchants.containsKey(entry.getKey()))
                .sorted(Comparator.comparing(entry -> enchants.get(entry.getKey()).name()))
                .forEach(entry -> {
                    CustomEnchant enchant = enchants.get(entry.getKey());
                    if (enchant != null) {
                        String value = enchant.name() + " " + roman(entry.getValue());
                        Map<MMOStatType, Double> bonuses = statBonusesFor(entry.getKey(), entry.getValue());
                        if (!bonuses.isEmpty()) {
                            value += " &8[&7" + bonuses.entrySet().stream()
                                    .map(bonus -> bonus.getKey().color() + "+" + formatStat(bonus.getValue())
                                            + (bonus.getKey().percent() ? "% " : " ") + bonus.getKey().display())
                                    .collect(java.util.stream.Collectors.joining("&7, ")) + "&8]";
                        }
                        lines.put(enchant.name(), value);
                    }
                });
        return lines;
    }

    private boolean isMmoCustomItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && (mmoStatStorage.hasStats(item)
                || item.getItemMeta().getPersistentDataContainer().has(customItemKey, PersistentDataType.STRING));
    }

    private String originalItemName(ItemStack item, ItemMeta meta) {
        String id = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        String configured = id == null ? null : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("custom-items.items." + id + ".name");
        String name = configured == null || configured.isBlank()
                ? (meta.hasDisplayName() ? meta.getDisplayName() : pretty(id == null ? item.getType().name() : id))
                : configured;
        String clean = org.bukkit.ChatColor.stripColor(CC.color(name));
        return clean == null || clean.isBlank() ? pretty(id == null ? item.getType().name() : id) : clean;
    }

    private String itemRarity(ItemMeta meta) {
        String rolled = meta.getPersistentDataContainer().get(itemRarityKey, PersistentDataType.STRING);
        if (rolled != null && !rolled.isBlank()) return rolled;
        String explicit = meta.getPersistentDataContainer().get(customRarityKey, PersistentDataType.STRING);
        if (explicit != null && !explicit.isBlank()) return explicit;
        if (meta.hasLore() && meta.getLore() != null) {
            for (int i = meta.getLore().size() - 1; i >= 0; i--) {
                String clean = org.bukkit.ChatColor.stripColor(CC.color(meta.getLore().get(i)));
                if (clean != null && clean.toUpperCase(Locale.ROOT).matches("(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|DIVINE).*")) {
                    return clean.split(" ")[0];
                }
            }
        }
        return "COMMON";
    }

    private String formatStat(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? String.valueOf((long) Math.rint(value)) : String.format(Locale.US, "%.1f", value);
    }

    private String abilityName(ItemMeta meta, List<String> lore) {
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        if (ability.has("id")) return pretty(ability.get("id").getAsString());
        if (lore == null) return null;
        for (String line : lore) {
            String clean = org.bukkit.ChatColor.stripColor(CC.color(line));
            if (clean != null && clean.startsWith("Ability: ")) return clean.substring("Ability: ".length());
        }
        return null;
    }

    private Integer abilityManaCost(ItemMeta meta, List<String> lore) {
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        if (ability.has("mana_cost") && ability.get("mana_cost").isJsonPrimitive() && ability.get("mana_cost").getAsJsonPrimitive().isNumber()) {
            return ability.get("mana_cost").getAsInt();
        }
        if (lore == null) return null;
        for (String line : lore) {
            String clean = org.bukkit.ChatColor.stripColor(CC.color(line));
            if (clean != null && clean.startsWith("Mana Cost: ")) {
                try {
                    return Integer.parseInt(clean.substring("Mana Cost: ".length()).trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private JsonObject jsonObject(String raw) {
        if (raw == null || raw.isBlank()) return new JsonObject();
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private boolean isCustomEnchantBook(ItemStack item) {
        return getBookEnchant(item) != null;
    }

    private Map<String, Integer> vanillaEnchants(Map<Enchantment, Integer> vanilla) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (vanilla == null || vanilla.isEmpty()) return result;
        vanilla.forEach((enchantment, level) -> {
            String id = MMOEnchantStorage.fromVanilla(enchantment);
            if (id != null && level != null && level > 0) result.merge(id, level, Math::max);
        });
        return result;
    }

    private Map<String, Integer> enchantmentsFromBookOrItem(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String bookEnchant = getBookEnchant(item);
        if (bookEnchant != null) {
            result.put(bookEnchant, getBookLevel(item));
            return result;
        }
        result.putAll(getEnchants(item));
        if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            result.putAll(vanillaEnchants(storageMeta.getStoredEnchants()));
        }
        if (result.isEmpty()) result.putAll(mmoEnchantStorage.readVanillaEnchants(item));
        return result;
    }

    private String conflictWithMap(Map<String, Integer> current, String id) {
        String normalized = normalize(id);
        if ((normalized.equals("sharpness") || normalized.equals("sharpfang")) && (current.containsKey("smite") || current.containsKey("smitebound"))) return current.containsKey("smite") ? "smite" : "smitebound";
        if ((normalized.equals("smite") || normalized.equals("smitebound")) && (current.containsKey("sharpness") || current.containsKey("sharpfang"))) return current.containsKey("sharpness") ? "sharpness" : "sharpfang";
        if ((normalized.equals("fortune") || normalized.equals("smelting_touch")) && current.containsKey("silk_touch")) return "silk_touch";
        if (normalized.equals("silk_touch") && (current.containsKey("fortune") || current.containsKey("smelting_touch"))) {
            return current.containsKey("fortune") ? "fortune" : "smelting_touch";
        }
        String requested = requestedConflict(current, normalized);
        if (requested != null) return requested;
        return null;
    }

    private String requestedConflict(Map<String, Integer> current, String id) {
        CustomEnchantDefaults100.Definition definition = requestedDefinitions.get(id);
        if (definition != null) {
            for (String conflict : definition.conflicts()) if (current.containsKey(normalize(conflict))) return normalize(conflict);
        }
        for (String currentId : current.keySet()) {
            CustomEnchantDefaults100.Definition other = requestedDefinitions.get(currentId);
            if (other != null && other.conflicts().stream().map(this::normalize).anyMatch(id::equals)) return currentId;
        }
        return null;
    }

    private Map<String, Integer> equippedArmorEnchants(Player player) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            getEnchants(armor).forEach((id, level) -> result.merge(id, level, Integer::sum));
        }
        return result;
    }

    private void addDamage(EntityDamageByEntityEvent event, Map<String, Integer> enchants, String id, double perLevel) {
        int level = enchants.getOrDefault(id, 0);
        if (level > 0 && perLevel > 0.0D) event.setDamage(event.getDamage() + (perLevel * level));
    }

    private void reduceDamage(EntityDamageEvent event, Map<String, Integer> enchants, String id, double perLevel) {
        int level = enchants.getOrDefault(id, 0);
        if (level > 0 && perLevel > 0.0D) event.setDamage(event.getDamage() * Math.max(0.15D, 1.0D - (perLevel * level)));
    }

    private void multiplyDamage(EntityDamageByEntityEvent event, Map<String, Integer> enchants, String id, double perLevel) {
        int level = enchants.getOrDefault(id, 0);
        if (level > 0 && perLevel > 0.0D) event.setDamage(event.getDamage() * (1.0D + perLevel * level));
    }

    private void chanceEffect(Map<String, Integer> enchants, String id, double perLevel, java.util.function.IntConsumer effect) {
        int level = enchants.getOrDefault(id, 0);
        if (level > 0 && random.nextDouble() < perLevel * level) effect.accept(level);
    }

    private String getBookEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(bookKey, PersistentDataType.STRING);
    }

    private int getBookLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(bookLevelKey, PersistentDataType.INTEGER);
        return level == null ? 1 : level;
    }

    private void consumeOne(ItemStack item) {
        item.setAmount(item.getAmount() - 1);
    }

    private void autoSmelt(Block block) {
        Material result = switch (block.getType()) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            default -> null;
        };
        if (result == null) return;
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(result));
        block.setType(Material.AIR);
    }

    private boolean isUndead(LivingEntity entity) {
        return switch (entity.getType()) {
            case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED, SKELETON, STRAY, WITHER_SKELETON, WITHER, ZOMBIFIED_PIGLIN, PHANTOM, ZOGLIN -> true;
            default -> false;
        };
    }

    private boolean isBoss(LivingEntity entity) {
        String type = entity.getType().name();
        return type.equals("ENDER_DRAGON") || type.equals("WITHER") || type.equals("WARDEN")
                || entity.getScoreboardTags().stream().anyMatch(tag -> tag.equalsIgnoreCase("boss") || tag.equalsIgnoreCase("smp_boss"));
    }

    private boolean isDungeon(LivingEntity entity) {
        return entity.getWorld().getName().toLowerCase(Locale.ROOT).contains("dungeon")
                || entity.getScoreboardTags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains("dungeon"));
    }

    private void applyPassiveEffects() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Map<String, Integer> values = equippedArmorEnchants(player);
            int rejuvenate = values.getOrDefault("rejuvenate_custom", 0);
            if (rejuvenate > 0 && player.getHealth() > 0.0D) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + player.getMaxHealth() * rejuvenate * 0.002D));
            }
            int night = values.getOrDefault("night_hunter", 0);
            if (night > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false, false));
            int jump = values.getOrDefault("jump_boost_custom", 0);
            if (jump > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, Math.min(2, jump - 1), true, false, false));
            int stride = values.getOrDefault("long_stride", 0);
            if (stride > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, Math.min(2, (stride - 1) / 2), true, false, false));
            int frost = values.getOrDefault("frost_ward", 0);
            if (frost > 0) {
                player.setFreezeTicks(Math.max(0, player.getFreezeTicks() - frost * 10));
                PotionEffect slow = player.getPotionEffect(PotionEffectType.SLOWNESS);
                if (slow != null && slow.getDuration() > 20) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Math.max(1, slow.getDuration() - frost * 5), slow.getAmplifier(), slow.isAmbient(), slow.hasParticles(), slow.hasIcon()), true);
                }
            }
        }
    }

    private void registerDefaults() {
        add("sharpness", "Sharpness", "weapon", 5, "&9");
        add("smite", "Smite", "weapon", 5, "&9");
        add("critical", "Critical", "weapon", 5, "&9");
        add("wisdom", "Wisdom", "all", 5, "&b");
        add("first_strike", "First Strike", "weapon", 5, "&9");
        add("execute", "Execute", "weapon", 5, "&9");
        add("sweeping_edge", "Sweeping Edge", "weapon", 3, "&9");
        add("fire_aspect", "Fire Aspect", "weapon", 2, "&9");
        add("knockback", "Knockback", "weapon", 2, "&9");
        add("looting", "Looting", "weapon", 5, "&9");
        add("experience", "Experience", "weapon", 5, "&9");
        add("vampirism", "Vampirism", "weapon", 5, "&9");
        add("power", "Power", "bow", 5, "&9");
        add("punch", "Punch", "bow", 2, "&9");
        add("flame", "Flame", "bow", 1, "&9");
        add("infinity", "Infinity", "bow", 1, "&9");
        add("snipe", "Snipe", "bow", 5, "&9");
        add("piercing", "Piercing", "bow", 5, "&9");
        add("infinite_quiver", "Infinite Quiver", "bow", 5, "&9");
        add("protection", "Protection", "armor", 5, "&9");
        add("growth", "Growth", "armor", 5, "&9");
        add("thorns", "Thorns", "armor", 5, "&9");
        add("feather_falling", "Feather Falling", "armor", 4, "&9");
        add("respiration", "Respiration", "armor", 3, "&9");
        add("aqua_affinity", "Aqua Affinity", "armor", 1, "&9");
        add("rejuvenate", "Rejuvenate", "armor", 5, "&9");
        add("speed", "Speed", "armor", 5, "&9");
        add("unbreaking", "Unbreaking", "all", 5, "&9");
        add("mending", "Mending", "all", 1, "&9");
        add("efficiency", "Efficiency", "tool", 5, "&9");
        add("fortune", "Fortune", "tool", 5, "&9");
        add("silk_touch", "Silk Touch", "tool", 1, "&9");
        add("smelting_touch", "Smelting Touch", "tool", 1, "&9");
        add("telekinesis", "Telekinesis", "tool", 1, "&9");
        add("compact", "Compact", "tool", 5, "&9");
        add("luck_of_the_sea", "Luck of the Sea", "fishing", 5, "&9");
        add("lure", "Lure", "fishing", 5, "&9");
        add("angler", "Angler", "fishing", 5, "&9");
        add("blessing", "Blessing", "fishing", 5, "&9");
        add("sharpfang", "Sharpfang", "weapon", 5, "&c");
        add("lifesteal", "Lifesteal", "weapon", 3, "&c");
        add("venom", "Venom", "weapon", 4, "&2");
        add("frostbite", "Frostbite", "weapon", 4, "&b");
        add("inferno", "Inferno", "weapon", 4, "&6");
        add("thunderstrike", "Thunderstrike", "weapon", 3, "&e");
        add("witherbrand", "Witherbrand", "weapon", 3, "&8");
        add("executioner", "Executioner", "weapon", 5, "&4");
        add("giant_slayer", "Giant Slayer", "weapon", 5, "&d");
        add("dragonbane", "Dragonbane", "weapon", 5, "&5");
        add("enderbane", "Enderbane", "weapon", 5, "&5");
        add("smitebound", "Smitebound", "weapon", 5, "&f");
        add("combo", "Combo", "weapon", 5, "&6");
        add("stun", "Stun", "weapon", 3, "&7");
        add("cleave", "Cleave", "weapon", 4, "&c");
        add("guard", "Guard", "armor", 5, "&9");
        add("stone_skin", "Stone Skin", "armor", 5, "&7");
        add("fireguard", "Fireguard", "armor", 4, "&6");
        add("blastguard", "Blastguard", "armor", 4, "&e");
        add("featherweight", "Featherweight", "armor", 4, "&f");
        add("last_stand", "Last Stand", "armor", 4, "&c");
        add("swiftstep", "Swiftstep", "armor", 3, "&b");
        add("nightcloak", "Nightcloak", "armor", 1, "&8");
        add("thornsoul", "Thornsoul", "armor", 3, "&d");
        add("prospector", "Prospector", "tool", 5, "&e");
        add("autosmelt", "Autosmelt", "tool", 1, "&6");
        add("veinburst", "Veinburst", "tool", 3, "&7");
        add("timber", "Timber", "tool", 3, "&2");
        add("harvester", "Harvester", "tool", 4, "&a");
        add("replanter", "Replanter", "tool", 1, "&a");
        add("magnet", "Magnet", "tool", 3, "&b");
        add("durability", "Durability", "tool", 5, "&f");
        add("scholar", "Scholar", "tool", 4, "&d");
        add("precision", "Precision", "bow", 5, "&e");
        add("volley", "Volley", "bow", 3, "&a");
        add("piercer", "Piercer", "bow", 4, "&9");
        add("snare", "Snare", "bow", 3, "&b");
        add("flameburst", "Flameburst", "bow", 3, "&6");
    }

    private void registerRequestedDefaults() {
        invalidRequestedDefinitions.clear();
        invalidRequestedDefinitions.addAll(CustomEnchantDefaults100.invalidDefinitions());
        for (CustomEnchantDefaults100.InvalidDefinition invalid : invalidRequestedDefinitions) {
            plugin.getLogger().warning("[SMP Enchants] Skipped default enchant " + invalid.id() + ": " + invalid.reason());
        }
        for (CustomEnchantDefaults100.Definition definition : CustomEnchantDefaults100.definitions()) {
            requestedDefinitions.put(definition.id(), definition);
            enchants.putIfAbsent(definition.id(), new CustomEnchant(definition.id(), definition.name(),
                    definition.type(), definition.maxLevel(), rarityColor(definition.rarity()), definition.statBonuses()));
        }
    }

    private void add(String id, String name, String type, int maxLevel, String color) {
        enchants.put(id, new CustomEnchant(id, name, type, maxLevel, color, defaultStatBonuses(id)));
    }

    private Map<MMOStatType, Double> defaultStatBonuses(String id) {
        return switch (normalize(id)) {
            case "sharpness", "sharpfang", "first_strike", "execute" -> Map.of(MMOStatType.DAMAGE, 5.0D, MMOStatType.STRENGTH, 2.0D);
            case "critical", "precision" -> Map.of(MMOStatType.CRIT_CHANCE, 1.0D, MMOStatType.CRIT_DAMAGE, 5.0D);
            case "wisdom" -> Map.of(MMOStatType.MANA, 10.0D, MMOStatType.INTELLIGENCE, 5.0D);
            case "protection", "guard", "stone_skin" -> Map.of(MMOStatType.DEFENSE, 5.0D, MMOStatType.HEALTH, 8.0D);
            case "growth", "rejuvenate" -> Map.of(MMOStatType.HEALTH, 15.0D);
            case "efficiency", "prospector", "compact" -> Map.of(MMOStatType.MINING_SPEED, 12.0D);
            case "fortune" -> Map.of(MMOStatType.MINING_FORTUNE, 5.0D);
            case "harvester", "replanter" -> Map.of(MMOStatType.FARMING_FORTUNE, 6.0D);
            case "timber" -> Map.of(MMOStatType.FORAGING_FORTUNE, 6.0D);
            case "lure", "angler" -> Map.of(MMOStatType.FISHING_SPEED, 5.0D);
            case "luck_of_the_sea", "blessing" -> Map.of(MMOStatType.FISHING_FORTUNE, 5.0D);
            default -> Map.of();
        };
    }

    private String rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> "&a";
            case "RARE" -> "&9";
            case "EPIC" -> "&5";
            case "LEGENDARY" -> "&6";
            case "MYTHIC" -> "&d";
            default -> "&f";
        };
    }

    private String inferredRarity(CustomEnchant enchant) {
        if (enchant.color().equals("&d")) return "MYTHIC";
        if (enchant.color().equals("&6")) return "LEGENDARY";
        if (enchant.color().equals("&5")) return "EPIC";
        if (enchant.color().equals("&9") || enchant.color().equals("&b")) return "RARE";
        if (enchant.color().equals("&a")) return "UNCOMMON";
        return "COMMON";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "";
        String[] parts = normalize(text).split("_");
        List<String> out = new ArrayList<>();
        for (String part : parts) if (!part.isBlank()) out.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        return String.join(" ", out);
    }

    private double combatHealth(LivingEntity target) {
        if (plugin instanceof club.abelix.games.fakevival.Game game
                && game.getSmpMobEngine() != null
                && game.getSmpMobEngine().isEngineMob(target)) {
            return game.getSmpMobEngine().getCurrentHp(target);
        }
        return target.getHealth();
    }

    private double combatMaxHealth(LivingEntity target) {
        if (plugin instanceof club.abelix.games.fakevival.Game game
                && game.getSmpMobEngine() != null
                && game.getSmpMobEngine().isEngineMob(target)) {
            return game.getSmpMobEngine().getMaxHp(target);
        }
        return target.getMaxHealth();
    }

    public String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(value);
        };
    }

    public record CustomEnchant(String id, String name, String type, int maxLevel, String color,
                                Map<MMOStatType, Double> statBonusesPerLevel) {
        public CustomEnchant {
            statBonusesPerLevel = statBonusesPerLevel == null ? Map.of() : Map.copyOf(statBonusesPerLevel);
        }
    }

    public record ApplyResult(boolean success, String message) {
    }

    public record RequestedValidation(int registered, int metadataWarnings, int handlerWarnings, int statWarnings,
                                      int disabled, int invalidDefinitions, int skippedDefinitions,
                                      List<CustomEnchantDefaults100.InvalidDefinition> invalidDefinitionDetails,
                                      boolean uniqueBatch) {
        public int warnings() {
            return metadataWarnings + handlerWarnings + statWarnings;
        }
    }

    private record HitSequence(UUID target, long lastHit, int count) {
    }
}
