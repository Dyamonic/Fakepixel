package club.abelix.games.buildffa.game.vote;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.WorldConfig;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author milansky
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class VoteManager {
    Map<Integer, WorldConfig> votes = new HashMap<>();

    public void addVote(final Gamer gamer, final WorldConfig config) {
        votes.put(gamer.getPlayerId(), config);
    }

    public @Nullable WorldConfig getVote(final Gamer gamer) {
        return votes.get(gamer.getPlayerId());
    }

    public int countVotes(final @NotNull WorldConfig config) {
        int i = 0;

        for (val entry : votes.entrySet())
            if (entry.getValue() == config) i++;

        return i;
    }

    public Optional<WorldConfig> getBestMap() {
        val votesMap = new HashMap<WorldConfig, Integer>();

        votes.forEach((id, config) ->
                votesMap.put(config, votesMap.getOrDefault(config, 0) + 1));

        return votesMap.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    public void apply(final @NotNull BuildFFAGame game) {
        getBestMap().or(() -> getRandomWorldConfig(game)).ifPresentOrElse(config -> {
            if (game.getWorldConfig() == config) {
                game.broadcastLocalized("buildffa_vote_apply_same");
                return;
            }

            game.updateWorldConfig(config);
            game.broadcastLocalized("buildffa_vote_applied", "%map%", config.displayName());
        }, () -> game.broadcastLocalized("buildffa_vote_apply_failed"));

        votes.clear();
    }

    private Optional<WorldConfig> getRandomWorldConfig(final @NotNull BuildFFAGame game) {
        return Optional.ofNullable(RandomUtil.getRandomElement(game.getFfaConfig().getGameWorlds().toArray(WorldConfig[]::new)));
    }

    public void instantChange(
            final Gamer gamer,
            final BuildFFAGame game,
            final @NotNull WorldConfig config
    ) {
        game.updateWorldConfig(config);

        HeartBeat.runLaterSync(() -> {
            game.broadcastLocalized("buildffa_instant_change_broadcast",
                    "%gamer%", gamer.getDisplayName(), "%map%", config.displayName());
            game.broadcastLocalizedTitle(
                    "buildffa_instant_change_title", "buildffa_instant_change_subtitle",
                    "%gamer%", gamer.getDisplayName(),
                    "%map%", config.displayName()
            );
        }, 20);
    }
}
