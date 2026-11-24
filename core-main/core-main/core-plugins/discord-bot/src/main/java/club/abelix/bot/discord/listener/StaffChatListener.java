package club.abelix.bot.discord.listener;

import club.abelix.bot.discord.DiscordBot;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.StaffMessageEvent;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>StaffChatListener creation on 11.01.2023 at 15:44</p>
 */

@RequiredArgsConstructor
public class StaffChatListener implements Listener {
    private final DiscordBot bot;

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onStaffChatMessage(StaffMessageEvent event) {
        for (TextChannel staffInfoChannel : bot.getStaffInfoChannels()) {
            staffInfoChannel.sendMessage(ChatColor.stripColor(event.getContext().format(Language.defaultLanguage()))).queue();
        }
    }
}
