package club.abelix.api.base.playertop.storage;

import club.abelix.api.base.playertop.type.PlayerTop;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerTopStorage {

    public static final PlayerTopStorage IMP = new PlayerTopStorage();

    // -----------------------------------------------------------------

    private final List<PlayerTop<?>> playerTops = new ArrayList<>();

    public List<PlayerTop<?>> getRegisteredTops() {
        return Collections.unmodifiableList(playerTops);
    }

    public void registerTop(@NonNull PlayerTop<?> playerTop) {
        playerTops.add(playerTop);
    }

}
