package club.abelix.bot.discord.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscordSyncBotConfig {

    long guildId;
    long linkedRoleId;
    long linkChannelId;
    long boosterRoleId;
    List<Long> staffChannels;
    List<AdaptedRoleConfig> adaptedRoles;

}