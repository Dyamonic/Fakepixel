package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.event.Cancellable;

/**
 * @author bashmak
 * This file has been created on 07.01.2025
 **/
@Getter
public final class GamerGameModeChangeEvent extends GamerEvent implements Cancellable {

    private final GameMode newGameMode;
    private final Gamer target;

    @Setter
    private boolean cancelled;

    public GamerGameModeChangeEvent(final @NonNull Gamer gamer,
                                    final @NonNull GameMode gameMode,
                                    final Gamer target) {
        super(gamer);

        this.newGameMode = gameMode;
        this.target = target;
    }
}
