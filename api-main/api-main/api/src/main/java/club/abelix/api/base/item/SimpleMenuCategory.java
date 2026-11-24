package club.abelix.api.base.item;

import club.abelix.api.base.item.gui.CategoryListGui;
import club.abelix.commons.language.Language;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @author Unidentified Person
 */
public abstract class SimpleMenuCategory implements GameMenuCategory {

    private final ItemStack icon;
    private final String name;

    public SimpleMenuCategory(ItemStack icon, String name) {
        this.icon = icon;
        this.name = name;
    }

    public SimpleMenuCategory(Material icon, String name) {
        this(new ItemStack(icon), name);
    }

    @Override
    public ItemStack getIcon() {
        return icon;
    }

    @Override
    public String getDisplayName(Language language) {
        return language.getMessage(name + "_name");
    }

    @Override
    public List<String> getDescription(Language language) {
        return language.getMessageList(name + "_description");
    }

    @Override
    public abstract void openGui(Player player, CategoryListGui from);
}
