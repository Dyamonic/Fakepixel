package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.api.server.lobby.gift.GiftManager;
import club.abelix.core.bukkit.StarshipBukkit;
import com.google.common.base.Joiner;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class GiftBookGui {

    public GiftBookGui(@NonNull Gamer gamer, @NonNull GiftManager.PendingGift gift) {
        openBook(gamer, gift);
    }

    public void openBook(@NonNull Gamer to, @NonNull GiftManager.PendingGift gift) {
        val lang = to.getLanguage();
        PagedBook purchaseBook = new PagedBook();

        Gamer from = gift.sender();

        val rank = StarshipBukkit.getInstance().getRanksService().findRankById(gift.type().getRankId())
                .orElse(null);

        if (rank != null) {
            TextComponent component = new TextComponent(Joiner.on("\n").join(lang.getMessageList("gift_book_text",
                    "%from%", from.getDisplayName(),
                    "%group%", rank.getDisplayName(to.getLanguage())
            )));

            TextComponent acceptComponent = new TextComponent(lang.getMessage("accept_gift_button"));
            acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gift accept " + from.getName()));
            acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {
                    new TextComponent("/gift accept " + from.getName())
            }));

            TextComponent empty = new TextComponent("\n\n");

            TextComponent denyComponent = new TextComponent(lang.getMessage("deny_gift_button"));
            denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gift deny " + from.getName()));
            denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {
                    new TextComponent("/gift deny " + from.getName())
            }));

            purchaseBook.addPage().addComponents(component, empty, acceptComponent, empty, denyComponent);
            purchaseBook.openBook(to.getPlayer());
        }
    }

}
