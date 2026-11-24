package club.abelix.api.server.lobby.guild.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.lobby.guild.event.GamerGuildTagUpdate;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CGetGuildInfo;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * @author pro0xy
 * <p>Created on 05.09.2023 at 15:05</p>
 */

public class GuildListener extends YListener {

    public GuildListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamerJoin(GamerJoinEvent event){
        Gamer gamer = event.getGamer();

        if (gamer.getProperty("IN_GUILD") == 0){
            return;
        }

        if (!(gamer.getGuildTag() == null) && !gamer.getGuildTag().isEmpty()){
            gamer.getPlayer().setPlayerListName(gamer.getTabListName() + gamer.getPlayerName() + " " + gamer.getGuildTag());
        }
    }

    @EventHandler
    public void onGamerGuildTagUpdate(GamerGuildTagUpdate event) {
        Gamer gamer = event.getGamer();

        if (gamer.getProperty("IN_GUILD") == 0){
            return;
        }

        if (!(gamer.getGuildTag() == null) && !gamer.getGuildTag().isEmpty()){
            gamer.getPlayer().setPlayerListName(gamer.getTabListName() + gamer.getPlayerName() + " " + gamer.getGuildTag());
        }
    }

}

