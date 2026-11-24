package club.abelix.games.buildffa.game.receivable;

import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class AbstractPhantom implements Receivable {
    private final FakePlayerConfig config;

    public abstract Hologram createHologram(@NotNull Player receiver);

    public abstract FakeEntityPlayer createPlayer(@NotNull Player receiver);

    protected Location hologramLocation() {
        return config.location().clone().add(0, 1.6, 0);
    }

    protected Location fakePlayerLocation() {
        return config.location();
    }

    protected Skin skin() {
        return config.skin();
    }

    @Override
    public final void addReceiver(@NotNull Player player) {
        val hologram = createHologram(player);
        val fakePlayer = createPlayer(player);

        hologram.addReceiver(player);
        fakePlayer.addReceiver(player);
    }
}
