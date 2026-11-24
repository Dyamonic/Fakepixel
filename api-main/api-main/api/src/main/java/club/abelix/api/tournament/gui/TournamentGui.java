package club.abelix.api.tournament.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.tournament.service.TournamentService;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.json.JSON;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Master;
import club.abelix.tournament.grpc.RatingMode;
import club.abelix.tournament.grpc.TournamentInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class TournamentGui extends Gui {

    private static final int ROWS = 4;
    private static final int TOURNAMENT_SLOT = Gui.toSlot(4, 0);
    private static final int STATUS_SLOT = Gui.toSlot(4, 1);
    private static final int DESCRIPTION_SLOT = Gui.toSlot(4, 2);
    private static final int CLOSE_SLOT = Gui.toSlot(4, 3);

    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 17, 18, 26,
            27, 28, 29, 30, 32, 33, 34, 35
    };


    private static final List<ModeDefinition> ALL_MODE_DEFINITIONS = List.of(
            new ModeDefinition(Master.BW, RatingMode.RATING_MODE_BED_WARS, Material.BED, 0, Gui.toSlot(3, 1)),
            new ModeDefinition(Master.DUELS, RatingMode.RATING_MODE_DUELS, Material.IRON_SWORD, 0, Gui.toSlot(5, 1)),
            new ModeDefinition(Master.SW, RatingMode.RATING_MODE_SKY_WARS, Material.ENDER_PEARL, 0, Gui.toSlot(4, 1))
    );

    private final TournamentService service;
    private ViewState viewState = ViewState.loading();

    public TournamentGui(
            final @NonNull Gamer gamer,
            final @NonNull TournamentService service
    ) {
        super(
                gamer,
                Lang.of("tournament_gui_title"),
                ROWS
        );

        this.service = service;

        this.draw();
        this.loadData();
    }

    @Override
    public void draw() {
        removeRange(0, getSize());
        drawBorder(BORDER_SLOTS);
        generateCloseItem(CLOSE_SLOT);

        switch (viewState.status()) {
            case LOADING -> add(DATA_IS_LOADING_ICON, STATUS_SLOT);
            case READY -> {
                drawTournamentItem(viewState.tournament());
                drawDescriptionItem(viewState.tournament(), true);
                drawModeItems(viewState.modeCards());
            }
            case NO_ACTIVE -> {
                drawDescriptionItem(null, false);
                drawStatusItem(Material.BARRIER, "tournament_gui_status_no_active_name", "tournament_gui_status_no_active_lore");
            }
            case NOT_REGISTERED -> {
                drawTournamentItem(viewState.tournament());
                drawDescriptionItem(viewState.tournament(), false);
                drawStatusItem(Material.BARRIER, "tournament_gui_status_not_registered_name", "tournament_gui_status_not_registered_lore");
            }
            case ERROR -> {
                drawDescriptionItem(null, false);
                drawStatusItem(Material.BARRIER, "tournament_gui_status_error_name", "tournament_gui_status_error_lore");
            }
        }
    }

    private void loadData() {
        service.getCurrentTournament().whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("Failed to load current tournament for player {}", getGamer().getPlayerId(), throwable);
                applyViewState(ViewState.error());
                return;
            }

            if (!response.getHasActiveTournament() || !response.hasTournament()) {
                applyViewState(ViewState.noActive());
                return;
            }

            TournamentInfo tournament = response.getTournament();

            if (!service.isRegistered(getGamer(), tournament)) {
                applyViewState(ViewState.notRegistered(tournament));
                return;
            }

            loadRatings(tournament);
        });
    }

    private void loadRatings(@NonNull TournamentInfo tournament) {
        List<ModeDefinition> activeModes = activeModes(tournament);

        List<CompletableFuture<ModeCard>> futures = activeModes.stream()
                .map(mode -> service.getUserRating(getGamer(), mode.ratingMode())
                        .handle((ratingResponse, throwable) -> {
                            if (throwable != null) {
                                log.warn(
                                        "Failed to load tournament rating for player {} and mode {}",
                                        getGamer().getPlayerId(),
                                        mode.ratingMode(),
                                        throwable
                                );
                                return new ModeCard(mode, null);
                            }

                            return new ModeCard(mode, ratingResponse.getRating());
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, throwable) -> {
                    EnumMap<Master, ModeCard> modeCards = new EnumMap<>(Master.class);

                    for (CompletableFuture<ModeCard> future : futures) {
                        ModeCard modeCard = future.join();
                        modeCards.put(modeCard.definition().master(), modeCard);
                    }

                    applyViewState(ViewState.ready(tournament, modeCards));
                });
    }

    private void applyViewState(@NonNull ViewState nextState) {
        HeartBeat.runSync(() -> {
            viewState = nextState;

            if (isOpened()) {
                draw();
                getPlayer().updateInventory();
            }
        });
    }

    private void drawTournamentItem(@NonNull TournamentInfo tournament) {
        List<String> lore = new ArrayList<>();
        String description = translated(tournament.getDescription());
        val tournamentLink = getLanguage().getMessage("tournament_info_link");

        if (!description.isBlank() && !Objects.equals(description, tournament.getDescription())) {
            appendLore(lore, description);
            lore.add(" ");
        }

        lore.addAll(translatedLore("tournament_gui_tournament_lore", "%link%", tournamentLink));

        add(ItemUtil.getBuilder(Material.NETHER_STAR)
                .glowing()
                .setName(translated(tournament.getTitle()))
                .setLore(lore)
                .build(), TOURNAMENT_SLOT, (e) -> {
            getGamer().sendMessage("\n");
            getGamer().sendRawMessage(JSON.CLICK_URL_LAYOUT.formatted(tournamentLink, tournamentLink));
            getGamer().sendMessage("\n");
        });
    }

    private void drawDescriptionItem(TournamentInfo tournament, boolean registered) {
        List<String> lore = new ArrayList<>();
        val tournamentLink = getLanguage().getMessage("tournament_info_link");

        if (tournament != null && !tournament.getRatingModesList().isEmpty()) {

            List<String> modeNames = activeModes(tournament).stream()
                    .map(d -> d.master().getDisplayName())
                    .toList();
            String modesStr = String.join(", ", modeNames);
            lore.addAll(translatedLore("tournament_gui_info_lore", "%modes%", modesStr, "%link%", tournamentLink));
        } else {
            lore.addAll(translatedLore("tournament_gui_info_lore_no_modes", "%link%", tournamentLink));
        }

        if (!registered) {
            lore.add(" ");
            lore.addAll(translatedLore("tournament_gui_info_lore_not_registered"));
        }

        add(ItemUtil.getBuilder(Material.BOOK)
                .setName(translated("tournament_gui_info_name"))
                .setLore(lore)
                .build(), DESCRIPTION_SLOT, (e) -> {
            getGamer().sendMessage("\n");
            getGamer().sendRawMessage(JSON.CLICK_URL_LAYOUT.formatted(tournamentLink, tournamentLink));
            getGamer().sendMessage("\n");
        });
    }

    private void drawModeItems(@NonNull EnumMap<Master, ModeCard> modeCards) {
        for (ModeCard modeCard : modeCards.values()) {
            ModeDefinition definition = modeCard.definition();
            Long rating = modeCard.rating();

            List<String> lore = translatedLore(
                    "tournament_gui_mode_lore",
                    "%rating%", formatRating(rating)
            );

            ItemUtil.Builder builder = ItemUtil.getBuilder(definition.material())
                    .setDurability(definition.durability())
                    .setName(translated("tournament_gui_mode_name", "%mode%", definition.master().getDisplayName()))
                    .setLore(lore);

            if (rating != null && rating > 0) {
                builder.glowing();
            }

            add(builder.build(), definition.slot());
        }
    }

    private void drawStatusItem(@NonNull Material material, @NonNull String nameKey, @NonNull String loreKey) {
        add(ItemUtil.getBuilder(material)
                .setName(translated(nameKey))
                .setLore(translatedLore(loreKey))
                .build(), STATUS_SLOT);
    }

    private String formatRating(Long rating) {
        if (rating == null) {
            return translated("tournament_gui_rating_unavailable");
        }

        return Leveling.formatGrouped(rating.intValue());
    }

    private void appendLore(@NonNull List<String> lore, @NonNull String block) {
        block.lines().forEach(line -> lore.add(line.isEmpty() ? " " : line));
    }

    private static List<ModeDefinition> activeModes(@NonNull TournamentInfo tournament) {
        List<RatingMode> tournamentModes = tournament.getRatingModesList();
        if (tournamentModes.isEmpty()) {
            return ALL_MODE_DEFINITIONS;
        }
        return ALL_MODE_DEFINITIONS.stream()
                .filter(d -> tournamentModes.contains(d.ratingMode()))
                .toList();
    }

    private record ModeDefinition(
            Master master,
            RatingMode ratingMode,
            Material material,
            int durability,
            int slot
    ) {
    }

    private record ModeCard(ModeDefinition definition, Long rating) {
    }

    private record ViewState(State status, TournamentInfo tournament, EnumMap<Master, ModeCard> modeCards) {

        private static ViewState loading() {
            return new ViewState(State.LOADING, null, new EnumMap<>(Master.class));
        }

        private static ViewState ready(TournamentInfo tournament, EnumMap<Master, ModeCard> modeCards) {
            return new ViewState(State.READY, tournament, modeCards);
        }

        private static ViewState noActive() {
            return new ViewState(State.NO_ACTIVE, null, new EnumMap<>(Master.class));
        }

        private static ViewState notRegistered(TournamentInfo tournament) {
            return new ViewState(State.NOT_REGISTERED, tournament, new EnumMap<>(Master.class));
        }

        private static ViewState error() {
            return new ViewState(State.ERROR, null, new EnumMap<>(Master.class));
        }
    }

    private enum State {
        LOADING,
        READY,
        NO_ACTIVE,
        NOT_REGISTERED,
        ERROR
    }
}
