package club.abelix.api.base.playertop.builder;

import club.abelix.api.base.playertop.storage.PlayerTopStorage;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.core.bukkit.HeartBeat;

public abstract class PlayerTopBuilder<T> {

    protected int maxPlayers = 10;
    protected int updateInterval = HeartBeat.TEN_MINUTES;

    public static SingleTopBuilder defaultTopBuilder() {
        return new SingleTopBuilder();
    }

    public static PropertyTopBuilder propertyTopBuilder() {
        return new PropertyTopBuilder();
    }

    public static StatisticTopBuilder statisticTopBuilder() {
        return new StatisticTopBuilder();
    }

    public T maxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return (T) this;
    }

    public T updateInterval(int interval) {
        this.updateInterval = interval;
        return (T) this;
    }

    public final PlayerTop<?> create() {
        PlayerTop<?> playerTop = generate();

        if (updateInterval > 0) {
            playerTop.startUpdating(updateInterval);
        }

        PlayerTopStorage.IMP.registerTop(playerTop);

        return playerTop;
    }

    protected abstract PlayerTop<?> generate();
}
