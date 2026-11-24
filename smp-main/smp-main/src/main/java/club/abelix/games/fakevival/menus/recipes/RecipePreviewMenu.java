package club.abelix.games.fakevival.menus.recipes;

import club.abelix.games.fakevival.managers.RecipeManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.recipes.CustomRecipe;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecipePreviewMenu {
    private final RecipeManager recipeManager;

    public RecipePreviewMenu(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public void open(Player player, CustomRecipe recipe) {
        open(player, recipe, 0);
    }

    public void open(Player player, CustomRecipe recipe, int categoryPage) {
        int safePage = Math.max(0, categoryPage);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("recipe-preview:" + recipe.category().name() + ":" + safePage + ":" + recipe.id()), 54, CC.color("&8Recipe: " + recipeManager.getDisplayName(recipe.id())));
        int[] recipeSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int row = 0; row < 3; row++) {
            String line = row < recipe.shape().size() ? recipe.shape().get(row) : "   ";
            for (int column = 0; column < 3; column++) {
                char key = column < line.length() ? line.charAt(column) : ' ';
                if (key == ' ' || !recipe.ingredients().containsKey(key)) continue;
                String ingredient = recipe.ingredients().get(key);
                inventory.setItem(recipeSlots[row * 3 + column], ingredient(player, ingredient, recipe.ingredientAmount(key)));
            }
        }

        boolean unlocked = recipeManager.isUnlocked(player, recipe);
        inventory.setItem(24, statusItem(player, recipe, unlocked));
        inventory.setItem(25, resultItem(player, recipe, unlocked));
        boolean ingredients=recipeManager.hasIngredients(player,recipe);
        inventory.setItem(40,item(unlocked&&ingredients?Material.LIME_CONCRETE:Material.RED_CONCRETE,unlocked&&ingredients?"&aCraft Item":!unlocked?"&cRecipe Locked":"&cMissing Ingredients",List.of(unlocked&&ingredients?"&eClick to craft!":!unlocked?"&7Unlock the recipe before crafting.":"&7Collect the missing ingredients.","&8Craft Recipe: "+recipe.id())));
        inventory.setItem(50, item(Material.BARRIER, "&cBack", List.of("&7Return to the previous menu.", "", "&eClick to go back!")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private ItemStack ingredient(Player player, String value, int required) {
        ItemStack item = recipeManager.createIngredient(value, required);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!meta.hasDisplayName()) meta.setDisplayName(CC.color("&f" + pretty(value)));
            List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
            int owned = recipeManager.countIngredient(player, value);
            lore.add("");
            lore.add(CC.color("&7Required: &e" + required));
            lore.add(CC.color("&7You have: " + (owned >= required ? "&a" : "&c") + owned));
            if (owned < required) lore.add(CC.color("&cMissing: " + (required - owned)));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack statusItem(Player player, CustomRecipe recipe, boolean unlocked) {
        List<String> lore = new ArrayList<>();
        if (unlocked) {
            lore.add("&aUnlocked");
            lore.add("&eClick to view recipe!");
        } else {
            lore.add("&cLocked");
            lore.add("&7Unlock from: &e" + recipeManager.requirementText(recipe));
            String progress = recipeManager.progressText(player, recipe);
            if (!progress.isBlank()) lore.add(progress);
        }
        for(String requirement:recipeManager.requirementLines(player,recipe))lore.add(requirement);
        return item(unlocked ? Material.LIME_DYE : Material.RED_DYE, unlocked ? "&aRecipe Unlocked" : "&cRecipe Locked", lore);
    }

    private ItemStack resultItem(Player player, CustomRecipe recipe, boolean unlocked) {
        ItemStack result = recipeManager.createResult(recipe);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
            lore.add("");
            lore.add(CC.color("&7Recipe Name: &f" + recipeManager.getDisplayName(recipe.id())));
            lore.add(CC.color("&7Status: " + (unlocked ? "&aUnlocked" : "&cLocked")));
            lore.add(CC.color("&7Requirement: &e" + recipeManager.requirementText(recipe)));
            String progress = recipeManager.progressText(player, recipe);
            if (!progress.isBlank()) lore.add(CC.color(progress));
            lore.add("");
            lore.add(CC.color(unlocked ? "&aYou can craft this recipe." : "&cUnlock this recipe to craft it."));
            lore.add(CC.color("&8Recipe: " + recipe.id()));
            meta.setLore(lore);
            result.setItemMeta(meta);
        }
        return result;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null || material.isAir() ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Ingredient";
        String[] words = text.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return builder.toString();
    }
}
