package club.abelix.api.base.division.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.SimpleMenuCategory;
import club.abelix.api.base.item.gui.CategoryListGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionCategoryGui creation on 05.04.2024 at 17:53</p>
 */

public class DivisionCategoryGui extends SimpleMenuCategory {

    public DivisionCategoryGui() {
        super(Material.BOOK, "division_category_name");
    }

    public CompletableFuture<List<String>> getFutureDescription(Gamer gamer) {
        return CompletableFuture.completedFuture(gamer.getLanguage().getMessageList("division_category_description"));
    }
    @Override
    public void openGui(Player player, CategoryListGui from) {
        DivisionGui.openGui(Gamer.getGamer(player), from);
    }
}
