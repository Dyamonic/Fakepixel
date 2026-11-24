package club.abelix.api.tournament.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.tournament.telegram.TelegramChannelSubscriptionClient;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.json.JSON;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class TournamentSocialLinksGui extends Gui {

    private static final int ROWS = 3;
    private static final int CONFIRM_SLOT = Gui.toSlot(3, 2);
    private static final int CLOSE_SLOT = Gui.toSlot(5, 2);
    private static final int INFO_SLOT = Gui.toSlot(4, 2);

    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private static final int[] SOCIAL_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final List<SocialLink> socialLinks;
    private final TelegramChannelSubscriptionClient telegramSubscriptionClient;
    private final Runnable onConfirm;

    public TournamentSocialLinksGui(
            final @NonNull Gamer gamer,
            final @NonNull List<SocialLink> socialLinks,
            final @NonNull TelegramChannelSubscriptionClient telegramSubscriptionClient,
            final @NonNull Runnable onConfirm
    ) {
        super(gamer, Lang.of("tournament_social_links_gui_title"), ROWS);
        this.socialLinks = socialLinks;
        this.telegramSubscriptionClient = telegramSubscriptionClient;
        this.onConfirm = onConfirm;
        this.draw();
    }

    @Override
    public void draw() {
        removeRange(0, getSize());
        drawBorder(BORDER_SLOTS);
        drawSocialLinks();
        drawInfoButton();
        drawConfirmButton();
        generateCloseItem(CLOSE_SLOT);
    }

    private void drawSocialLinks() {
        for (int i = 0; i < socialLinks.size() && i < SOCIAL_SLOTS.length; i++) {
            SocialLink link = socialLinks.get(i);
            int slot = SOCIAL_SLOTS[i];

            add(ItemUtil.getBuilder(link.material())
                    .setName(translated(link.nameKey()))
                    .setLore(translatedLore(link.loreKey(), "%url%", link.url()))
                    .build(), slot, action -> {
                gamer.sendMessage("\n");
                gamer.sendRawMessage(JSON.CLICK_URL_LAYOUT.formatted(link.url(), link.url()));
                gamer.sendMessage("\n");
            });
        }
    }

    private void drawInfoButton() {
        add(ItemUtil.getBuilder(Material.BOOK)
                .setName(translated("tournament_social_links_info_name"))
                .setLore(translatedLore("tournament_social_links_info_lore"))
                .build(), INFO_SLOT);
    }

    private void drawConfirmButton() {
        add(ItemUtil.getBuilder(Material.EMERALD)
                .glowing()
                .setName(translated("tournament_social_links_confirm_name"))
                .setLore(translatedLore("tournament_social_links_confirm_lore"))
                .build(), CONFIRM_SLOT, e -> handleConfirm());
    }

    private void handleConfirm() {
        getGamer().playSound(YSound.NOTE_PLING);

        List<SocialLink> linksWithChannel = socialLinks.stream()
                .filter(SocialLink::requiresSubscription)
                .toList();

        // Нет каналов для проверки — сразу регистрируем
        if (linksWithChannel.isEmpty()) {
            getPlayer().closeInventory();
            onConfirm.run();
            return;
        }

        if (!telegramSubscriptionClient.isConfigured()) {
            log.warn("TournamentSocialLinksGui: Telegram subscription client is not configured");
            getGamer().sendLocalizedMessage("internal_error");
            getGamer().playSound(YSound.VILLAGER_NO);
            return;
        }

        if (!getGamer().isAuthDataLoaded()) {
            getGamer().loadAuthData();
        }

        if (!getGamer().isAuthDataLoaded()) {
            getGamer().sendLocalizedMessage("internal_error");
            getGamer().playSound(YSound.VILLAGER_NO);
            return;
        }

        val authData = getGamer().getAuthData();

        if (!authData.has2FA()) {
            getGamer().sendLocalizedMessage("tournament_2fa_required_to_join_tournament");
            getGamer().playSound(YSound.VILLAGER_NO);
            return;
        }

        val telegramUserId = authData.getTelegramId();

        List<CompletableFuture<TelegramChannelSubscriptionClient.SubscriptionCheckResult>> futures =
                linksWithChannel.stream()
                        .map(link -> telegramSubscriptionClient.checkSubscription(link.channelId(), telegramUserId))
                        .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        log.warn("TournamentSocialLinksGui: subscription check error for player {}",
                                getGamer().getPlayerId(), throwable);
                        HeartBeat.runSync(() -> {
                            getGamer().sendLocalizedMessage("internal_error");
                            getGamer().playSound(YSound.VILLAGER_NO);
                        });
                        return;
                    }

                    for (var future : futures) {
                        var result = future.join();

                        switch (result.status()) {
                            case SUBSCRIBED -> { /* ок */ }
                            case NOT_SUBSCRIBED -> {
                                HeartBeat.runSync(() -> {
                                    getGamer().sendLocalizedMessage("tournament_not_subscribed");
                                    getGamer().playSound(YSound.VILLAGER_NO);
                                });
                                return;
                            }
                            case RATE_LIMITED -> {
                                log.warn("TournamentSocialLinksGui: Telegram rate limit during subscription check for player {}. Allowing registration. Retry after: {}",
                                        getGamer().getPlayerId(), result.retryAfterSeconds());
                            }
                            case FAILED -> {
                                log.warn("TournamentSocialLinksGui: subscription check failed for player {}: {}",
                                        getGamer().getPlayerId(), result.description());
                            }
                        }
                    }

                    // Все подписки подтверждены
                    HeartBeat.runSync(() -> {
                        getPlayer().closeInventory();
                        onConfirm.run();
                    });
                });
    }

    /**
     * @param material  иконка в GUI
     * @param nameKey   ключ локализации названия
     * @param loreKey   ключ локализации описания (доступна переменная %url%)
     * @param url       ссылка (подставляется в lore через %url%)
     * @param channelId ID Telegram-канала для проверки подписки (null — проверка не требуется)
     */
    public record SocialLink(
            Material material,
            String nameKey,
            String loreKey,
            String url,
            @Nullable String channelId
    ) {
        /**
         * Возвращает true, если для этой ссылки нужна проверка подписки.
         */
        public boolean requiresSubscription() {
            return channelId != null && !channelId.isBlank();
        }
    }
}
