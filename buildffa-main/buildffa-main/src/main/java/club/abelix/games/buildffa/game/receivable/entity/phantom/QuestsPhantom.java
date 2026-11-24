package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class QuestsPhantom extends AbstractPhantom {
    private final BuildFFAGame game;

    public QuestsPhantom(final @NonNull BuildFFAGame game, final @NotNull FakePlayerConfig config) {
        super(config);
        this.game = game;
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val hologram = new Hologram(hologramLocation());
        hologram.addTextLine("§aQuests");
        hologram.addTextLine("§e§lCLICK FOR QUESTS");
        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(fakePlayerLocation(), skin());
        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> game.getQuestManager().open(event.getPlayer()));
        return fakePlayer;
    }
}
