package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.RecipeManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.menus.recipes.RecipeBookMenu;
import club.abelix.games.fakevival.menus.recipes.RecipePreviewMenu;
import club.abelix.games.fakevival.recipes.RecipeCategory;
import club.abelix.games.fakevival.util.CommandUtil;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;

public final class RecipesCommand implements CommandExecutor, Listener {
    private final RecipeManager recipeManager;
    private final RecipeBookMenu bookMenu;
    private final RecipePreviewMenu previewMenu;

    public RecipesCommand(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.bookMenu = new RecipeBookMenu(recipeManager);
        this.previewMenu = new RecipePreviewMenu(recipeManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length>0&&args[0].equalsIgnoreCase("validate")){
            if(!sender.hasPermission("fakevival.recipes.admin")){sender.sendMessage(CC.color("&cYou do not have permission to validate recipes."));return true;}
            recipeManager.rebuildVanillaRecipeLinks();var result=recipeManager.validate();sender.sendMessage("[SMP] Recipe validation complete: recipes="+result.recipes()+", vanillaLinks="+result.vanillaLinks()+", warnings="+result.warnings()+".");return true;
        }
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        bookMenu.openMain(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.equals("recipes") && !type.startsWith("recipes:") && !type.startsWith("recipes-") && !type.startsWith("recipe-preview:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        int slot = event.getRawSlot();
        if (slot == 50 && clicked.getType() == org.bukkit.Material.BARRIER && name.equals("back")) {
            if (type.startsWith("recipe-preview:")) {
                String[] parts = type.split(":", 4);
                bookMenu.openCategory(player, RecipeCategory.parse(parts.length > 1 ? parts[1] : "UTILITY"), parsePage(parts, 2));
            } else if (type.startsWith("recipes:")) {
                bookMenu.openMain(player);
            } else {
                player.closeInventory();
            }
            return;
        }
        if(slot==49&&clicked.getType()==Material.BARRIER&&name.equals("close")){player.closeInventory();return;}
        if(type.equals("recipes-vanilla")&&slot==49){bookMenu.openMain(player);return;}
        if(type.startsWith("recipes-vanilla-items:")){
            String[] parts=type.split(":",3);int page=parsePage(parts,2);if(slot==49){bookMenu.openMain(player);return;}if(slot==45){bookMenu.openVanillaItems(player,parts[1],page-1);return;}if(slot==53){bookMenu.openVanillaItems(player,parts[1],page+1);return;}
        }
        if(type.startsWith("recipes-vanilla-path:")){
            String[] parts=type.split(":",3);Material material=Material.matchMaterial(parts[1]);int page=parsePage(parts,2);if(slot==49){if(material!=null)VanillaBazaarRegistry.byMaterial(material).ifPresent(e->bookMenu.openVanillaItems(player,e.subcategory(),0));return;}if(material!=null&&slot==45){bookMenu.openVanillaRecipes(player,material,page-1);return;}if(material!=null&&slot==53){bookMenu.openVanillaRecipes(player,material,page+1);return;}
        }
        if (type.startsWith("recipes:")) {
            String[] parts = type.split(":", 4);
            RecipeCategory currentCategory = RecipeCategory.parse(parts.length > 1 ? parts[1] : "UTILITY");
            int page = parsePage(parts, 2);
            String filter=parts.length>3?parts[3]:"all";
            if(slot==46){String next=switch(filter){case "all"->"unlocked";case "unlocked"->"craftable";case "craftable"->"locked";default->"all";};bookMenu.openCategory(player,currentCategory,0,next);return;}
            if (slot == 48 && name.equals("previous page")) {
                bookMenu.openCategory(player, currentCategory, Math.max(0, page - 1),filter);
                return;
            }
            if (slot == 51 && name.equals("next page")) {
                bookMenu.openCategory(player, currentCategory, page + 1,filter);
                return;
            }
        }

        String category = loreValue(clicked, "Recipe Category: ");
        if (category != null) {
            RecipeCategory parsed=RecipeCategory.parse(category);if(parsed==RecipeCategory.VANILLA)bookMenu.openVanilla(player);else bookMenu.openCategory(player,parsed,0);
            return;
        }
        String subcategory=loreValue(clicked,"Vanilla Subcategory: ");if(subcategory!=null){bookMenu.openVanillaItems(player,subcategory,0);return;}
        String vanilla=loreValue(clicked,"Vanilla Material: ");if(vanilla!=null){Material material=Material.matchMaterial(vanilla);if(material!=null)bookMenu.openVanillaRecipes(player,material,0);return;}
        String craft=loreValue(clicked,"Craft Recipe: ");if(craft!=null){recipeManager.getRecipe(craft).ifPresent(recipe->{if(recipeManager.craftFromInventory(player,recipe)){player.sendMessage(CC.color("&aCrafted &e"+recipeManager.getDisplayName(recipe.id())+"&a."));previewMenu.open(player,recipe,0);}else player.sendMessage(CC.color(recipeManager.isUnlocked(player,recipe)?"&cMissing Ingredients":"&cRecipe Locked"));});return;}

        String recipeId = loreValue(clicked, "Recipe: ");
        if (recipeId != null) {
            recipeManager.getRecipe(recipeId).ifPresent(recipe -> {
                String[] parts = type.split(":", 3);
                previewMenu.open(player, recipe, type.startsWith("recipes:") ? parsePage(parts, 2) : 0);
            });
        }
    }

    @EventHandler public void onInventoryDrag(InventoryDragEvent event){if(event.getInventory().getHolder() instanceof MenuHolder(String type)&&(type.equals("recipes")||type.startsWith("recipes:")||type.startsWith("recipes-")||type.startsWith("recipe-preview:")))event.setCancelled(true);}

    private int parsePage(String[] parts, int index) {
        if (parts.length <= index) return 0;
        try {
            return Math.max(0, Integer.parseInt(parts[index]));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }
}
