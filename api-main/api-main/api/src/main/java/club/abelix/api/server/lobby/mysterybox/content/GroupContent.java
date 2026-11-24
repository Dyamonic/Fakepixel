package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.impl.GroupReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.util.PermissionUtil;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class GroupContent extends GroupReward implements CaseContent {

    private final Rank rank;
    private final Rarity rarity;

    public GroupContent(@NonNull Rarity rarity, @NonNull Rank rank) {
        super(rank, 0);

        this.rank = rank;
        this.rarity = rarity;
    }

    public GroupContent(@NonNull Rarity rarity, @NonNull Rank rank, long durationSeconds) {
        super(rank, durationSeconds);

        this.rank = rank;
        this.rarity = rarity;
    }

    @Override
    public EquivalentType getEquivalentType() {
        return GoldType.INSTANCE;
    }

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        return PermissionUtil.extractLimit(rank, "duplicate.equivalent");
    }

}

