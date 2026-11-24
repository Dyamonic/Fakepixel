package club.abelix.api.server.lobby.gift;

import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.reward.impl.PlusColorReward;
import club.abelix.api.base.reward.impl.SuffixReward;
import club.abelix.api.server.lobby.gift.data.Gift;
import club.abelix.api.server.lobby.gift.data.GiftType;
import club.abelix.commons.data.permission.prefix.defaults.RankPrefixModifier;
import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GiftBonuses {

    private static final List<Bonus> BONUSES = List.of(
            new Bonus(5, new SuffixReward(19)),
            new Bonus(10, new SuffixReward(20)),
            new Bonus(15, new SuffixReward(21)),
            new Bonus(100, new PlusColorReward(RankPrefixModifier.DARK_AQUA)),

            new Bonus(5, GiftType.MVP_PLUS.getRankId(), new SuffixReward(22)),
            new Bonus(3, GiftType.SPONSOR_1.getRankId(), new SuffixReward(23))
    );

    public static List<Reward> resolve(@NonNull Collection<Gift> gifts) {
        int totalCount = gifts.size();

        Map<Integer, Long> byRank = gifts.stream()
                .collect(Collectors.groupingBy(
                        g -> g.type().getRankId(),
                        Collectors.counting()
                ));

        List<Reward> result = new ArrayList<>();

        for (Bonus bonus : BONUSES) {
            if (bonus.rankId() == 0) {
                if (totalCount == bonus.exactAmount()) {
                    result.add(bonus.reward());
                }
            } else {
                val rankCount = byRank.getOrDefault(bonus.rankId(), 0L);

                if (rankCount == bonus.exactAmount()) {
                    result.add(bonus.reward());
                }
            }
        }

        return result;
    }

    public static List<Bonus> getBonuses() {
        return BONUSES;
    }

    public record Bonus(int exactAmount, int rankId, Reward reward) {

        public Bonus(int exactAmount, Reward reward) {
            this(exactAmount, 0, reward);
        }
    }
}
