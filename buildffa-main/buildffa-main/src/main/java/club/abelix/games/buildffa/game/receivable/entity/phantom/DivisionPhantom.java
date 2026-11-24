package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.gui.standard.DivisionGui;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DivisionPhantom extends AbstractPhantom {
    private final BuildFFAGame game;

    public DivisionPhantom(final @NonNull BuildFFAGame game, final @NonNull FakePlayerConfig config) {
        super(config);
        this.game = game;
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val hologram = new Hologram(hologramLocation());
        val header = Lang.of("stats_npc_header")
                .setAll("%mode%", CoreUtil.getUncommon().getDisplayName())
                .compile();

        hologram.addTextLine(header);

        val gamer = Gamer.getGamer(receiver);
        val rating = gamer.getLifetimeStat("rating");
        val division = AbstractDivision.getRankForRating(rating).getName(Language.defaultLanguage());

        hologram.addTextLine(receiver.getName() + ": " + division + " (" + rating + ")");
        hologram.addTextLine(Lang.of("stats_npc_footer").compile());

        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();
        val fakePlayer = fakeEntityApi.createPlayer(fakePlayerLocation(), Gamer.getGamer(receiver).getSkin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> new DivisionGui(event.getPlayer(), game.getPrestigeManager()));

        return fakePlayer;
    }
}
