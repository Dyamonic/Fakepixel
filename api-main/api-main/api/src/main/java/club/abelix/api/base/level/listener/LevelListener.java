package club.abelix.api.base.level.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.level.event.LevelUpEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Language;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class LevelListener extends YListener {

    public LevelListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!CoreUtil.isLobby()) {
            return;
        }
        Player player = event.getPlayer();


        if (player == null || !Bukkit.getOnlinePlayers().contains(player)) {
            return;
        }

        Gamer gamer = Gamer.getGamer(player);
        Language language = gamer.getLanguage();

        int level = gamer.getLevel();
        int lastProceedReward = gamer.getProperty(Leveling.LAST_LEVEL_REWARD_PROPERTY);

        if (lastProceedReward == 0) {
            lastProceedReward = 1;

            gamer.setProperty(Leveling.LAST_LEVEL_REWARD_PROPERTY, 1);
        }

        int rewardsDelta = level - lastProceedReward;

        if (rewardsDelta > 0) {
            try {
                gamer.sendRawMessage(language.getMessage("uncollected_level_rewards", "%rewards%", rewardsDelta,
                        "%ending%", language.getEnding(rewardsDelta, "uncollected_level_reward")));
            } catch (Exception ignored) {}
        }

    }

    @EventHandler
    public void onLevelUp(LevelUpEvent event) {
        Gamer gamer = event.getGamer();
        Player player = gamer.getPlayer();

        if (player == null) {
            return;
        }

        int level = gamer.getLevel();

        gamer.sendLocalizedMessage("levelup",
                "%level%", level,
                "%exp%", Leveling.getTotalExpToLevel(level),
                "%for_players%", !gamer.getRanks().hasPermission("levelup.announce")
                        ? gamer.getLanguage().getMessage("levelup_for_players")
                        : ""
        );
    }

}
