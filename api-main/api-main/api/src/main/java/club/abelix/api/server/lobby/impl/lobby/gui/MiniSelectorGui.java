package club.abelix.api.server.lobby.impl.lobby.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.item.ModeIcons;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.impl.lobby.GameLobbyServer;
import club.abelix.api.server.lobby.impl.lobby.gui.map.MapClusterGui;
import club.abelix.api.server.lobby.manager.LobbyManager;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public class MiniSelectorGui extends Gui {

    private static final int[] slots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44};

    private final Set<Mini> subModes;
    private StatusQueryResult result;

    public MiniSelectorGui(Gui parent, @NonNull Gamer gamer) {
        this(parent, gamer, CoreUtil.getMaster().getMinis());
    }

    public MiniSelectorGui(Gui parent, @NonNull Gamer gamer, Set<Mini> subModes) {
        super(gamer.getPlayer(), Lang.of("mini_select_gui_title"), 5);

        this.subModes = subModes.stream()
                .filter(m -> m != Mini.BEDWARS_RANKED)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        this.lookup();
        setParent(parent);

        draw();
    }

    private void lookup() {
        val query = StatusQueries.parametrizedQuery()
                .arenaState(GameState.STAND_BY)
                .modes(subModes.stream().map(Enum::name).toList())
                .kind(ServerKind.ARENA)
                .build();

        val result = StarshipBukkit.getInstance()
                .getStatusQueryExecutionService()
                .executeSync(query);

        if (result.isEmpty()) {
            gamer.sendLocalizedMessage("no_servers_available");
            return;
        }

        this.result = result;
    }

    @Override
    public void draw() {
        drawBorder(slots);
        BaseLobbyServer lobbyServer = LobbyManager.INSTANCE.getLobby();

        boolean useQueues = lobbyServer instanceof GameLobbyServer && ((GameLobbyServer) lobbyServer).isUseQueues();

        if (result.isEmpty()) {
            add(ItemUtil.getBuilder(Material.GLASS_BOTTLE)
                    .setName(translated("mini_selector_gui_empty"))
                    .setLore(translatedLore("mini_selector_gui_empty_lore"))
                    .build(), toSlot(4, 2));
        } else {
            val miniIterator = subModes.iterator();
            int levelY = subModes.size() > 10 ? 1 : 2;

            for (int y = levelY; y < 4; y++) {
                for (int x = 2; x < 7; x++) {
                    val mini = miniIterator.hasNext() ? miniIterator.next() : null;

                    if (mini == null) {
                        return;
                    }

                    val arenaServers = getByMode(mini);

                    int servers = arenaServers.size();
                    long free = arenaServers.stream().filter(arena -> GameState.valueOf(arena.getArenaState().toUpperCase()) == GameState.STAND_BY).count();
                    int online = 0;

                    for (StatusQueryResult.ArenaPerServer server : arenaServers) {
                        online += server.getOnline();
                    }

                    ItemUtil.Builder builder = ItemUtil.getBuilder(ModeIcons.get(mini));
                    builder.setName(translated("mini_selector_gui_entry", "%mode%", mini.getDisplayName()));
                    builder.addItemFlags();

                    if (servers > 0) {
                        builder.setLore(translatedLore("mini_selector_gui_entry_lore",
                                "%servers%", servers, "%free_servers%", free, "%total_online%", online));

                        if (useQueues) {
                            builder.addLore(translatedLore("mini_selector_gui_entry_lore_queue"));
                        } else {
                            builder.addLore(translatedLore("mini_selector_gui_entry_lore_join"));
                        }
                    } else {
                        builder.setLore(translatedLore("mini_selector_gui_entry_lore_unavailable"));
                    }

                    ClickAction action = servers > 0 ? (clickEvent) -> {
                        if (useQueues) {
                            LobbyManager.INSTANCE.<GameLobbyServer>getLobby().joinQueue(gamer, mini);
                            return;
                        }

                        switch (clickEvent.getClick()) {
                            case LEFT -> gamer.connectMode(mini);
                            case RIGHT -> new MapClusterGui(this, gamer, mini);
                        }
                    } : null;

                    add(builder.build(), toSlot(x, y), action);
                }
            }
        }
    }

    private List<StatusQueryResult.ArenaPerServer> getByMode(Mini mini) {
        return result.getPerServer().values().stream().filter((server) -> {
            if (!(server instanceof StatusQueryResult.ArenaPerServer arena)) {
                return false;
            }

            return arena.getMode().equalsIgnoreCase(mini.name());
        }).map(server -> (StatusQueryResult.ArenaPerServer) server).toList();
    }
}
