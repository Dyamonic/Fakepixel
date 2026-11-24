package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

@NamedCommand("axclient")
@IgnoreLogs
public class AxClientCommand {

    @Executor
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Language language = gamer.getLanguage();
        PagedBook pagedBook = new PagedBook();
        val page_1 = new TextComponent(language.getMessage("axclient_book_text"));

        val emptyComponent = new TextComponent("\n");

        val urlComponent = new TextComponent(
                language.getMessage("axclient_book_url_text")
        );

        urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, language.getMessage("axclient_url")));
        urlComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(ChatColor.YELLOW + language.getMessage("axclient_book_url_hool"))
                        }
                )
        );

        pagedBook.addPage().addComponents(page_1, emptyComponent, urlComponent);
        pagedBook.openBook(gamer.getPlayer());
    }
}
