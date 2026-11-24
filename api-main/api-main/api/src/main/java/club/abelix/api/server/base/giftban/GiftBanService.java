package club.abelix.api.server.base.giftban;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CBanPlayer;
import lombok.val;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GiftBanService {

    private final Map<UUID, GiftBanRequest> pendingGiftBans = new HashMap<>();

    public boolean hasPendingGiftBan(
            Gamer gamer
    ) {
        return pendingGiftBans.values()
                .stream()
                .anyMatch(p -> p.target().equals(gamer));
    }

    public void openExist(Gamer gamer) {
        val request = pendingGiftBans.values()
                .stream()
                .filter(p -> p.target().equals(gamer))
                .findFirst();

        request.ifPresent(r -> {
            val book = createBook(
                    pendingGiftBans.entrySet()
                            .stream()
                            .filter(e -> e.getValue().equals(r))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow(),
                    r.sender(),
                    r.target(),
                    r.reason()
            );

            book.openBook(gamer.getPlayer());
        });
    }

    public void create(
            Gamer sender,
            Gamer target,
            String reason
    ) {
        val uuid = UUID.randomUUID();
        val book = createBook(uuid, sender, target, reason);

        pendingGiftBans.put(uuid, new GiftBanRequest(
                sender,
                target,
                reason
        ));

        book.openBook(target.getPlayer());
    }

    private PagedBook createBook(
            UUID uuid,
            Gamer sender,
            Gamer target,
            String reason
    ) {
        val language = target.getLanguage();
        val pagedBook = new PagedBook();
        val mainText = new TextComponent(language.getMessage("gift_ban_book_main_text", "%sender%", sender.getDisplayName(), "%reason%", reason));

        val yesButton = new TextComponent(language.getMessage("gift_ban_book_yes_button"));

        yesButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/giftban confirm " + uuid));

        val support = new TextComponent(language.getMessage("gift_ban_book_support_button"));

        support.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.youtube.com/watch?v=xvFZjo5PgG0"));

        pagedBook.addPage().addComponents(mainText, yesButton, yesButton.duplicate(), support);
        return pagedBook;
    }

    public void process(
            Gamer gamer
    ) {
        val uuid = pendingGiftBans.entrySet()
                .stream()
                .filter(e -> e.getValue().target().equals(gamer))
                .map(Map.Entry::getKey)
                .findFirst();

        uuid.ifPresent(id -> process(id, gamer));
    }

    public void process(
            UUID requestId,
            Gamer sender
    ) {
        if (!pendingGiftBans.containsKey(requestId)) {
            sender.sendMessage("No pending gift ban found for you :(");
            return;
        }

        sender.sendMessage("Thanks for confirming the gift ban!");
        HeartBeat.runLaterSync(() -> {
            val pendingBan = pendingGiftBans.get(requestId);

            Connection.sendPacket(new CBanPlayer(
                    pendingBan.sender().getName(),
                    pendingBan.target().getName(),
                    pendingBan.reason(),
                    -1
            ));

            pendingGiftBans.remove(requestId);
        }, 10);
    }

    private record GiftBanRequest(
            Gamer sender,
            Gamer target,
            String reason
    ) {
    }

}
