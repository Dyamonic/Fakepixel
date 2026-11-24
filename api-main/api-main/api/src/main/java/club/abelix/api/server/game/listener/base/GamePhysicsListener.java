package club.abelix.api.server.game.listener.base;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.GameListener;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class GamePhysicsListener extends GameListener<Game> {

    @Getter
    private final World world;

    public GamePhysicsListener(Game game) {
        this(game, game.getGameWorld());
    }

    public GamePhysicsListener(Game game, World world) {
        super(game);

        this.world = world;
        this.world.setGameRuleValue("doDaylightCycle", "false");
    }

//    @EventHandler
//    public void onEntitySpawn(EntitySpawnEvent event) {
//        checkGame(event, cancel());
//    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onFade(BlockFadeEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onLeavesDecayEvent(LeavesDecayEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onPhysic(BlockPhysicsEvent event) {
        checkGame(event, () -> event.setCancelled(true));
    }
}
