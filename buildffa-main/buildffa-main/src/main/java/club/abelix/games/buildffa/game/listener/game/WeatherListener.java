package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class WeatherListener extends YListener {
    public WeatherListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());
    }

    @EventHandler
    public void onWeatherChange(@NotNull final WeatherChangeEvent event) {
        event.setCancelled(true);
    }
}
