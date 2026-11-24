package club.abelix.bot.discord.user;

import club.abelix.core.app.data.gamer.Gamer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DiscordBotUser {

    private final long discordId;

    private final int playerId;

    /**
     * Получить объект онлайн игрока по нику
     *
     * @return - игрок, если онлайн
     * null, если оффлайн
     */
    public Gamer getPlayer() {
        return Gamer.getOfflineGamer(playerId, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });
    }

}

