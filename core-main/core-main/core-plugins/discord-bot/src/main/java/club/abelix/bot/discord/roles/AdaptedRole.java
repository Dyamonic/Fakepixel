package club.abelix.bot.discord.roles;

import club.abelix.commons.data.rank.Rank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdaptedRole {

    Rank ranks;
    long discordRoleId;

    public AdaptedRole(long discordRoleId, Rank ranks) {
        this.discordRoleId = discordRoleId;
        this.ranks = ranks;
    }

}
