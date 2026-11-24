package club.abelix.games.buildffa.game.receivable;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public interface Receivable {

    void addReceiver(@NotNull Player player);

}
