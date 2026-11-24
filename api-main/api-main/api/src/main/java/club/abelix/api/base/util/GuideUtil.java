package club.abelix.api.base.util;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.commons.language.Language;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

@UtilityClass
public class GuideUtil {
    public void activateBook(Gamer gamer) {
        Language language = gamer.getLanguage();
        PagedBook pagedBook = new PagedBook();
        val page_1 = new TextComponent(language.getMessage("guide_book_page_1"));
        val page_2_discord = new TextComponent(language.getMessage("guide_book_page_2_discord"));
        val page_2_email = new TextComponent(language.getMessage("guide_book_page_2_email"));
        val page_2_store = new TextComponent(language.getMessage("guide_book_page_2_store"));

        val emptyComponent = new TextComponent("\n");

        val urlComponent = new TextComponent(
                language.getMessage("guide_book_store")
        );

        urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, language.getMessage("donate_website_url")));
        urlComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(ChatColor.YELLOW + language.getMessage("donate_website_url"))
                        }
                )
        );

        val discordComponent = new TextComponent(
                language.getMessage("guide_book_discord")
        );

        discordComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://ap.a4x.cc/discord"));
        discordComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(ChatColor.YELLOW  + "https://ap.a4x.cc/discord")
                        }
                )
        );

        val emailComponent = new TextComponent(
                language.getMessage("guide_book_email")
        );

        emailComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(ChatColor.YELLOW  + "agera@abelix.team")
                        }
                )
        );

        pagedBook.addPage().addComponents(page_1);
        pagedBook.addPage().addComponents(page_2_discord, emptyComponent, discordComponent, emptyComponent, page_2_email, emptyComponent, emailComponent, emptyComponent, page_2_store,emptyComponent, urlComponent);
        pagedBook.openBook(gamer.getPlayer());
    }
}
