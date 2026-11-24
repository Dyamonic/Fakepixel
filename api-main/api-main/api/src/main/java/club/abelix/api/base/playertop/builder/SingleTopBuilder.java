package club.abelix.api.base.playertop.builder;

import club.abelix.api.base.playertop.provider.PlayerTopProvider;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.playertop.type.single.SinglePlayerTop;
import lombok.NonNull;
import org.bukkit.Location;

public class SingleTopBuilder extends PlayerTopBuilder<SingleTopBuilder> {

    private String localizationKey;

    private Location location;

    private PlayerTopProvider provider;

    public SingleTopBuilder localizationKey(@NonNull String localizationKey) {
        this.localizationKey = localizationKey;
        return this;
    }

    public SingleTopBuilder location(@NonNull Location location) {
        this.location = location;
        return this;
    }

    public SingleTopBuilder provider(@NonNull PlayerTopProvider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    protected PlayerTop<?> generate() {
        SinglePlayerTop playerTop
                = new SinglePlayerTop(location, localizationKey, maxPlayers);

        playerTop.register(provider);

        return playerTop;
    }
}
