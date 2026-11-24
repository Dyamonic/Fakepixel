package club.abelix.api.base.box.reward.standard;

import club.abelix.api.base.box.reward.AbstractBoxReward;
import club.abelix.api.base.discord.DiscordSyncQueueSqlHandler;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.RankIcons;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class RankBoxReward extends AbstractBoxReward {

    private final Rank rank;
    private final long durationSeconds;
    private final int equivalent;

    public RankBoxReward(
            @NonNull Rarity rarity,
            @NonNull Rank rank,
            long durationSeconds,
            int equivalent,
            int chance
    ) {
        super(rarity, chance);

        this.rank = rank;
        this.durationSeconds = durationSeconds;
        this.equivalent = equivalent;
    }

    public RankBoxReward(
            Rarity rarity,
            Rank rank,
            int equivalent,
            int chance
    ) {
        super(
                rarity,
                chance
        );

        this.rank = rank;
        this.durationSeconds = 0;
        this.equivalent = equivalent;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        val durationString = durationSeconds == 0 ?
                language.getMessage("forever") :
                Duration.durationWordsSec(language, durationSeconds);

        return language.getMessage("reward_group_name",
                "%group%", rank.getDisplayName(language),
                "%duration%", durationString
        );
    }

    @Override
    public @UnmodifiableView @NotNull List<@NotNull String> getDescription(@NotNull Language language) {
        return List.of();
    }

    @Override
    public boolean apply(@NotNull Gamer gamer) {
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

                    gamer.loadRanks();
                    Connection.sendPacket(new BUserRankRefreshPacket(gamer.getPlayerId()));
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
    public @NotNull ItemStack getRewardIcon(@NonNull Language language) {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }

    @Override
    public @NotNull EquivalentType getEquivalentType() {
        return new GoldType();
    }

    @Override
    public int getEquivalent(@NotNull Gamer gamer) {
        return equivalent;
    }

}
