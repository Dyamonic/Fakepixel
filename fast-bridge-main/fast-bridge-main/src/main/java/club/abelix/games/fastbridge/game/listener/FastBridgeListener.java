package club.abelix.games.fastbridge.game.listener;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class FastBridgeListener extends YListener {

    @Getter
    private final FastBridgeGame game;

    public FastBridgeListener(FastBridgeGame game) {
        super(game.getPlugin());

        this.game = game;
    }

    /**
     * @apiNote <p>проверяет валидность игрока и его сессии
     */
    public void checkSession(Player player, Consumer<FastBridgeGamer> success) {
        val gamer = FastBridgeGamer.get(player);

        if (gamer == null) {
            player.kickPlayer("An error occurred! Please try again later.");
            return;
        };

        if (gamer.getIsland() == null) return;
        if (!gamer.getIsland().getRawConfig().worldName().equals(player.getWorld().getName())) return;
        if (gamer.getIsland().getOccupiedBy() == null) return;
        if (!gamer.getIsland().getOccupiedBy().equals(gamer)) return;
//        if (gamer.getGamer().getPlayer().getGameMode().equals(GameMode.SPECTATOR)) return;

        success.accept(gamer);
    }

    public <E extends PlayerEvent> void checkSession(E event, BiConsumer<E, FastBridgeGamer> success) {
        checkSession(event.getPlayer(), gamer -> success.accept(event, gamer));
    }

    public <E extends BlockPlaceEvent> void checkSession(E event, BiConsumer<E, FastBridgeGamer> success) {
        checkSession(event.getPlayer(), gamer -> success.accept(event, gamer));
    }

    public <E extends BlockBreakEvent> void checkSession(E event, BiConsumer<E, FastBridgeGamer> success) {
        checkSession(event.getPlayer(), gamer -> success.accept(event, gamer));
    }

    public <E extends InventoryInteractEvent> void checkSession(E event, BiConsumer<E, FastBridgeGamer> success) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        checkSession(player, gamer -> success.accept(event, gamer));
    }

    public <E extends EntityEvent> void checkSession(E event, BiConsumer<E, FastBridgeGamer> success) {
        if (event.getEntity() instanceof Player player) {
            checkSession(player, gamer -> success.accept(event, gamer));
        }
    }

}
