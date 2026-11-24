package club.abelix.api.server.lobby.gamehistory;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamehistory.GameHistoryService;
import club.abelix.api.base.gamehistory.model.GameInfo;
import club.abelix.api.base.gamehistory.model.PlayerInfo;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GameHistoryLobbyGui extends Gui {

    private static final int ROWS = 6;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final Gamer target;
    private List<GameInfo> games = Collections.emptyList();
    private boolean loaded = false;
    
    private final PagedZone<GameInfo> pagedZone = new PagedZone<>(this) {
        @Override
        protected Collection<GameInfo> getItems() {
            return games;
        }

        @Override
        protected void drawItem(GameInfo game, int slot) {
            GameHistoryLobbyGui.this.drawGameItem(game, slot);
        }
    };

    public static void open(@NonNull Gamer gamer) {
        open(gamer, null);
    }

    public static void open(@NonNull Gamer gamer, @Nullable Gui parent) {
        openOther(gamer, gamer, parent);
    }

    public static void openOther(@NonNull Gamer viewer, @NonNull Gamer target) {
        openOther(viewer, target, null);
    }

    public static void openOther(@NonNull Gamer viewer, @NonNull Gamer target, @Nullable Gui parent) {
        HeartBeat.runSync(() -> new GameHistoryLobbyGui(viewer.getPlayer(), target, parent));
    }

    private GameHistoryLobbyGui(
            @NonNull Player viewer,
            @NonNull Gamer target,
            @Nullable Gui parent
    ) {
        super(viewer, (viewer.getName().equalsIgnoreCase(target.getPlayerName())
                ? Lang.of("game_history_title").compile()
                : Lang.of("game_history_title_other").set("%player%", target.getPlayerName()).compile()), ROWS);

        this.target = target;
        setParent(parent);
        pagedZone.addSlotRect(1, 1, 7, 4);

        draw();
        loadGames();
    }

    private void loadGames() {
        GameHistoryService service = FalconAPI.getInstance().getGameHistoryService();
        if (service == null) {
            loaded = true;
            HeartBeat.runSync(this::draw);
            return;
        }

        service.getGamesByPlayer(target, 0, 100).whenComplete((page, ex) -> {
            if (ex != null) {
                loaded = true;
                HeartBeat.runSync(this::draw);
                return;
            }

            games = page.getGames() != null ? page.getGames() : Collections.emptyList();
            loaded = true;

            HeartBeat.runSync(() -> {
                if (isOpened()) {
                    draw();
                }
            });
        });
    }

    @Override
    public void draw() {
        for (int i = 0; i < getSize(); i++) {
            remove(i);
        }

        drawBorder(BORDER_SLOTS_FOR_SIX_ROWS_GUI);
        if (getParent() != null) {
            generateBackItem(48, e -> getParent().reopen());
            generateCloseItem(49);
        } else {
            generateCloseItem();
        }

        if (!loaded) {
            add(DATA_IS_LOADING_ICON, toSlot(4, 3));
            return;
        }

        if (games.isEmpty()) {
            ItemStack noGames = ItemUtil.getBuilder(Material.WOOD_SWORD)
                    .setName(Lang.of("game_history_no_games_name"))
                    .setLore(Lang.of("game_history_no_games_lore"))
                    .build();
            add(noGames, toSlot(4, 3));
            return;
        }

        pagedZone.draw();
        pagedZone.drawButtons();
    }

    private void drawGameItem(GameInfo game, int slot) {
        boolean isWinner = game.getWinners().stream()
                .anyMatch(p -> p.getPlayerId() == target.getPlayerId());

        Material material = isWinner ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
        String modeDisplayName = resolveModeDisplayName(game);
        String mapDisplayName = resolveMapDisplayName(game);

        long durationMs = game.getEndTime() - game.getStartTime();
        String durationStr = formatDuration(durationMs);
        String dateStr;
        synchronized (DATE_FORMAT) {
            dateStr = DATE_FORMAT.format(new Date(game.getStartTime()));
        }

        String winnersStr = game.getWinners().stream()
                .map(PlayerInfo::getPlayerName)
                .collect(Collectors.joining(", "));
        if (winnersStr.isEmpty()) {
            winnersStr = translated("game_history_value_none");
        }

        String winState = "game_history_entry_lose";

        if (isWinner) {
            winState = "game_history_entry_win";
        }

        ItemStack icon = ItemUtil.getBuilder(material)
                .setName(language.getMessage((isWinner ? "game_history_entry_title_win" : "game_history_entry_title_lose"),
                        "%mode%", modeDisplayName, "%map%", mapDisplayName))
                .setLore(language.getMessageList("game_history_entry", "%date%", dateStr,
                        "%mode%", modeDisplayName,
                        "%map%", mapDisplayName,
                        "%player_count%", game.getTotalPlayersCount(),
                        "%duration%", durationStr,
                        "%is_win%", language.getMessage(winState),
                        "%winner%", winnersStr))
                .build();

        add(icon, slot, e -> GameHistoryGameDetailsGui.open(this, game, target));
    }

    private String resolveModeDisplayName(GameInfo game) {
        Mini mini = resolveMini(game.getMode());
        return mini != null ? mini.getMaster().getDisplayName() + " " + mini.getDisplayName() : (game.getMode() != null ? game.getMode() : translated("game_history_value_none"));
    }

    private String resolveMapDisplayName(GameInfo game) {
        if (game.getMapName() == null || game.getMapName().isEmpty()) {
            return translated("game_history_value_none");
        }

        String translatedMap = language.getMessage(game.getMapName());
        return translatedMap != null && !translatedMap.trim().isEmpty() ? translatedMap : game.getMapName();
    }

    private Mini resolveMini(String modeName) {
        if (modeName == null || modeName.isEmpty()) {
            return null;
        }

        try {
            return Mini.valueOf(modeName);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return Mini.valueOf(modeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String formatDuration(long ms) {
        if (ms <= 0) {
            return translated("game_history_value_none");
        }

        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0) {
            return translated("game_history_duration_minutes_seconds",
                    "%minutes%", minutes,
                    "%seconds%", seconds);
        }

        return translated("game_history_duration_seconds", "%seconds%", seconds);
    }
}
