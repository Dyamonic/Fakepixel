package club.abelix.api.base.reward.impl;

import club.abelix.api.base.discord.DiscordSyncQueueSqlHandler;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.util.item.RankIcons;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.bukkit.StarshipBukkit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class GroupReward implements Reward {

    protected final Rank rank;
    protected final long durationSeconds;

    @Override
    public boolean give(@NotNull Gamer gamer) {
        if (gamer.getRanks().hasRank(rank.getId())) {
            return false;
        }

        LocalDateTime expires = null;

        if (durationSeconds > 0) {
            expires = LocalDateTime.now().plusSeconds(durationSeconds);
        }

        StarshipBukkit.getInstance()
                .getUserRanksService()
                .addUserRank(gamer.getPlayerId(), rank, expires)
                .whenComplete((r, t) -> {
                    if (t != null) {
                        gamer.sendLocalizedMessage("error_rank_add_ask_support");
                        log.error("Failed to add rank {} to gamer {}: {}", rank.getId(), gamer.getName(), t.getMessage(), t);
                        return;
                    }

                    DiscordSyncQueueSqlHandler.INSTANCE
                            .enqueuePlayerSync(gamer.getPlayerId(), "rank_change")
                            .exceptionally(queueError -> {
                                log.error("Failed to enqueue Discord sync for gamer {} after adding rank {}: {}",
                                        gamer.getName(), rank.getId(), queueError.getMessage(), queueError);
                                return null;
                            });
                });

        return true;
    }

    @Override
    public String getRewardName(@NotNull Language language) {
        String durationString = durationSeconds == 0 ?
                language.getMessage("forever") :
                Duration.durationWordsSec(language, durationSeconds);

        return language.getMessage("reward_group_name",
                "%group%", rank.getDisplayName(language),
                "%duration%", durationString
        );
    }

    @Override
    public ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }

}
