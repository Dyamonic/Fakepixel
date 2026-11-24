package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.level.LevelingUtil;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import club.abelix.network.type.mode.Uncommon;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class StatisticPhantom extends AbstractPhantom {
    public StatisticPhantom(final @NotNull FakePlayerConfig config) {
        super(config);
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val gamer = Gamer.getGamer(receiver);
        val hologram = new Hologram(hologramLocation());

        hologram.addTextLine(gamer.getLanguage().getMessage("stats_npc_line_kills",
                "%value%", gamer.getLifetimeStat("kills")));
        hologram.addTextLine(gamer.getLanguage().getMessage("stats_npc_line_deaths",
                "%value%", gamer.getLifetimeStat("deaths")));
        hologram.addTextLine(gamer.getLanguage().getMessage("stat_npc_mode_level",
                "%level%", LevelingUtil.getFormattedLevel(gamer, false)));

        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val gamer = Gamer.getGamer(receiver);
        val fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(fakePlayerLocation(), skin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> {
            gamer.sendMessage("FIXME: Add method to open detailed gui with uncommon");
        });

        return fakePlayer;
    }
}
