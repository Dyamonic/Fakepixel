package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.network.type.mode.Uncommon;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyManager extends YListener {
    private static final int MIN_STREAK = 10;
    private static final int COINS_PER_STREAK = 10;

    private final BuildFFAGame game;
    private final Map<UUID, Integer> bounties = new HashMap<>();

    public BountyManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    public void updateBounty(Player player, int streak) {
        if (streak < MIN_STREAK) return;

        int bounty = streak * COINS_PER_STREAK;
        int old = bounties.getOrDefault(player.getUniqueId(), 0);
        if (bounty <= old) return;

        bounties.put(player.getUniqueId(), bounty);

        Gamer gamer = Gamer.getGamer(player);
        broadcast("&cA bounty of &e" + bounty + " coins &cwas placed on "
            + gamer.getDisplayName() + "&c!");
    }

    public int claimBounty(Player killer, Player victim) {
        Integer bounty = bounties.remove(victim.getUniqueId());
        if (bounty == null || bounty <= 0) return 0;

        Gamer killerGamer = Gamer.getGamer(killer);
        Gamer victimGamer = Gamer.getGamer(victim);

        int reward = bounty;
        killerGamer.addCoins(Uncommon.BUILDFFA, reward);
        game.getQuestManager().handleCoinsEarned(killer, reward);
        game.getQuestManager().handleBountyClaim(killer);

        broadcast(killerGamer.getDisplayName()
            + " &7claimed &e" + reward
            + " coins &7from "
            + victimGamer.getDisplayName()
            + "&7's bounty.");

        return reward;
    }

    public void clear(Player player) {
        bounties.remove(player.getUniqueId());
    }

    private void broadcast(String message) {
        for (Player player : game.getPlugin().getServer().getOnlinePlayers()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
