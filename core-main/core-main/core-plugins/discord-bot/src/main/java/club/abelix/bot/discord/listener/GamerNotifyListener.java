package club.abelix.bot.discord.listener;

import club.abelix.bot.discord.DiscordBot;
import club.abelix.bot.discord.handler.BotSqlHandler;
import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GamerNotifiListener creation on 11.10.2023 at 21:18</p>
 */

@RequiredArgsConstructor
public class GamerNotifyListener implements Listener {

    private final DiscordBot bot;

    @Subscribe
    public void onGamerJoin(@NotNull GamerJoinEvent event) {
        Gamer gamer = event.getGamer();
        CoreApplication.getInstance().getScheduler().schedule(
                () -> {
                    DiscordBotUser discordBotUser = BotSqlHandler.INSTANCE
                            .getByPlayerId(gamer.getPlayerId());

                    if (discordBotUser == null) {
                        gamer.sendLocalizedMessage("discord_join_and_sync");
                    }
                }, 3, TimeUnit.SECONDS);
    }
}
