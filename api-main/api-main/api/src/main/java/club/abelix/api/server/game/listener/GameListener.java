package club.abelix.api.server.game.listener;

import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;

import java.util.function.Consumer;

public abstract class GameListener<G extends Game> extends YListener {

    private static final Consumer<Cancellable> CANCEL = event -> event.setCancelled(true);
    protected final G game;

    public GameListener(G game) {
        super(game.getPlugin());

        this.game = game;
    }

    public void checkGame(Game game, Runnable runnable) {
        if (this.game == game) {
            runnable.run();
        }
    }

    public void checkGame(Location location, Runnable runnable) {
        if (location.getWorld() == getWorld()) {
            runnable.run();
        }
    }

    public void checkGame(World world, Runnable runnable) {
        if (world == getWorld()) {
            runnable.run();
        }
    }

    public void checkGame(Gamer gamer, Runnable runnable) {
        checkGame(gamer.getGame(), runnable);
    }

    public void checkGame(Player player, Runnable runnable) {
        Gamer gamer = Gamer.getGamer(player);

        checkGame(gamer, runnable);
    }

    public <E extends AsyncChatFormatEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(AsyncChatFormatEvent event, Runnable runnable) {
        checkGame(event.getSender(), runnable);
    }

    public <E extends GameEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(GameEvent event, Runnable runnable) {
        checkGame(event.getGame(), runnable);
    }

    public <E extends GamerEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(VehicleEvent event, Runnable runnable) {
        checkGame(event.getVehicle().getLocation(), runnable);
    }

    public <E extends VehicleEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(GamerEvent event, Runnable runnable) {
        checkGame(event.getGamer(), runnable);
    }

    public <E extends InventoryEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(InventoryEvent event, Runnable runnable) {
        checkGame(event.getView().getPlayer().getLocation(), runnable);
    }

    public <E extends PlayerEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(PlayerEvent event, Runnable runnable) {
        checkGame(event.getPlayer().getLocation(), runnable);
    }

    public <E extends BlockEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(BlockEvent event, Runnable runnable) {
        checkGame(event.getBlock().getLocation(), runnable);
    }

    public <E extends EntityEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(EntityEvent event, Runnable runnable) {
        checkGame(event.getEntity().getLocation(), runnable);
    }

    public <E extends WorldEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(WorldEvent event, Runnable runnable) {
        checkGame(event.getWorld(), runnable);
    }

    public <E extends WeatherEvent> void checkGame(E event, Consumer<E> success) {
        checkGame(event, () -> success.accept(event));
    }

    public void checkGame(WeatherEvent event, Runnable runnable) {
        checkGame(event.getWorld(), runnable);
    }

    protected World getWorld() {
        return game.getGameWorld();
    }

    @SuppressWarnings("unchecked")
    public <E extends Cancellable> Consumer<E> cancel() {
        return (Consumer<E>) CANCEL;
    }
}
