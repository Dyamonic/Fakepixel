package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.recipes.CustomRecipe;
import club.abelix.games.fakevival.recipes.RecipeCategory;
import club.abelix.games.fakevival.recipes.RecipeUnlock;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;

public final class RecipeManager {
    private final JavaPlugin plugin;
    private final CollectionManager collectionManager;
    private final CustomItemManager customItemManager;
    private final CustomEnchantManager customEnchantManager;
    private final SkillManager skillManager;
    private final QuestManager questManager;
    private final Map<String, CustomRecipe> recipes = new LinkedHashMap<>();
    private final Map<NamespacedKey, String> recipeKeys = new LinkedHashMap<>();
    private final Set<String> warnedInvalidRecipes = new java.util.HashSet<>();
    private volatile Map<Material,List<CustomRecipe>> vanillaRecipeLinks=Map.of();

    public RecipeManager(JavaPlugin plugin, CollectionManager collectionManager, CustomItemManager customItemManager, CustomEnchantManager customEnchantManager, SkillManager skillManager, QuestManager questManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.customItemManager = customItemManager;
        this.customEnchantManager = customEnchantManager;
        this.skillManager = skillManager;
        this.questManager = questManager;
    }

    public void setup() {
        load();
        registerCraftingRecipes();
    }

    public void reload() {
        setup();
    }

    public List<CustomRecipe> getRecipes() {
        return List.copyOf(recipes.values());
    }

    public int skippedInvalidRecipeCount() {
        return warnedInvalidRecipes.size();
    }

    public List<CustomRecipe> getRecipes(RecipeCategory category) {
        return recipes.values().stream()
                .filter(recipe -> recipe.category() == category)
                .sorted(Comparator.comparing(CustomRecipe::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<CustomRecipe> getRecipe(String id) {
        return Optional.ofNullable(recipes.get(normalize(id)));
    }

    public Optional<CustomRecipe> getRecipe(NamespacedKey key) {
        return Optional.ofNullable(recipeKeys.get(key)).flatMap(this::getRecipe);
    }

    public String getDisplayName(String id) {
        return getRecipe(id).map(recipe -> strip(recipe.displayName())).orElse(pretty(id));
    }

    public ItemStack createResult(CustomRecipe recipe) {
        String resultId = normalize(recipe.resultId());
        ItemStack item;
        if (resultId.startsWith("custom_enchant:")) {
            item = customEnchantManager.createBook(resultId.substring("custom_enchant:".length()), 1);
        } else if (customItemManager.itemExists(resultId)) {
            item = customItemManager.createItem(resultId, 1);
        } else {
            Material material = material(recipe.resultId(), Material.PAPER);
            item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.color(recipe.displayName()));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    public ItemStack createIngredient(String value) {
        String id = customIngredientId(value);
        if (customItemManager.itemExists(id)) return customItemManager.createItem(id, 1);
        return new ItemStack(material(materialIngredientName(value), Material.PAPER));
    }

    public ItemStack createIngredient(String value, int amount) {
        ItemStack item = createIngredient(value);
        item.setAmount(Math.clamp(amount, 1, item.getMaxStackSize()));
        return item;
    }

    public int countIngredient(Player player, String expected) {
        if (player == null) return 0;
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (ingredientMatches(item, expected)) total += item.getAmount();
        }
        return total;
    }

    private boolean ingredientMatches(ItemStack actual, String expected) {
        if (actual == null || actual.getType().isAir()) return false;
        String actualId = normalize(customItemManager.getItemId(actual));
        if (isCustomIngredient(expected)) {
            return customItemManager.itemExists(customIngredientId(expected)) && customIngredientId(expected).equals(actualId);
        }
        return actualId.isBlank() && actual.getType() == material(materialIngredientName(expected), Material.AIR);
    }

    public boolean matchesIngredients(CraftingInventory inventory, CustomRecipe recipe) {
        if (inventory == null || recipe == null) return false;
        ItemStack[] matrix = inventory.getMatrix();
        if (matrix.length < 9) return false;
        for (int row = 0; row < 3; row++) {
            String shapeRow = row < recipe.shape().size() ? recipe.shape().get(row) : "   ";
            for (int column = 0; column < 3; column++) {
                char symbol = shapeRow.charAt(column);
                ItemStack actual = matrix[row * 3 + column];
                if (symbol == ' ') {
                    if (actual != null && !actual.getType().isAir()) return false;
                    continue;
                }
                String expected = recipe.ingredients().get(symbol);
                if (expected == null || actual == null || actual.getType().isAir()) return false;
                if (!ingredientMatches(actual, expected)) return false;
                if (actual.getAmount() < recipe.ingredientAmount(symbol)) return false;
            }
        }
        return true;
    }

    public void consumeAdditionalIngredients(CraftingInventory inventory, CustomRecipe recipe) {
        if (inventory == null || recipe == null) return;
        ItemStack[] matrix = inventory.getMatrix();
        for (int row = 0; row < 3; row++) {
            String shapeRow = row < recipe.shape().size() ? recipe.shape().get(row) : "   ";
            for (int column = 0; column < 3; column++) {
                char symbol = shapeRow.charAt(column);
                if (symbol == ' ') continue;
                int extra = recipe.ingredientAmount(symbol) - 1;
                ItemStack actual = matrix[row * 3 + column];
                if (extra <= 0 || actual == null) continue;
                actual.setAmount(actual.getAmount() - extra);
            }
        }
        inventory.setMatrix(matrix);
    }

    public boolean isUnlocked(Player player, CustomRecipe recipe) {
        RecipeUnlock unlock = recipe.unlock();
        boolean primary=switch (unlock.type()) {
            case DEFAULT -> true;
            case COLLECTION -> collectionManager.getUnlockedTier(player, unlock.collectionId()) >= unlock.tier();
            case MULTI_COLLECTION -> unlock.requirements().entrySet().stream()
                    .allMatch(entry -> collectionManager.getUnlockedTier(player, entry.getKey()) >= entry.getValue());
            case SKILL_LEVEL -> skillManager != null && skillManager.getLevel(player, unlock.skill()) >= unlock.level();
            case SKILL_XP -> skillManager!=null&&skillManager.getXp(player,unlock.skill())>=unlock.level();
            case COLLECT_MATERIAL -> collectionManager.getProgress(player,unlock.collectionId())>=unlock.tier();
            case COLLECTION_TIER -> collectionManager.getUnlockedTier(player,unlock.collectionId())>=unlock.tier();
            case ADVENTURE_RANK -> plugin instanceof club.abelix.games.fakevival.Game game&&game.getAdventureGuildManager().profile(player).rank().order>=club.abelix.games.fakevival.managers.AdventureGuildManager.Rank.valueOf(unlock.skill().toUpperCase(Locale.ROOT)).order;
            case DUNGEON_COMPLETION -> plugin instanceof club.abelix.games.fakevival.Game game&&game.getAdventureGuildManager().profile(player).dungeons()>0;
            case BOSS_KILL -> plugin instanceof club.abelix.games.fakevival.Game game&&game.getAdventureGuildManager().profile(player).bosses()>0;
            case PLAYER_LEVEL -> plugin instanceof club.abelix.games.fakevival.Game game&&game.getPlayerLevelManager().getLevel(player)>=unlock.level();
            case QUEST -> isQuestUnlocked(player, unlock);
            case PERMISSION -> !unlock.permission().isBlank() && player.hasPermission(unlock.permission());
            case ADMIN_ONLY -> player.hasPermission("fakevival.admin.recipes") || player.isOp();
        };
        return primary&&additionalRequirementsMet(player,recipe);
    }

    private boolean additionalRequirementsMet(Player player,CustomRecipe recipe){return requirementLines(player,recipe).stream().noneMatch(line->line.startsWith("&c"));}
    public List<String> requirementLines(Player player,CustomRecipe recipe){
        List<String> lines=new ArrayList<>();String base="recipes."+recipe.id()+".requirements";ConfigurationSection section=club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(base);if(section==null)return lines;
        ConfigurationSection collect=section.getConfigurationSection("collect-material");if(collect!=null)for(String key:collect.getKeys(false)){Material material=material(key,Material.AIR);int need=collect.getInt(key);int have=collectionManager.getProgress(player,key.toLowerCase(Locale.ROOT));lines.add(check(have>=need,"Required Material Collected: "+need+"x "+pretty(material.name())));}
        ConfigurationSection levels=section.getConfigurationSection("skill-level");if(levels!=null)for(String skill:levels.getKeys(false)){int need=levels.getInt(skill);lines.add(check(skillManager.getLevel(player,skill)>=need,"Skill: "+pretty(skill)+" Level "+need));}
        ConfigurationSection xp=section.getConfigurationSection("skill-xp");if(xp!=null)for(String skill:xp.getKeys(false)){int need=xp.getInt(skill);lines.add(check(skillManager.getXp(player,skill)>=need,"Skill XP: "+pretty(skill)+" "+need));}
        if(section.contains("adventure-rank")&&plugin instanceof club.abelix.games.fakevival.Game game){String rank=section.getString("adventure-rank","E");var profile=game.getAdventureGuildManager().profile(player);int need=club.abelix.games.fakevival.managers.AdventureGuildManager.Rank.valueOf(rank.toUpperCase(Locale.ROOT)).order;lines.add(check(profile.rank().order>=need,"Adventure Rank: "+rank));}
        if(section.contains("player-level")&&plugin instanceof club.abelix.games.fakevival.Game game){int need=section.getInt("player-level");lines.add(check(game.getPlayerLevelManager().getLevel(player)>=need,"Player Level: "+need));}
        if((section.contains("dungeon-completion")||section.contains("boss-kill"))&&plugin instanceof club.abelix.games.fakevival.Game game){var profile=game.getAdventureGuildManager().profile(player);if(section.contains("dungeon-completion"))lines.add(check(profile.dungeons()>0,"Dungeon Completion: "+section.getString("dungeon-completion")));if(section.contains("boss-kill"))lines.add(check(profile.bosses()>0,"Boss Defeat: "+section.getString("boss-kill")));}
        return lines;
    }
    private String check(boolean met,String text){return (met?"&a✔ ":"&c✖ ")+text;}

    public String requirementText(CustomRecipe recipe) {
        RecipeUnlock unlock = recipe.unlock();
        return switch (unlock.type()) {
            case DEFAULT -> "Default Recipe";
            case COLLECTION -> strip(collectionManager.getDisplayName(unlock.collectionId())) + " Tier " + collectionManager.toRoman(unlock.tier());
            case MULTI_COLLECTION -> unlock.requirements().entrySet().stream()
                    .map(entry -> strip(collectionManager.getDisplayName(entry.getKey())) + " Tier " + collectionManager.toRoman(entry.getValue()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("Multiple Collections");
            case SKILL_LEVEL -> pretty(unlock.skill()) + " Level " + unlock.level();
            case SKILL_XP -> pretty(unlock.skill())+" XP "+unlock.level();
            case COLLECT_MATERIAL -> "Collect "+unlock.tier()+"x "+pretty(unlock.collectionId());
            case COLLECTION_TIER -> strip(collectionManager.getDisplayName(unlock.collectionId()))+" Tier "+collectionManager.toRoman(unlock.tier());
            case ADVENTURE_RANK -> "Adventure Rank "+unlock.skill().toUpperCase(Locale.ROOT);
            case DUNGEON_COMPLETION -> "Dungeon Completion: "+pretty(unlock.quest());
            case BOSS_KILL -> "Boss Defeat: "+pretty(unlock.quest());
            case PLAYER_LEVEL -> "PlayerLevel "+unlock.level();
            case QUEST -> "Quest: " + pretty(unlock.quest());
            case PERMISSION -> "Permission: " + unlock.permission();
            case ADMIN_ONLY -> "Admin Only";
        };
    }

    public int requiredCollectionAmount(CustomRecipe recipe) {
        RecipeUnlock unlock = recipe.unlock();
        if (unlock.type() != RecipeUnlock.Type.COLLECTION) return 0;
        return collectionManager.getTierRequired(unlock.collectionId(), unlock.tier());
    }

    public String progressText(Player player, CustomRecipe recipe) {
        RecipeUnlock unlock = recipe.unlock();
        if (unlock.type() == RecipeUnlock.Type.COLLECTION) {
            int required = requiredCollectionAmount(recipe);
            int progress = collectionManager.getProgress(player, unlock.collectionId());
            return "&7Progress: &e" + format(Math.min(progress, required)) + "&7/&e" + format(required) + " " + strip(collectionManager.getDisplayName(unlock.collectionId())).replace(" Collection", "");
        }
        if (unlock.type() == RecipeUnlock.Type.MULTI_COLLECTION) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unlock.requirements().entrySet()) {
                parts.add(strip(collectionManager.getDisplayName(entry.getKey())) + " " + collectionManager.toRoman(collectionManager.getUnlockedTier(player, entry.getKey())) + "/" + collectionManager.toRoman(entry.getValue()));
            }
            return "&7Progress: &e" + String.join("&7, &e", parts);
        }
        if (unlock.type() == RecipeUnlock.Type.SKILL_LEVEL && skillManager != null) {
            return "&7Progress: &e" + skillManager.getLevel(player, unlock.skill()) + "&7/&e" + unlock.level() + " " + pretty(unlock.skill());
        }
        return "";
    }

    public boolean isAdminHiddenFor(HumanEntity viewer, CustomRecipe recipe) {
        return recipe.unlock().type() == RecipeUnlock.Type.ADMIN_ONLY
                && !viewer.hasPermission("fakevival.admin.recipes")
                && !viewer.isOp();
    }

    private void load() {
        recipes.clear();
        recipeKeys.clear();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("recipes");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            CustomRecipe recipe = loadRecipe(normalize(id), section.getConfigurationSection(id));
            if (recipe != null && validRecipeItems(recipe)) recipes.put(recipe.id(), recipe);
        }
        rebuildVanillaRecipeLinks();
    }

    public List<CustomRecipe> getRecipesRelatedTo(Material material){return vanillaRecipeLinks.getOrDefault(material,List.of());}
    public int vanillaLinkCount(){return vanillaRecipeLinks.values().stream().mapToInt(List::size).sum();}
    public void rebuildVanillaRecipeLinks(){
        Map<Material,List<CustomRecipe>> links=new LinkedHashMap<>();
        Set<Material> linkedMaterials=new LinkedHashSet<>();VanillaBazaarRegistry.entries().forEach(entry->linkedMaterials.add(entry.material()));
        for(String collection:collectionManager.getCollectionIds())for(String tracked:collectionManager.getTrackedMaterials(collection)){Material material=Material.matchMaterial(tracked);if(material!=null)linkedMaterials.add(material);}
        for(Material material:linkedMaterials){
            LinkedHashMap<String,CustomRecipe> found=new LinkedHashMap<>();Set<String> frontier=new HashSet<>();
            Set<String> pathCollections=new HashSet<>(collectionManager.getCollectionsRelatedTo(material));
            for(CustomRecipe recipe:recipes.values())if(pathCollections.contains(collectionManager.internalCollectionId(recipe.unlock().collectionId())))found.put(recipe.id(),recipe);
            for(CustomRecipe recipe:recipes.values())for(String ingredient:recipe.ingredients().values()){
                if(!isCustomIngredient(ingredient)&&material(materialIngredientName(ingredient),Material.AIR)==material){found.put(recipe.id(),recipe);frontier.add(normalize(recipe.resultId()));}
                else if(isCustomIngredient(ingredient)){String id=customIngredientId(ingredient);if(customItemManager.itemExists(id)&&customItemManager.getMaterial(id)==material){found.put(recipe.id(),recipe);frontier.add(normalize(recipe.resultId()));}}
            }
            // Collection/drop source links connect vanilla gathering to its first custom material.
            for(String id:customItemManager.getItemIds()){
                String base="custom-items.items."+id+".collection.drop.sources";
                if(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base).stream().anyMatch(v->v.equalsIgnoreCase(material.name())))frontier.add(normalize(id));
            }
            for(int depth=0;depth<3&&!frontier.isEmpty();depth++){
                Set<String> next=new HashSet<>();
                for(CustomRecipe recipe:recipes.values())for(String ingredient:recipe.ingredients().values())if(isCustomIngredient(ingredient)&&frontier.contains(customIngredientId(ingredient))){if(found.putIfAbsent(recipe.id(),recipe)==null)next.add(normalize(recipe.resultId()));}
                frontier=next;
            }
            links.put(material,List.copyOf(found.values()));
        }
        vanillaRecipeLinks=Map.copyOf(links);
    }

    public boolean hasIngredients(Player player,CustomRecipe recipe){for(var e:recipe.ingredients().entrySet())if(uses(recipe.shape(),e.getKey())&&countIngredient(player,e.getValue())<recipe.ingredientAmount(e.getKey()))return false;return true;}
    public boolean craftFromInventory(Player player,CustomRecipe recipe){
        if(player==null||recipe==null||!isUnlocked(player,recipe)||!hasIngredients(player,recipe))return false;
        for(var e:recipe.ingredients().entrySet())if(uses(recipe.shape(),e.getKey()))removeIngredient(player,e.getValue(),recipe.ingredientAmount(e.getKey()));
        Map<Integer,ItemStack> overflow=player.getInventory().addItem(createResult(recipe).clone());overflow.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));return true;
    }
    private void removeIngredient(Player player,String expected,int amount){int left=amount;ItemStack[] contents=player.getInventory().getStorageContents();for(int i=0;i<contents.length&&left>0;i++){ItemStack item=contents[i];if(!ingredientMatches(item,expected))continue;int take=Math.min(left,item.getAmount());item.setAmount(item.getAmount()-take);left-=take;}player.getInventory().setStorageContents(contents);}

    public ValidationResult validate(){int warnings=warnedInvalidRecipes.size();Set<String> ids=new HashSet<>();for(CustomRecipe recipe:recipes.values()){if(!ids.add(recipe.id()))warnings++;if(recipe.category()==null||recipe.ingredients().isEmpty()||createResult(recipe).getType().isAir())warnings++;}Set<String> requestedRecipes=new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.RequestedCollectionRecipeDefaults.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.recipeIds());requestedRecipes.addAll(club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.recipeIds());for(String requested:requestedRecipes)if(!recipes.containsKey(requested))warnings++;for(String requested:club.abelix.games.fakevival.config.defaults.FuturisticArmorDefaults.recipeIds())if(!recipes.containsKey(requested))warnings++;Set<String> requestedMaterials=new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.RequestedMaterialDefaults80.materialIds());requestedMaterials.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.materialIds());requestedMaterials.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.materialIds());requestedMaterials.addAll(club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.materialIds());requestedMaterials.addAll(club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.materialIds());requestedMaterials.addAll(club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.materialIds());for(String material:requestedMaterials)if(!customItemManager.itemExists(material))warnings++;for(String item:List.of("futuristic_helmet","futuristic_chestplate","futuristic_leggings","futuristic_boots","enchanted_netherite_ingot","enchanted_iron_block","enchanted_blaze_powder","enchanted_diamond_helmet"))if(!customItemManager.itemExists(item))warnings++;warnings+=(int)VanillaBazaarRegistry.entries().stream().filter(e->getRecipesRelatedTo(e.material()).isEmpty()).count();return new ValidationResult(recipes.size(),vanillaLinkCount(),warnings);}
    public record ValidationResult(int recipes,int vanillaLinks,int warnings){}

    private CustomRecipe loadRecipe(String id, ConfigurationSection section) {
        if (section == null) return null;
        List<String> shape = normalizeShape(section.getStringList("shape"));
        if (shape.isEmpty()) return null;
        NamespacedKey key = new NamespacedKey(plugin, "recipe_" + id);
        Map<Character, String> ingredients = new LinkedHashMap<>();
        Map<Character, Integer> ingredientAmounts = new LinkedHashMap<>();
        ConfigurationSection ingredientSection = section.getConfigurationSection("ingredients");
        if (ingredientSection != null) {
            for (String ingredientKey : ingredientSection.getKeys(false)) {
                if (ingredientKey.length() != 1) continue;
                ingredients.put(ingredientKey.charAt(0), ingredientSection.getString(ingredientKey, "AIR"));
            }
        }
        ConfigurationSection amountSection = section.getConfigurationSection("amounts");
        if (amountSection != null) {
            for (String amountKey : amountSection.getKeys(false)) {
                if (amountKey.length() == 1) ingredientAmounts.put(amountKey.charAt(0), Math.clamp(amountSection.getInt(amountKey, 1), 1, 64));
            }
        }
        return new CustomRecipe(
                id,
                section.getString("display-name", "&a" + pretty(id)),
                RecipeCategory.parse(section.getString("category", "UTILITY")),
                section.getString("result", id),
                shape,
                Map.copyOf(ingredients),
                Map.copyOf(ingredientAmounts),
                loadUnlock(section.getConfigurationSection("unlock")),
                key
        );
    }

    private RecipeUnlock loadUnlock(ConfigurationSection section) {
        if (section == null) return RecipeUnlock.defaultUnlock();
        RecipeUnlock.Type type = RecipeUnlock.parseType(section.getString("type", "DEFAULT"));
        Map<String, Integer> requirements = new LinkedHashMap<>();
        ConfigurationSection requirementSection = section.getConfigurationSection("requirements");
        if (requirementSection != null) {
            for (String key : requirementSection.getKeys(false)) requirements.put(key, requirementSection.getInt(key, 1));
        }
        return new RecipeUnlock(
                type,
                section.getString("collection", ""),
                section.getInt("tier", 1),
                RecipeUnlock.normalizeRequirements(requirements),
                section.getString("skill", ""),
                section.getInt("level", 1),
                section.getString("quest", ""),
                section.getString("period", ""),
                section.getString("permission", "")
        );
    }

    private void registerCraftingRecipes() {
        for (CustomRecipe recipe : recipes.values()) {
            Bukkit.removeRecipe(recipe.key());
            if (!validRecipeItems(recipe)) continue;
            ShapedRecipe shapedRecipe = new ShapedRecipe(recipe.key(), createResult(recipe));
            shapedRecipe.shape(recipe.shape().toArray(String[]::new));
            boolean valid = true;
            for (Map.Entry<Character, String> entry : recipe.ingredients().entrySet()) {
                if (!uses(recipe.shape(), entry.getKey())) continue;
                Material material = isCustomIngredient(entry.getValue())
                        ? createIngredient(entry.getValue()).getType()
                        : material(materialIngredientName(entry.getValue()), Material.AIR);
                if (material == Material.AIR) {
                    valid = false;
                    warnInvalid(recipe.id(), "ingredient '" + entry.getValue() + "' is not a valid material or custom item");
                    break;
                }
                if (isCustomIngredient(entry.getValue())) {
                    shapedRecipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(createIngredient(entry.getValue())));
                } else {
                    shapedRecipe.setIngredient(entry.getKey(), material);
                }
            }
            if (!valid) continue;
            try {
                Bukkit.addRecipe(shapedRecipe);
                recipeKeys.put(recipe.key(), recipe.id());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                warnInvalid(recipe.id(), "it could not be registered: " + exception.getMessage());
            }
        }
    }

    private boolean validRecipeItems(CustomRecipe recipe) {
        String result = normalize(recipe.resultId());
        if (!result.startsWith("custom_enchant:") && material(recipe.resultId(), Material.AIR) == Material.AIR
                && !customItemManager.itemExists(result)) {
            warnInvalid(recipe.id(), "output '" + recipe.resultId() + "' is missing");
            return false;
        }
        for (String ingredient : recipe.ingredients().values()) {
            if (isCustomIngredient(ingredient) && !customItemManager.itemExists(customIngredientId(ingredient))) {
                warnInvalid(recipe.id(), "custom ingredient '" + customIngredientId(ingredient) + "' is missing");
                return false;
            }
        }
        return true;
    }

    private void warnInvalid(String recipeId, String reason) {
        if (warnedInvalidRecipes.add(recipeId + ":" + reason)) {
            plugin.getLogger().warning("Skipping recipe '" + recipeId + "' because " + reason + ".");
        }
    }

    private boolean isCustomIngredient(String value) {
        if (value == null) return false;
        if (value.regionMatches(true, 0, "material:", 0, 9)) return false;
        if (value.regionMatches(true, 0, "custom:", 0, 7)) return true;
        return customItemManager.itemExists(normalize(value));
    }

    private String customIngredientId(String value) {
        String text = value == null ? "" : value.trim();
        return normalize(text.regionMatches(true, 0, "custom:", 0, 7) ? text.substring(7) : text);
    }

    private String materialIngredientName(String value) {
        String text = value == null ? "" : value.trim();
        return text.regionMatches(true, 0, "material:", 0, 9) ? text.substring(9) : text;
    }

    private boolean uses(List<String> shape, char symbol) {
        for (String line : shape) if (line.indexOf(symbol) >= 0) return true;
        return false;
    }

    private boolean isQuestUnlocked(Player player, RecipeUnlock unlock) {
        if (questManager == null || unlock.quest().isBlank()) return false;
        String period = unlock.period().isBlank() ? questManager.getAdventureQuestPeriod(unlock.quest()) : unlock.period();
        if (period != null && questManager.isAdventureQuestClaimed(player, period, unlock.quest())) return true;
        return false;
    }

    private List<String> normalizeShape(List<String> shape) {
        if (shape == null || shape.isEmpty()) return List.of();
        List<String> normalized = new ArrayList<>();
        for (int row = 0; row < Math.min(3, shape.size()); row++) {
            String line = shape.get(row) == null ? "" : shape.get(row);
            normalized.add((line + "   ").substring(0, 3));
        }
        return normalized;
    }

    private Material material(String name, Material fallback) {
        try {
            return Material.valueOf((name == null ? "" : name).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_").trim();
    }

    private String strip(String text) {
        return org.bukkit.ChatColor.stripColor(CC.color(text == null ? "" : text));
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Recipe";
        String[] words = text.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String format(int number) {
        return String.format(Locale.US, "%,d", number);
    }
}
