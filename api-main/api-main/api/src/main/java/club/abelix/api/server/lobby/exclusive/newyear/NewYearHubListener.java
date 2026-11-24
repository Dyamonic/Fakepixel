package club.abelix.api.server.lobby.exclusive.newyear;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>NewYearHubListener creation on 08.10.2023 at 01:14</p>
 */

public class NewYearHubListener extends YListener {

    public NewYearHubListener(@NonNull AbstractPlugin plugin) {
        super(plugin);

        new ChristmasSnow(plugin).timer(5,5);
        FalconAPI.getInstance().getCommandManager().addCommand(SnowCommand.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamerJoin(PlayerLoginEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());
        gamer.sendLocalizedMessage("new_year_snow_notification");
    }
}
