package club.abelix.api.server.lobby.impl.lobby.gui.spectate;

import club.abelix.network.type.mode.Mini;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpectateArena {

    private final String server, map;
    private final Mini mini;
    private final int players;
}
