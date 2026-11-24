package club.abelix.api.base.item;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.gui.CategoryListGui;
import club.abelix.commons.language.Language;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public interface GameMenuCategory {

    ItemStack getIcon();

    String getDisplayName(Language language);

    List<String> getDescription(Language language);

    default CompletableFuture<List<String>> getFutureDescription(Gamer gamer) {
        return null;
    }

    void openGui(Player player, CategoryListGui from);

}
