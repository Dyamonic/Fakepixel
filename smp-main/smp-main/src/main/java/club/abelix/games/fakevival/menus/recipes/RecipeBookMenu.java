package club.abelix.games.fakevival.menus.recipes;

import club.abelix.games.fakevival.managers.RecipeManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.recipes.CustomRecipe;
import club.abelix.games.fakevival.recipes.RecipeCategory;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;
import java.util.Locale;

public final class RecipeBookMenu {
    private final RecipeManager recipeManager;

    public RecipeBookMenu(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("recipes"), 54, CC.color("&8Recipe Book"));
        inventory.setItem(4, item(Material.KNOWLEDGE_BOOK, "&aRecipe Book", List.of(
                "&7View unlocked and locked recipes.",
                "&7Discover vanilla and custom crafting paths.",
                "",
                "&eClick a category to browse."
        )));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
        List<VanillaBazaarRegistry.Subcategory> categories=VanillaBazaarRegistry.subcategories();
        for(int index=0;index<slots.length&&index<categories.size();index++){
            VanillaBazaarRegistry.Subcategory category=categories.get(index);
            List<VanillaBazaarRegistry.Entry> vanillaItems=VanillaBazaarRegistry.entries(category.id());
            java.util.LinkedHashMap<String,CustomRecipe> related=new java.util.LinkedHashMap<>();
            for(VanillaBazaarRegistry.Entry entry:vanillaItems)for(CustomRecipe recipe:recipeManager.getRecipesRelatedTo(entry.material()))if(!recipeManager.isAdminHiddenFor(player,recipe))related.putIfAbsent(recipe.id(),recipe);
            long unlocked=related.values().stream().filter(recipe->recipeManager.isUnlocked(player,recipe)).count();
            inventory.setItem(slots[index],item(category.icon(),"&e"+category.displayName(),List.of(
                    "&7View recipes connected to this category.",
                    "&7Vanilla Items: &e"+vanillaItems.size(),
                    "&7Recipes: &e"+related.size(),
                    "&7Unlocked: &a"+unlocked+"&7/&a"+related.size(),
                    "",
                    "&eClick to browse!",
                    "&8Vanilla Subcategory: "+category.id()
            )));
        }

        inventory.setItem(49, item(Material.BARRIER,"&cClose",List.of("&7Close the recipe book.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openVanilla(Player player){
        openMain(player);
    }

    public void openVanillaItems(Player player,String subcategory,int page){
        List<VanillaBazaarRegistry.Entry> entries=VanillaBazaarRegistry.entries(subcategory);int[] slots={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};int pages=Math.max(1,(entries.size()+slots.length-1)/slots.length),safe=Math.clamp(page,0,pages-1);String display=VanillaBazaarRegistry.subcategories().stream().filter(s->s.id().equals(subcategory)).map(VanillaBazaarRegistry.Subcategory::displayName).findFirst().orElse("Vanilla");Inventory inventory=Bukkit.createInventory(new MenuHolder("recipes-vanilla-items:"+subcategory+":"+safe),54,CC.color("&8Vanilla - "+display));
        for(int i=0;i<slots.length&&safe*slots.length+i<entries.size();i++){var entry=entries.get(safe*slots.length+i);List<CustomRecipe> related=recipeManager.getRecipesRelatedTo(entry.material());long unlocked=related.stream().filter(r->recipeManager.isUnlocked(player,r)).count();inventory.setItem(slots[i],item(entry.material(),"&f"+pretty(entry.material().name()),List.of("&7Material: &e"+entry.material().name(),"&7Related Recipes: &a"+related.size(),"&7Unlocked: &a"+unlocked+"&7/&a"+related.size(),"","&eClick to view recipes!","&8Vanilla Material: "+entry.material().name())));}
        if(safe>0)inventory.setItem(45,previousPageButton());inventory.setItem(49,backButton());if(safe+1<pages)inventory.setItem(53,nextPageButton());fill(inventory);player.openInventory(inventory);
    }

    public void openVanillaRecipes(Player player,Material material,int page){
        List<CustomRecipe> recipes=recipeManager.getRecipesRelatedTo(material);int[] slots={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};int pages=Math.max(1,(recipes.size()+slots.length-1)/slots.length),safe=Math.clamp(page,0,pages-1);Inventory inventory=Bukkit.createInventory(new MenuHolder("recipes-vanilla-path:"+material.name()+":"+safe),54,CC.color("&8Recipes - "+pretty(material.name())));
        for(int i=0;i<slots.length&&safe*slots.length+i<recipes.size();i++){CustomRecipe recipe=recipes.get(safe*slots.length+i);boolean unlocked=recipeManager.isUnlocked(player,recipe),craftable=recipeManager.hasIngredients(player,recipe);ItemStack display=recipeManager.createResult(recipe).clone();ItemMeta meta=display.getItemMeta();List<String> lore=new ArrayList<>();lore.add("&7Output: &e"+recipeManager.getDisplayName(recipe.id()));lore.add("&7Category: &e"+recipe.category().displayName());lore.add("&7Status: "+(unlocked?"&aUnlocked":"&cLocked"));lore.add("");lore.add("&eUnlock Requirements:");lore.add((unlocked?"&a✔ ":"&c✖ ")+recipeManager.requirementText(recipe));lore.addAll(recipeManager.requirementLines(player,recipe));lore.add("");lore.add(craftable?"&aIngredients available":"&cMissing ingredients");lore.add("&eClick to view recipe!");lore.add("&8Recipe: "+recipe.id());meta.setDisplayName(CC.color((unlocked?"&a":"&c")+recipeManager.getDisplayName(recipe.id())));meta.setLore(CC.colorList(lore));display.setItemMeta(meta);inventory.setItem(slots[i],display);}
        if(safe>0)inventory.setItem(45,previousPageButton());inventory.setItem(49,backButton());if(safe+1<pages)inventory.setItem(53,nextPageButton());fill(inventory);player.openInventory(inventory);
    }

    public void openCategory(Player player, RecipeCategory category) {
        openCategory(player, category, 0);
    }

    public void openCategory(Player player, RecipeCategory category, int page) {
        openCategory(player,category,page,"all");
    }
    public void openCategory(Player player, RecipeCategory category, int page,String filter) {
        int[] slots = contentSlots();
        List<CustomRecipe> recipes = recipeManager.getRecipes(category).stream()
                .filter(recipe -> !recipeManager.isAdminHiddenFor(player, recipe))
                .filter(recipe->switch(filter.toLowerCase(Locale.ROOT)){case "unlocked"->recipeManager.isUnlocked(player,recipe);case "craftable"->recipeManager.isUnlocked(player,recipe)&&recipeManager.hasIngredients(player,recipe);case "locked"->!recipeManager.isUnlocked(player,recipe);default->true;})
                .toList();
        int totalPages = Math.max(1, (recipes.size() + slots.length - 1) / slots.length);
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("recipes:" + category.name() + ":" + safePage+":"+filter.toLowerCase(Locale.ROOT)), 54, CC.color("&8" + category.displayName()));
        inventory.setItem(4, item(category.icon(), "&a" + category.displayName(), List.of(
                "&7Unlocked recipes are craftable.",
                "&7Locked recipes show their requirements.",
                "",
                "&eClick a recipe to preview."
        )));

        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < recipes.size(); index++) {
            CustomRecipe recipe = recipes.get(start + index);
            boolean unlocked = recipeManager.isUnlocked(player, recipe);
            List<String> lore = new ArrayList<>();
            lore.add("&7Category: &e" + category.displayName());
            lore.add("&7Unlocked: " + (unlocked ? "&aYes" : "&cNo"));
            lore.add("&7Requires: &e" + recipeManager.requirementText(recipe));
            lore.add("");
            if (unlocked) {
                lore.add("&aUnlocked");
                lore.add("&eClick to view recipe!");
            } else {
                lore.add("&cLocked");
                lore.add("&7Unlock from: &e" + recipeManager.requirementText(recipe));
                String progress = recipeManager.progressText(player, recipe);
                if (!progress.isBlank()) lore.add(progress);
            }
            lore.add("&8Recipe: " + recipe.id());
            ItemStack display = recipeManager.createResult(recipe);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.color((unlocked ? "&a" : "&c") + recipeManager.getDisplayName(recipe.id())));
                meta.setLore(CC.colorList(lore));
                display.setItemMeta(meta);
            }
            inventory.setItem(slots[index], display);
        }

        inventory.setItem(46,item(Material.HOPPER,"&eFilter: &f"+pretty(filter),List.of("&7All, Unlocked, Craftable, Locked","","&eClick to change!","&8Recipe Filter: "+filter)));
        if (safePage > 0) inventory.setItem(48, previousPageButton());
        inventory.setItem(50, backButton());
        if (safePage + 1 < totalPages) inventory.setItem(51, nextPageButton());
        fill(inventory);
        player.openInventory(inventory);
    }

    private int[] contentSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
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

    private ItemStack backButton() {
        return item(Material.BARRIER, "&cBack", List.of("&7Return to the previous menu.", "", "&eClick to go back!"));
    }

    private ItemStack previousPageButton() {
        return item(Material.ARROW, "&aPrevious Page", List.of("&7Go to the previous page.", "", "&eClick to go back!"));
    }

    private ItemStack nextPageButton() {
        return item(Material.ARROW, "&aNext Page", List.of("&7Go to the next page.", "", "&eClick to continue!"));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }
    private String pretty(String value){String[] words=value.toLowerCase(Locale.ROOT).split("_");StringBuilder out=new StringBuilder();for(String word:words){if(!out.isEmpty())out.append(' ');out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));}return out.toString();}
}
