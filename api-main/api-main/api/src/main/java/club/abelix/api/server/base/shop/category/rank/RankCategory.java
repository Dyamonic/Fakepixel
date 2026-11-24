package club.abelix.api.server.base.shop.category.rank;

import club.abelix.api.base.util.book.PagedBook;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.item.RankIcons;
import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.commons.data.permission.Group;
import gnu.trove.list.array.TIntArrayList;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import w.util.Env;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 15/01/2023 13:40
 */
public class RankCategory extends ShopCategory {
    public RankCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "donate_description_gui_title", item);
        draw();
    }

    @Override
    public void drawCategory() {
        val slots = new TIntArrayList();
        slots.add(new int[]{11, 12, 13, 14, 15, 21, 23});

        val donateGroups = RankIcons.getDonateGroups();

        for (val group : donateGroups) {
            int slot = slots.get(0);
            drawItem(slot, group);
            slots.remove(slot);
        }
    }


    public void drawItem(int slot, @NonNull Group group) {
        val key = "donate_description_gui_" + group.name();

        val builder = ItemUtil.getBuilder(RankIcons.getRankIcon(group)).addItemFlags();

        builder.setName(language.getMessage(key));
        builder.setLore(language.getMessage(key + "_lore",
                "%prefix%", group.getPrefix() + getGamer().getName()));


        add(builder.build(), slot, (event) -> {
            if (group == Group.DEFAULT) {
                openBook(language.getMessage("donate_description_gui_purchase_book_donate",
                        "%rank%", group.getDisplayName()));
            } else {
                val rank = gamer.getRanks().findPriorityRank().rank();

                openBook(language.getMessage("donate_description_gui_purchase_book_subscription",
                        "%subscription%", group.getDisplayName(),
                        "%rank%", rank.getDisplayName(getLanguage())));
            }
        });
    }

    public void openBook(@NonNull String description) {
        PagedBook purchaseBook = new PagedBook();

        TextComponent component = new TextComponent(description);

        TextComponent urlComponent = new TextComponent(
                language.getMessage("donate_description_gui_purchase_book_url")
        );

        urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Env.getString("PROJECT_WEBSITE")));
        urlComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(language.getMessage("donate_description_gui_purchase_book_hover"))
                        }
                )
        );

        purchaseBook.addPage().addComponents(component, urlComponent);
        purchaseBook.openBook(getPlayer());
    }
}
