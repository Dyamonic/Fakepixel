package club.abelix.bot.discord.listener;

import club.abelix.bot.discord.DiscordBot;
import club.abelix.bot.discord.handler.BotSqlHandler;
import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.PlayerRankAddEvent;
import club.abelix.core.app.event.gamer.PlayerRankRemoveEvent;
import club.abelix.core.app.event.gamer.RankChangedEvent;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@RequiredArgsConstructor
public class GameBotListener implements Listener {

    private final DiscordBot bot;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerAddRank(PlayerRankAddEvent event) {
        onPlayerRankChange(event);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPlayerRemoveRank(PlayerRankRemoveEvent event) {
        onPlayerRankChange(event);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onGamerJoin(GamerJoinEvent event) {
        bot.reconcileBoosterOnJoin(event.getGamer());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    private void onPlayerRankChange(RankChangedEvent event) {
        Gamer gamer = event.getGamer();

        DiscordBotUser botUser = BotSqlHandler.INSTANCE.getByPlayerId(gamer.getPlayerId());

        // синхронизация роли в дискорде, если аккаунт привязан
        if (botUser != null) {
            bot.discordUserSync(botUser.getDiscordId(), gamer);
        }
    }

}
