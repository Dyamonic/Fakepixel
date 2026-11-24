package club.abelix.core.app.event.arena;

import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.network.common.enums.GameState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ArenaChangeStateEvent extends Event {

    private final ArenaServer server;

    private final GameState oldState, newState;

}
