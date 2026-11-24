package club.abelix.api.tournament.npc;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.tournament.RemoteTournamentClient;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.api.tournament.service.TournamentService;
import club.abelix.api.tournament.telegram.TelegramChannelSubscriptionClient;
import club.abelix.api.tournament.top.TournamentPlayerTopProvider;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Lobby;
import club.abelix.tournament.grpc.GetCurrentTournamentResponse;
import club.abelix.tournament.grpc.RatingMode;
import org.jetbrains.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public final class TournamentNpcTask extends AbstractTask {

    private static final long INTERVAL_TICKS = 15 * 20;

    private final TournamentClient client;
    private final TournamentService service;
    private final TelegramChannelSubscriptionClient telegramSubscriptionClient;
    private final ConfigurationSection tournamentTopSection;

    private final AtomicReference<String> spawnedForTournamentId = new AtomicReference<>();

    public TournamentNpcTask(
            TaskRegistrationProvider provider,
            TournamentClient client,
            TournamentService service,
            TelegramChannelSubscriptionClient telegramSubscriptionClient,
            ConfigurationSection tournamentTopSection
    ) {
        super(provider);
        this.client = client;
        this.service = service;
        this.telegramSubscriptionClient = telegramSubscriptionClient;
        this.tournamentTopSection = tournamentTopSection;
    }

    public void startChecking() {
        async().timer(0, INTERVAL_TICKS);
    }

    @Override
    protected void run() {
        // Сбрасываем кэш перед каждой проверкой, чтобы получить актуальные данные с бэкенда —
        // особенно важно, если турнир добавили напрямую в БД без команды.
        if (client instanceof RemoteTournamentClient remote) {
            remote.invalidateCurrentTournamentCache();
        }

        client.getCurrentTournament().whenComplete((response, err) -> {
            if (err != null) {
                log.error("TournamentNpcTask: failed to fetch current tournament", err);
                return;
            }

            if (!response.getHasActiveTournament() || !response.hasTournament()) {
                return;
            }

            val tournamentId = response.getTournament().getId();

            val serverRatingMode = resolveServerRatingMode();
            if (serverRatingMode == null || !response.getTournament().getRatingModesList().contains(serverRatingMode)) {
                log.info("TournamentNpcTask: tournament [id={}] does not include server mode {}, skipping",
                        tournamentId, serverRatingMode);
                return;
            }

            if (!spawnedForTournamentId.compareAndSet(null, tournamentId)) {
                if (spawnedForTournamentId.get().equals(tournamentId)) {
                    return;
                }

                spawnedForTournamentId.set(tournamentId);
            }

            log.info("TournamentNpcTask: new active tournament [id={}], spawning NPCs and tops", tournamentId);
            HeartBeat.runSync(() -> spawnNpcAndTops(response));
            HeartBeat.runSync(() -> notifyPlayers(response));
        });
    }

    private void notifyPlayers(GetCurrentTournamentResponse response) {
        for (val gamer : Gamer.getGamers()) {
            gamer.playSound(YSound.CAT_MEOW);
            gamer.sendLocalizedMessage(
                    "tournament_started",
                    "%name%",
                    gamer.getLanguage().getMessage(
                            response.getTournament().getTitle()
                    )
            );
        }
    }

    private void spawnNpcAndTops(GetCurrentTournamentResponse response) {
        val tops = spawnTops();

        // Топы показываются через PlayerTopListener.onJoin, но если турнир появился
        // когда игроки уже онлайн — нужно показать им топы вручную.
        if (!tops.isEmpty()) {
            val onlineGamers = Gamer.getGamers();
            for (val top : tops) {
                for (val gamer : onlineGamers) {
                    top.showTo(gamer);
                }
            }
        }

        val npcSection = tournamentTopSection.getConfigurationSection("Npc");
        if (npcSection != null) {
            new TournamentJoinNpc(service, npcSection, telegramSubscriptionClient, response).create();
            log.info("TournamentNpcTask: join NPC spawned for tournament [id={}]", response.getTournament().getId());
        }
    }

    private List<PlayerTop<?>> spawnTops() {
        val created = new ArrayList<PlayerTop<?>>();

        for (val topSection : getTournamentTopSections()) {
            try {
                val mode = parseRatingMode(topSection.getString("Mode"));
                val provider = TournamentPlayerTopProvider.create(mode, client);
                val location = LocationUtil.stringToLocation(topSection.getString("Location"));

                val top = PlayerTopBuilder.defaultTopBuilder()
                        .provider(provider)
                        .localizationKey(topSection.getString("LocalizationKey", "tournament_top"))
                        .location(location)
                        .maxPlayers(topSection.getInt("MaxPlayers", 10))
                        .updateInterval(topSection.getInt("UpdateInterval",
                                tournamentTopSection.getInt("UpdateInterval", HeartBeat.TEN_MINUTES)))
                        .create();

                new TournamentTopSpawnEffect(location).play();
                created.add(top);
                log.info("TournamentNpcTask: top spawned for mode {}", mode);
            } catch (Exception e) {
                log.error("TournamentNpcTask: failed to spawn top from section {}", topSection.getCurrentPath(), e);
            }
        }

        return created;
    }

    private List<ConfigurationSection> getTournamentTopSections() {
        val topsSection = tournamentTopSection.getConfigurationSection("Tops");

        if (topsSection == null) {
            return tournamentTopSection.contains("Mode")
                    ? List.of(tournamentTopSection)
                    : List.of();
        }

        return topsSection.getKeys(false).stream()
                .map(topsSection::getConfigurationSection)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private static RatingMode resolveServerRatingMode() {
        val mode = CoreUtil.getMode();
        if (!mode.isGameLobby()) return null;
        val master = ((Lobby) mode).getMaster();
        if (master == null) return null;
        return switch (master) {
            case BW    -> RatingMode.RATING_MODE_BED_WARS;
            case SW    -> RatingMode.RATING_MODE_SKY_WARS;
            case DUELS -> RatingMode.RATING_MODE_DUELS;
            default    -> null;
        };
    }

    private static RatingMode parseRatingMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            throw new IllegalArgumentException("Tournament top mode is not configured");
        }

        return switch (rawMode.trim().toUpperCase()) {
            case "BEDWARS", "BED_WARS" -> RatingMode.RATING_MODE_BED_WARS;
            case "SKYWARS", "SKY_WARS" -> RatingMode.RATING_MODE_SKY_WARS;
            case "DUELS" -> RatingMode.RATING_MODE_DUELS;
            default -> throw new IllegalArgumentException("Unknown RatingMode: '" + rawMode
                    + "'. Available: " + Arrays.stream(RatingMode.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        };
    }
}
