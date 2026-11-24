package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;

public final class CollectionManager {
    private static final Set<String> HIDDEN_AGGREGATE_COLLECTIONS = Set.of("fish", "log", "logs", "monster", "combat");
    private static final Map<String, String> CATEGORY_OVERRIDES = Map.ofEntries(
            Map.entry("iron", "mining"),
            Map.entry("string", "combat"),
            Map.entry("wheat", "farming"),
            Map.entry("oak_log", "foraging"),
            Map.entry("bone", "combat"),
            Map.entry("cod", "fishing"),
            Map.entry("redstone", "mining"),
            Map.entry("cobblestone", "mining")
    );
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private RecipeManager recipeManager;
    private volatile Map<Material,List<String>> vanillaCollectionLinks=Map.of();

    public CollectionManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void setRecipeManager(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        rebuildVanillaCollectionLinks();
    }

    public List<String> getCollectionsRelatedTo(Material material){return vanillaCollectionLinks.getOrDefault(material,List.of());}
    public String getPrimaryCollectionId(Material material){List<String> ids=getCollectionsRelatedTo(material);return ids.isEmpty()?primaryCollection(material):ids.getFirst();}
    public String externalCollectionId(String id){String value=normalize(id);return value.endsWith("_collection")?value:value+"_collection";}
    public String internalCollectionId(String id){String value=normalize(id);return value.endsWith("_collection")?value.substring(0,value.length()-11):value;}
    public int vanillaLinkCount(){return vanillaCollectionLinks.values().stream().mapToInt(List::size).sum();}
    public void rebuildVanillaCollectionLinks(){
        Map<Material,List<String>> links=new LinkedHashMap<>();
        // Explicit Java registry paths take precedence over the legacy one-material/one-id fallback.
        Set<String> explicitPaths=new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.collectionIds());
        explicitPaths.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.collectionIds());
        explicitPaths.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.collectionIds());
        explicitPaths.addAll(club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.collectionIds());
        explicitPaths.addAll(club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.collectionIds());
        for(String id:explicitPaths){
            for(String configured:getTrackedMaterials(id)){
                Material material=Material.matchMaterial(configured);if(material==null)continue;
                List<String> values=new ArrayList<>(links.getOrDefault(material,List.of()));
                if(!values.contains(id))values.add(id);links.put(material,List.copyOf(values));
            }
        }
        for(VanillaBazaarRegistry.Entry entry:VanillaBazaarRegistry.entries()){
            String fallback=primaryCollection(entry.material());List<String> values=new ArrayList<>(links.getOrDefault(entry.material(),List.of()));
            if(!values.contains(fallback))values.add(fallback);links.put(entry.material(),List.copyOf(values));
        }
        vanillaCollectionLinks=Map.copyOf(links);
    }

    private String primaryCollection(Material material){String n=material.name();if(Set.of("RAW_IRON","IRON_INGOT","IRON_ORE","DEEPSLATE_IRON_ORE").contains(n))return "iron";if(Set.of("RAW_GOLD","GOLD_INGOT","GOLD_ORE","DEEPSLATE_GOLD_ORE","NETHER_GOLD_ORE").contains(n))return "gold";if(Set.of("RAW_COPPER","COPPER_INGOT","COPPER_ORE","DEEPSLATE_COPPER_ORE").contains(n))return "copper";if(Set.of("REDSTONE","REDSTONE_ORE","DEEPSLATE_REDSTONE_ORE").contains(n))return "redstone";if(Set.of("LAPIS_LAZULI","LAPIS_ORE","DEEPSLATE_LAPIS_ORE").contains(n))return "lapis";if(Set.of("DIAMOND","DIAMOND_ORE","DEEPSLATE_DIAMOND_ORE").contains(n))return "diamond";if(Set.of("EMERALD","EMERALD_ORE","DEEPSLATE_EMERALD_ORE").contains(n))return "emerald";if(Set.of("COAL","CHARCOAL","COAL_ORE","DEEPSLATE_COAL_ORE").contains(n))return "coal";if(Set.of("QUARTZ","NETHER_QUARTZ_ORE").contains(n))return "quartz";if(Set.of("ANCIENT_DEBRIS","NETHERITE_SCRAP","NETHERITE_INGOT").contains(n))return "netherite";if(n.endsWith("_LOG")||n.endsWith("_WOOD")||n.endsWith("_STEM")||n.endsWith("_HYPHAE"))return normalize(n);return normalize(n);}

    public ValidationResult validateVanillaLinks(){int warnings=0;Set<String> seen=new HashSet<>();for(String id:getCollectionIds()){if(!seen.add(normalize(id))||getTrackedMaterials(id).isEmpty())warnings++;for(CollectionUnlock unlock:getUnlocks(id))if(unlock.amount()<=0||(unlock.isRecipe()&&recipeManager!=null&&recipeManager.getRecipe(unlock.rewardValue()).isEmpty()))warnings++;}Set<String> requestedRecipes=new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.RequestedCollectionRecipeDefaults.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.recipeIds());if(recipeManager!=null)for(String requested:requestedRecipes){var recipe=recipeManager.getRecipe(requested).orElse(null);if(recipe==null||recipe.unlock().collectionId().isBlank()||getUnlocks(recipe.unlock().collectionId()).stream().noneMatch(unlock->requested.equals(unlock.rewardValue())))warnings++;}Set<String> requestedCollections=new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.collectionIds());requestedCollections.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.collectionIds());requestedCollections.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.collectionIds());requestedCollections.addAll(club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.collectionIds());requestedCollections.addAll(club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.collectionIds());for(String requested:requestedCollections)if(!getCollectionIds().contains(requested)||getUnlocks(requested).stream().map(CollectionUnlock::tierNumber).distinct().count()<10)warnings++;warnings+=(int)VanillaBazaarRegistry.entries().stream().filter(e->getCollectionsRelatedTo(e.material()).isEmpty()).count();return new ValidationResult(getCollectionIds().size(),vanillaLinkCount(),warnings);}
    public record ValidationResult(int collections,int vanillaLinks,int warnings){}

    public List<String> requirementLines(Player player,String collectionId){
        List<String> lines=new ArrayList<>();ConfigurationSection section=club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections.items."+normalize(collectionId)+".requirements");if(section==null)return lines;
        ConfigurationSection collect=section.getConfigurationSection("collect-material");if(collect!=null)for(String material:collect.getKeys(false)){int need=collect.getInt(material),have=getProgress(player,material);lines.add(mark(have>=need,"Collect "+need+"x "+pretty(material)));}
        if(plugin instanceof club.abelix.games.fakevival.Game game){ConfigurationSection levels=section.getConfigurationSection("skill-level");if(levels!=null)for(String skill:levels.getKeys(false)){int need=levels.getInt(skill);lines.add(mark(game.getSkillManager().getLevel(player,skill)>=need,pretty(skill)+" Level "+need));}ConfigurationSection xp=section.getConfigurationSection("skill-xp");if(xp!=null)for(String skill:xp.getKeys(false)){int need=xp.getInt(skill);lines.add(mark(game.getSkillManager().getXp(player,skill)>=need,pretty(skill)+" XP "+need));}if(section.contains("adventure-rank")){String rank=section.getString("adventure-rank","E").toUpperCase(Locale.ROOT);AdventureGuildManager.Rank needed=AdventureGuildManager.Rank.valueOf(rank);lines.add(mark(game.getAdventureGuildManager().profile(player).rank().order>=needed.order,"Adventure Rank "+needed.color+needed));}if(section.contains("player-level")){int need=section.getInt("player-level");lines.add(mark(game.getPlayerLevelManager().getLevel(player)>=need,"Player Level "+need));}if(section.contains("dungeon-completion"))lines.add(mark(game.getAdventureGuildManager().profile(player).dungeons()>0,"Dungeon Completion: "+section.getString("dungeon-completion")));if(section.contains("boss-kill"))lines.add(mark(game.getAdventureGuildManager().profile(player).bosses()>0,"Boss Defeat: "+section.getString("boss-kill")));}
        if(section.contains("recipe-unlock")&&recipeManager!=null){String recipe=section.getString("recipe-unlock","");boolean met=recipeManager.getRecipe(recipe).map(r->recipeManager.isUnlocked(player,r)).orElse(false);lines.add(mark(met,"Recipe Unlock: "+recipeManager.getDisplayName(recipe)));}return lines;
    }
    private String mark(boolean met,String text){return (met?"&a✔ ":"&c✖ ")+text;}

    public void addProgress(Player player, Material material, int amount) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("collections.enabled", true) || amount <= 0) return;
        boolean changed = false;
        for (String collectionId : getCollectionsRelatedTo(material)) {
            int before = getProgress(player, collectionId);
            int after = before + amount;
            dataManager.getData().set(progressPath(player, collectionId), after);
            notifyUnlocks(player, collectionId, before, after);
            changed = true;
        }
        if (changed) dataManager.save();
    }

    public void addCustomItemProgress(Player player, String customItemId, int amount) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("collections.enabled", true)
                || player == null || customItemId == null || amount <= 0) return;
        String materialId = normalize(customItemId);
        boolean changed = false;
        for (String collectionId : getCollectionIds()) {
            boolean tracks = getTrackedMaterials(collectionId).stream().map(this::normalize).anyMatch(materialId::equals);
            if (!tracks) continue;
            int before = getProgress(player, collectionId);
            int after = before + amount;
            dataManager.getData().set(progressPath(player, collectionId), after);
            notifyUnlocks(player, collectionId, before, after);
            changed = true;
        }
        if (changed) dataManager.save();
    }

    public int getProgress(Player player, String collectionId) {
        return dataManager.getData().getInt(progressPath(player, internalCollectionId(collectionId)), 0);
    }

    public boolean isUnlocked(Player player, String collectionId, int required) {
        return getProgress(player, collectionId) >= required;
    }

    public int getUnlockedTier(Player player, String collectionId) {
        int progress = getProgress(player, collectionId);
        int tier = 0;
        Set<String> counted = new LinkedHashSet<>();
        for (CollectionUnlock unlock : getUnlocks(collectionId)) {
            if (progress >= unlock.amount() && counted.add(unlock.tierKey())) tier++;
        }
        return tier;
    }

    public boolean isCustomItemUnlocked(Player player, String customItemId) {
        UnlockRequirement requirement = getUnlockRequirement(customItemId);
        return requirement == null || isUnlocked(player, requirement.collectionId(), requirement.required());
    }

    public UnlockRequirement getUnlockRequirement(String customItemId) {
        String target = normalize(customItemId);
        for (String collectionId : getCollectionIds()) {
            for (CollectionUnlock unlock : getUnlocks(collectionId)) {
                if (!unlock.isCustomItem()) continue;
                if (target.equals(normalize(unlock.rewardValue()))) {
                    return new UnlockRequirement(collectionId, unlock.amount());
                }
            }
        }
        return null;
    }

    public List<String> getCategoryIds() {
        return List.of("farming", "mining", "combat", "foraging", "fishing", "dungeon",
                "alchemy", "magic", "accessories", "tools", "event", "vanilla");
    }

    public List<String> getCollectionIds() {
        Set<String> ids = new LinkedHashSet<>();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections.items");
        if (section != null) for (String id : section.getKeys(false)) ids.add(normalize(id));
        ConfigurationSection root = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections");
        if (root != null) {
            for (String category : root.getKeys(false)) {
                if (category.equalsIgnoreCase("items") || category.equalsIgnoreCase("categories") || category.equalsIgnoreCase("enabled") || category.equalsIgnoreCase("rules") || category.equalsIgnoreCase("messages")) continue;
                ConfigurationSection categorySection = root.getConfigurationSection(category);
                if (categorySection != null) for (String id : categorySection.getKeys(false)) ids.add(normalize(id));
            }
        }
        club.abelix.games.fakevival.config.defaults.CustomEnchantUnlockDefaults.collectionRewards().values()
                .forEach(reward -> ids.add(normalize(reward.collectionId())));
        return ids.stream().sorted(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER).thenComparing(String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public List<String> getCollectionIdsByCategory(String categoryId) {
        String normalizedCategory = normalize(categoryId);
        List<String> collections = new ArrayList<>();
        for (String collectionId : getCollectionIds()) {
            if (HIDDEN_AGGREGATE_COLLECTIONS.contains(normalize(collectionId))) continue;
            if (normalize(getCategory(collectionId)).equals(normalizedCategory)) collections.add(collectionId);
        }
        collections.sort(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER).thenComparing(String.CASE_INSENSITIVE_ORDER));
        return collections;
    }

    public String getCategory(String collectionId) {
        String key = normalize(collectionId);
        String override = CATEGORY_OVERRIDES.get(key);
        if (override != null) return override;
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.items." + key + ".category", "");
        if (configured != null && !configured.isBlank()) return normalize(configured);
        String explicitCategory = findExplicitCategory(key);
        if (!explicitCategory.isBlank()) return explicitCategory;
        return inferCategory(key);
    }

    public String getCategoryDisplayName(String categoryId) {
        String key = normalize(categoryId);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.categories." + key + ".name", pretty(key) + " Collection");
    }

    public Material getCategoryIcon(String categoryId) {
        String key = normalize(categoryId);
        String fallback = switch (key) {
            case "farming" -> "WHEAT";
            case "mining" -> "DIAMOND_PICKAXE";
            case "combat" -> "IRON_SWORD";
            case "foraging" -> "OAK_LOG";
            case "fishing" -> "FISHING_ROD";
            case "dungeon" -> "ECHO_SHARD";
            case "alchemy" -> "BREWING_STAND";
            case "magic" -> "BLAZE_ROD";
            case "accessories" -> "EMERALD";
            case "tools" -> "IRON_PICKAXE";
            case "event" -> "NETHER_STAR";
            case "vanilla" -> "GRASS_BLOCK";
            default -> "BOOK";
        };
        return parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.categories." + key + ".icon", fallback));
    }

    public String getDisplayName(String collectionId) {
        String key = internalCollectionId(collectionId);
        String oldName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.items." + key + ".name", "");
        if (oldName != null && !oldName.isBlank()) return oldName;
        String category = findExplicitCategory(key);
        if (category.isBlank()) category = getCategory(key);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections." + category + "." + key + ".name", pretty(key) + " Collection");
    }

    public Material getIcon(String collectionId) {
        String key = internalCollectionId(collectionId);
        String icon = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.items." + key + ".icon", "");
        if (icon == null || icon.isBlank()) {
            String category = findExplicitCategory(key);
            Material direct=Material.matchMaterial(key);icon = category.isBlank() ? (direct==null?"CHEST":direct.name()) : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections." + category + "." + key + ".icon", "CHEST");
        }
        return parseMaterial(icon);
    }

    public List<String> getTrackedMaterials(String collectionId) {
        String key = internalCollectionId(collectionId);
        List<String> materials = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("collections.items." + key + ".materials");
        if (!materials.isEmpty()) return materials;
        String category = findExplicitCategory(key);
        if(!category.isBlank()){List<String> configured=club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("collections." + category + "." + key + ".materials");if(!configured.isEmpty())return configured;}
        return vanillaCollectionLinks.entrySet().stream().filter(entry->entry.getValue().contains(key)).map(entry->entry.getKey().name()).toList();
    }

    public List<CollectionUnlock> getUnlocks(String collectionId) {
        String key = internalCollectionId(collectionId);
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections.items." + key + ".unlocks");
        List<CollectionUnlock> unlocks = new ArrayList<>();
        if (section != null) {
            for (String tier : section.getKeys(false)) {
                String rewardType = normalize(section.getString(tier + ".reward-type", section.contains(tier + ".custom-item") ? "custom_item" : "perk"));
                String rewardValue = section.getString(tier + ".reward-value", section.getString(tier + ".custom-item", ""));
                String name = section.getString(tier + ".name", pretty(rewardValue == null || rewardValue.isBlank() ? tier : rewardValue));
                unlocks.add(new CollectionUnlock(tier, tierNumber(tier), section.getInt(tier + ".amount", 1), rewardType, normalizeRewardValue(rewardValue), name));
            }
        }
        String explicitCategory = findExplicitCategory(key);
        ConfigurationSection tiers = explicitCategory.isBlank() ? null : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections." + explicitCategory + "." + key + ".tiers");
        if (tiers != null) {
            for (String tier : tiers.getKeys(false)) {
                int amount = tiers.getInt(tier + ".required", tiers.getInt(tier + ".amount", 1));
                for (String reward : tiers.getStringList(tier + ".rewards")) {
                    String rewardType = "perk";
                    String rewardValue = reward;
                    int separator = reward.indexOf(':');
                    if (separator > 0) {
                        rewardType = normalize(reward.substring(0, separator));
                        rewardValue = reward.substring(separator + 1);
                    }
                    String name = "recipe".equals(rewardType) && recipeManager != null ? recipeManager.getDisplayName(rewardValue) : pretty(rewardValue);
                    unlocks.add(new CollectionUnlock(tier, tierNumber(tier), amount, rewardType, normalizeRewardValue(rewardValue), name));
                }
            }
        }
        if(unlocks.isEmpty()){int[]common={50,100,250,500,1000,2500,5000,10000,25000,50000};int[]rare={10,25,50,100,250,500,1000,2500,5000,10000};boolean isRare=Set.of("diamond","emerald","netherite").contains(key);int[]amounts=isRare?rare:common;for(int i=0;i<amounts.length;i++)unlocks.add(new CollectionUnlock("tier_"+(i+1),i+1,amounts[i],"perk","","Tier "+toRoman(i+1)));}
        club.abelix.games.fakevival.config.defaults.CustomEnchantUnlockDefaults.collectionRewards().forEach((enchantId, reward) -> {
            if (!normalize(reward.collectionId()).equals(key)) return;
            int amount = getTierRequiredFrom(unlocks, reward.tier());
            String name = plugin instanceof club.abelix.games.fakevival.Game game
                    ? game.getCustomEnchantManager().plainName(enchantId) : pretty(enchantId);
            boolean exists = unlocks.stream().anyMatch(unlock -> unlock.isEnchant() && unlock.rewardValue().equals(enchantId));
            if (!exists) unlocks.add(new CollectionUnlock("tier_" + reward.tier(), reward.tier(), amount, "enchant", enchantId, name));
        });
        unlocks.sort(Comparator.comparingInt(CollectionUnlock::amount));
        return unlocks;
    }

    private int getTierRequiredFrom(List<CollectionUnlock> unlocks, int tierNumber) {
        return unlocks.stream().filter(unlock -> unlock.tierNumber() == tierNumber)
                .mapToInt(CollectionUnlock::amount).min().orElse(1);
    }

    public int getTierRequired(String collectionId, int tierNumber) {
        for (CollectionUnlock unlock : getUnlocks(collectionId)) {
            if (unlock.tierNumber() == tierNumber) return unlock.amount();
        }
        return 1;
    }

    public void markPlayerPlaced(Location location) {
        if (location == null || location.getWorld() == null) return;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("collections.rules.block-player-placed-mining", true)) return;
        dataManager.getData().set(placedPath(location), true);
        dataManager.save();
    }

    public boolean consumePlayerPlaced(Location location) {
        if (location == null || location.getWorld() == null) return false;
        String path = placedPath(location);
        boolean placed = dataManager.getData().getBoolean(path, false);
        if (placed) {
            dataManager.getData().set(path, null);
            dataManager.save();
        }
        return placed;
    }

    private void notifyUnlocks(Player player, String collectionId, int before, int after) {
        for (CollectionUnlock unlock : getUnlocks(collectionId)) {
            if (before >= unlock.amount() || after < unlock.amount()) continue;
            if (unlock.isEnchant()) {
                player.sendMessage(CC.color("&aUnlocks Enchantment: &9" + unlock.name()));
                continue;
            }
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.messages.unlock", "&aCollection unlocked: &f%item% &7from %collection%&a!")
                    .replace("%collection%", getDisplayName(collectionId))
                    .replace("%tier%", toRoman(unlock.tierNumber()))
                    .replace("%item%", unlock.name())));
            if (unlock.isRecipe() || (unlock.isCustomItem() && recipeManager != null && recipeManager.getRecipe(unlock.customItemId()).isPresent())) {
                player.sendMessage(CC.color("&6&lCOLLECTION LEVEL UP!"));
                player.sendMessage(CC.color("&e" + stripColor(getDisplayName(collectionId)) + " " + toRoman(unlock.tierNumber())));
                player.sendMessage(CC.color("&aRecipe Unlocked: &f" + (recipeManager == null ? unlock.name() : recipeManager.getDisplayName(unlock.rewardValue()))));
            }
        }
    }

    private boolean matchesMaterial(String collectionId, Material material) {
        for (String name : getTrackedMaterials(collectionId)) {
            Material tracked = parseTrackedMaterial(name);
            if (tracked != null && tracked == material) return true;
        }
        return false;
    }

    private Material parseTrackedMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String inferCategory(String collectionId) {
        Material icon = getIcon(collectionId);
        String combined = collectionId + " " + icon.name();
        if (containsAny(combined, "wheat", "carrot", "potato", "pumpkin", "melon", "cane", "cocoa", "crop", "beetroot", "nether_wart")) return "farming";
        if (containsAny(combined, "log", "wood", "oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "foraging")) return "foraging";
        if (containsAny(combined, "fish", "rod", "cod", "salmon", "pufferfish", "fishing")) return "fishing";
        if (containsAny(combined, "zombie", "skeleton", "combat", "monster", "mob", "flesh", "bone", "spider", "creeper", "enderman", "blaze")) return "combat";
        if (containsAny(combined, "stone", "ore", "cobble", "diamond", "emerald", "iron", "gold", "coal", "lapis", "redstone", "copper", "mining")) return "mining";
        if (containsAny(combined, "potion", "herb", "alchemy", "elixir")) return "alchemy";
        if (containsAny(combined, "mana", "magic", "rune", "spell", "wand", "staff")) return "magic";
        if (containsAny(combined, "ring", "charm", "necklace", "totem", "accessory")) return "accessories";
        if (containsAny(combined, "tool", "pickaxe", "shovel", "hoe", "axe", "drill")) return "tools";
        if (containsAny(combined, "event", "festival")) return "event";
        if (containsAny(combined, "dungeon", "royal", "ancient", "void", "shadow")) return "dungeon";
        return "vanilla";
    }

    private String findExplicitCategory(String collectionId) {
        String key = normalize(collectionId);
        ConfigurationSection root = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("collections");
        if (root == null) return "";
        for (String category : root.getKeys(false)) {
            if (category.equalsIgnoreCase("items") || category.equalsIgnoreCase("categories") || category.equalsIgnoreCase("enabled") || category.equalsIgnoreCase("rules") || category.equalsIgnoreCase("messages")) continue;
            if (root.isConfigurationSection(category + "." + key)) return normalize(category);
        }
        return "";
    }

    private boolean containsAny(String text, String... parts) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String part : parts) if (lower.contains(part)) return true;
        return false;
    }

    private String progressPath(Player player, String collectionId) {
        return "players." + player.getUniqueId() + ".collections." + normalize(collectionId);
    }

    private String placedPath(Location location) {
        return "collection-placed-blocks." + location.getWorld().getUID() + "." + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ();
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.CHEST;
        }
    }

    private int tierNumber(String tier) {
        String digits = tier == null ? "" : tier.replaceAll("\\D+", "");
        if (digits.isBlank()) return 1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String normalizeRewardValue(String text) {
        String value = text == null ? "" : text;
        if (value.toLowerCase(Locale.ROOT).startsWith("recipe:")) value = value.substring("recipe:".length());
        return normalize(value);
    }

    private String stripColor(String text) {
        return org.bukkit.ChatColor.stripColor(CC.color(text == null ? "" : text));
    }

    public String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                builder.append(numerals[i]);
                number -= values[i];
            }
        }
        return builder.toString();
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Item";
        String[] parts = text.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    public record CollectionUnlock(String tierKey, int tierNumber, int amount, String rewardType, String rewardValue, String name) {
        public String customItemId() {
            return isCustomItem() ? rewardValue : "";
        }

        public boolean isCustomItem() {
            return "recipe".equals(rewardType) || "custom_item".equals(rewardType)
                    || "custom-item".equals(rewardType) || "customitem".equals(rewardType);
        }

        public boolean isRecipe() {
            return "recipe".equals(rewardType);
        }

        public boolean isEnchant() {
            return "enchant".equals(rewardType);
        }
    }

    public record UnlockRequirement(String collectionId, int required) {
    }
}
