package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class ExchangerPhantom extends AbstractPhantom {
    public ExchangerPhantom(final @NotNull FakePlayerConfig config) {
        super(config);
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val hologram = new Hologram(hologramLocation());

        hologram.addTextLine(Lang.of("exchanger_npc_hologram_title").compile());
        hologram.addTextLine(Lang.of("stats_npc_footer").compile());

        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(fakePlayerLocation(), skin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> ShopUtil.openExchanger(Gamer.getGamer(event.getPlayer())));

        return fakePlayer;
    }
}
