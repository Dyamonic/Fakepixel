package club.abelix.api.base.util.book;

import club.abelix.api.FalconAPI;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PagedBook {

    private final List<List<BaseComponent>> pages = new ArrayList<>();
    private ItemStack item;

    public Builder addPage() {
        item = null;

        List<BaseComponent> components = new ArrayList<>();
        pages.add(components);

        return new Builder(components);
    }

    public void openBook(Player player) {
        if (item == null) {
            item = new ItemStack(Material.WRITTEN_BOOK);

            ItemMeta meta = item.getItemMeta();

            pages.forEach(page -> FalconAPI.getNmsApi().getPlayerApi().addBookPage(meta, page.toArray(new BaseComponent[0])));

            item.setItemMeta(meta);
        }

        FalconAPI.getNmsApi().getPlayerApi().openBook(player, item);
    }

    @RequiredArgsConstructor
    public static class Builder {

        private final List<BaseComponent> components;

        public void addComponents(BaseComponent... components) {
            addComponents(Arrays.asList(components));
        }

        public void addStrings(Collection<String> strings) {
            if (strings == null || strings.isEmpty())
                return;

            strings.forEach(msg -> {
                String escapedMessage = msg.trim();
                BaseComponent[] component =
                        (escapedMessage.startsWith("{") && escapedMessage.endsWith("}"))
                                || (escapedMessage.startsWith("[") && escapedMessage.endsWith("]"))
                                ? ComponentSerializer.parse(msg)
                                : new BaseComponent[]{new TextComponent(msg)};

                components.addAll(Arrays.asList(component));
            });
        }

        public void addComponents(Collection<BaseComponent> components) {
            this.components.addAll(components);
        }
    }

}
