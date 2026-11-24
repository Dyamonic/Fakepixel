package club.abelix.api.tournament.npc;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hovering.HoveringItemsEffect;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.tournament.gui.TournamentSocialLinksGui;
import club.abelix.api.tournament.service.TournamentService;
import club.abelix.api.tournament.telegram.TelegramChannelSubscriptionClient;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.tournament.grpc.GetCurrentTournamentResponse;
import club.abelix.tournament.grpc.TournamentInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TournamentJoinNpc {

    private static final String TOURNAMENT_JOIN_COOLDOWN = "tournament/join/cooldown";

    TournamentService service;
    ConfigurationSection section;
    TelegramChannelSubscriptionClient telegramSubscriptionClient;
    GetCurrentTournamentResponse currentResponse;

    public void create() {
        val current = currentResponse.getTournament();
        val mode = CoreUtil.getMode();
        val item = selectItemStack(mode);
        val socialLinks = readSocialLinks();
        val skin = new Skin(
                "abelix_tournament",
                UUID.randomUUID().toString(),
                section.getString("Skin.Texture"),
                section.getString("Skin.Signature")
        );
        val location = LocationUtil.stringToLocation(section.getString("Location"));
        val npc = FalconAPI.getNmsApi()
                .getFakeEntityApi()
                .createPlayer(location, skin);
        val action = clickAction(socialLinks, current);

        npc.setClickAction(action);
        npc.setPublicStatus(true);

        val hologram = new Hologram(location.clone().add(0, 1, 0));

        hologram.addTextLine(Lang.of(current.getTitle()).compile());
        hologram.addTextLine(Lang.of("tournament_hologram_line_action").compile());

        hologram.setPublicStatus(true);

        new HoveringItemsEffect(location.clone())
                .createEffect(
                        item.getType(),
                        action
                );
    }

    private static ItemStack selectItemStack(ServerMode mode) {
        if (mode.isGameLobby() && ((Lobby) mode).getMaster() != null) {
            val master = ((Lobby) mode).getMaster();

            return switch (master) {
                case BW -> new ItemStack(Material.DIAMOND_SWORD);
                case DUELS -> new ItemStack(Material.IRON_SWORD);
                case SW -> new ItemStack(Material.ENDER_PEARL);
                default -> throw new IllegalStateException("Unexpected value: " + mode);
            };
        }

        throw new IllegalStateException("Unexpected value: " + mode);
    }

    private Consumer<Player> clickAction(
            List<TournamentSocialLinksGui.SocialLink> socialLinks,
            TournamentInfo info
    ) {
        return player -> Optional.ofNullable(Gamer.getGamer(player))
                .ifPresent(gamer -> {
                    if (service.isRegistered(gamer, info)) {
                        openGui(gamer);
                        return;
                    }

                    if (gamer.getCooldown(TOURNAMENT_JOIN_COOLDOWN).isActive()) {
                        gamer.sendLocalizedMessage("tournament_join_cooldown");
                        gamer.playSound(YSound.VILLAGER_NO);
                        return;
                    }

                    gamer.setCooldown(TOURNAMENT_JOIN_COOLDOWN, 10, TimeUnit.SECONDS);

                    if (!socialLinks.isEmpty()) {
                        HeartBeat.runSync(() ->
                                new TournamentSocialLinksGui(
                                        gamer,
                                        socialLinks,
                                        telegramSubscriptionClient,
                                        () -> register(gamer, info)
                                )
                        );
                    } else {
                        register(gamer, info);
                    }
                });
    }

    private void register(Gamer gamer, TournamentInfo info) {
        service.register(gamer, currentResponse.getTournament())
                .thenAccept(res -> {
                    if (!res) {
                        if (service.isRegistered(gamer, info)) {
                            openGui(gamer);
                        } else {
                            gamer.sendLocalizedMessage("tournament_registration_already_registered");
                            gamer.playSound(YSound.VILLAGER_NO);
                        }
                        return;
                    }

                    gamer.playSound(YSound.VILLAGER_YES);
                    gamer.sendLocalizedMessage("tournament_registration_success");
                });
    }

    private void openGui(Gamer gamer) {
        HeartBeat.runSync(() -> service.openTournamentGui(gamer));
    }

    /**
     * Читает список социальных ссылок из конфига.
     * Формат:
     * <pre>
     * SocialLinks:
     *   - Material: PAPER
     *     NameKey: tournament_social_telegram_name
     *     LoreKey: tournament_social_telegram_lore
     *     Url: "https://t.me/abelix"
     *     ChannelId: "@abelixgg"   # опционально — Telegram ID канала для проверки подписки
     * </pre>
     */
    private List<TournamentSocialLinksGui.SocialLink> readSocialLinks() {
        val rawLinks = section.getMapList("SocialLinks");

        if (rawLinks == null || rawLinks.isEmpty()) {
            return List.of();
        }

        List<TournamentSocialLinksGui.SocialLink> links = new ArrayList<>(rawLinks.size());

        for (val map : rawLinks) {
            try {
                Material material = Material.valueOf(String.valueOf(map.get("Material")).toUpperCase());
                String nameKey = String.valueOf(map.get("NameKey"));
                String loreKey = String.valueOf(map.get("LoreKey"));
                String url = String.valueOf(map.get("Url"));
                String channelId = normalizeChatId(map.get("ChannelId"));

                links.add(new TournamentSocialLinksGui.SocialLink(material, nameKey, loreKey, url, channelId));
            } catch (Exception e) {
                log.warn("Skipping invalid tournament social link entry: {}", map, e);
            }
        }

        return List.copyOf(links);
    }

    private static @Nullable String normalizeChatId(@Nullable Object rawChannel) {
        if (rawChannel == null) {
            return null;
        }

        if (rawChannel instanceof Number number) {
            return normalizeNumericChatId(number.longValue());
        }

        String value = String.valueOf(rawChannel).trim();

        if (value.isEmpty() || value.equalsIgnoreCase("null")) {
            return null;
        }

        if (value.startsWith("@")) {
            return value;
        }

        try {
            return normalizeNumericChatId(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static String normalizeNumericChatId(long rawChatId) {
        if (rawChatId < 0) {
            return Long.toString(rawChatId);
        }

        return "-100" + rawChatId;
    }
}
