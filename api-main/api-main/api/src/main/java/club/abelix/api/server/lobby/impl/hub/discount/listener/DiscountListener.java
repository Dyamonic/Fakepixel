package club.abelix.api.server.lobby.impl.hub.discount.listener;

import club.abelix.api.base.cron.CronService;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.api.server.lobby.impl.hub.discount.DiscountApiService;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class DiscountListener implements Listener {

    CronService cronService;

    DiscountApiService discountService;

    public DiscountListener(DiscountApiService discountService) {
        this.discountService = discountService;
        this.cronService = new CronService(StarshipBukkit.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGamerJoin(GamerJoinEvent event) {
        val gamer = event.getGamer();

        if (discountService.getCurrentAdvertisement().isEmpty()) {
            return;
        }

        val activeAdvertisement = discountService.getCurrentAdvertisement().get();

        if (LocalDateTime.now(ZoneId.of("Europe/Moscow")).isAfter(activeAdvertisement.expiresAt())) {
            return;
        }

        this.cronService.check(gamer, "discount", Duration.ofHours(1)).thenAccept(result -> {
            if (!result) return;
            val language = gamer.getLanguage();
            val pagedBook = new PagedBook();
            val mainText = new TextComponent(language.getMessage("discount_page_active_advertisement_text",
                    "%percent%", (int) activeAdvertisement.promoCode().discount(),
                    "%code%", activeAdvertisement.promoCode().code()));

            val emptyComponent = new TextComponent("\n");

            val urlComponent = new TextComponent(
                    language.getMessage("discount_page_active_advertisement_url_text",
                            "%percent%", (int) activeAdvertisement.promoCode().discount(),
                            "%code%", activeAdvertisement.promoCode().code())
            );

            urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, language.getMessage("donate_website_url")));
            urlComponent.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new BaseComponent[]{
                                    new TextComponent(ChatColor.YELLOW + language.getMessage("discount_page_active_advertisement_url_hover",
                                            "%percent%", (int) activeAdvertisement.promoCode().discount(),
                                            "%code%", activeAdvertisement.promoCode().code()))
                            }
                    )
            );
            pagedBook.addPage().addComponents(mainText, emptyComponent, urlComponent);
            HeartBeat.runSync(() -> pagedBook.openBook(gamer.getPlayer()));
        });
    }
}
