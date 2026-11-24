package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

@Getter
public class GameKillEvent extends GameEvent {

    // Вызывается, когда игрок помер, и трекер определил для него убийцу

    private final Gamer gamer;
    private final Gamer killerGamer;

    private final boolean playerLeft;

    private final EntityDamageEvent.DamageCause deathCause;
    private final Location killLocation;

    public GameKillEvent(@NonNull Game game, @NonNull Gamer gamer, Gamer killer,
                         @NonNull EntityDamageEvent.DamageCause cause) {

        this(game, gamer, killer, cause, false, gamer.getPlayer().getLocation());
    }

    public GameKillEvent(@NonNull Game game, @NonNull Gamer gamer, Gamer killer,
                         @NonNull EntityDamageEvent.DamageCause cause, boolean playerLeft, Location killLocation) {

        super(game);

        this.gamer = gamer;
        this.killerGamer = killer;
        this.playerLeft = playerLeft;
        this.deathCause = cause;
        this.killLocation = killLocation;
    }

    public Player getPlayer() {
        return gamer.getPlayer();
    }

    public Player getKiller() {
        return killerGamer != null ? killerGamer.getPlayer() : null;
    }

}
