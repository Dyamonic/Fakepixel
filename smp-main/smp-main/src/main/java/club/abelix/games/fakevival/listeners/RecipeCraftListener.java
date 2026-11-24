package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.RecipeManager;
import club.abelix.games.fakevival.recipes.CustomRecipe;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.CraftingInventory;

import java.util.Optional;

public final class RecipeCraftListener implements Listener {
    private final RecipeManager recipeManager;

    public RecipeCraftListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @EventHandler
    public void onCraftPreview(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        Optional<CustomRecipe> recipe = customRecipe(event.getRecipe());
        if (recipe.isEmpty()) return;
        if (!recipeManager.isUnlocked(player, recipe.get()) || !recipeManager.matchesIngredients(event.getInventory(), recipe.get())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Optional<CustomRecipe> recipe = customRecipe(event.getRecipe());
        if (recipe.isEmpty()) return;
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            event.setCancelled(true);
            player.sendMessage(CC.color("&cShift-crafting custom quantity recipes is disabled. Craft one at a time."));
            return;
        }
        if (event.getInventory() instanceof CraftingInventory crafting
                && recipeManager.isUnlocked(player, recipe.get())
                && recipeManager.matchesIngredients(crafting, recipe.get())) {
            recipeManager.consumeAdditionalIngredients(crafting, recipe.get());
            return;
        }
        event.setCancelled(true);
        event.getInventory().setResult(null);
        player.sendMessage(CC.color("&cYou have not unlocked this recipe yet!"));
    }

    private Optional<CustomRecipe> customRecipe(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) return Optional.empty();
        return recipeManager.getRecipe(keyed.getKey());
    }
}
