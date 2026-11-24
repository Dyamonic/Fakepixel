package club.abelix.api.server.lobby.guild;

import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.guild.listener.GuildListener;
import lombok.NonNull;

/**
 * @author pro0xy
 * <p>Created on 05.09.2023 at 15:37</p>
 */

public class GuildManager {
    public static final GuildManager INSTANCE = new GuildManager();

    public void register(@NonNull BaseLobbyServer server){
        new GuildListener(server.getPlugin());
    }
}
