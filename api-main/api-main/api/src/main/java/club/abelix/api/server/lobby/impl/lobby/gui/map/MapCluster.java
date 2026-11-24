package club.abelix.api.server.lobby.impl.lobby.gui.map;

import club.abelix.network.common.enums.GameState;
import club.abelix.network.type.mode.Mini;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
class MapCluster {

    // Набор серверов с одной картой
    private final String serverName, map;

    private final Mini mini;

    private final GameState state;

    private final int online, slots, availableGames;


}
