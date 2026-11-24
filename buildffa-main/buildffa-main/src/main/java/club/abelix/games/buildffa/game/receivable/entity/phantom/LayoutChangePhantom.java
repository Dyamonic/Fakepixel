package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.gui.standard.LayoutChangeGui;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LayoutChangePhantom extends AbstractPhantom {

    public LayoutChangePhantom(final @NonNull FakePlayerConfig config) {
        super(config);
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val hologram = new Hologram(hologramLocation());

        hologram.addTextLine(Lang.of("hologram_layout_change_info").compile());
        hologram.addTextLine(Lang.of("stats_npc_footer").compile());

        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();
        val fakePlayer = fakeEntityApi.createPlayer(fakePlayerLocation(), skin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> new LayoutChangeGui(event.getPlayer()));

        return fakePlayer;
    }
}
