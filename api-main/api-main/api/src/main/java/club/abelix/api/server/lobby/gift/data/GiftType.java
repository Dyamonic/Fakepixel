package club.abelix.api.server.lobby.gift.data;

import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.reward.impl.ExperienceReward;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum GiftType {

    VIP(399, 2, new ExperienceReward(5_000)),
    VIP_PLUS(699, 3, new ExperienceReward(10_000)),
    MVP(1399, 4, new ExperienceReward(25_000)),
    MVP_PLUS(2499, 5, new ExperienceReward(50_000)),
    SPONSOR_1(4999, 13, 365, new ExperienceReward(75_000)),
    SPONSOR_2(6999, 14, 365, new ExperienceReward(75_000)),
    SPONSOR_3(9999, 15, 365, new ExperienceReward(75_000)),
    SPECIAL_365(23999, 16, 365, new ExperienceReward(100_000)),
    SPECIAL(44999, 16, new ExperienceReward(100_000))
    ;

    int price;
    int rankId;
    long time;
    Reward reward;

    GiftType(int price, int rankId, @NonNull Reward reward) {
        this.price = price;
        this.rankId = rankId;
        this.time = 0;
        this.reward = reward;
    }

}