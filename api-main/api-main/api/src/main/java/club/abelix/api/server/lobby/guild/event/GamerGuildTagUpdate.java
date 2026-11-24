package club.abelix.api.server.lobby.guild.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import club.abelix.core.bukkit.event.YEvent;

/**
 * @author pro0xy
 * <p>Created on 06.09.2023 at 16:43</p>
 */

public class GamerGuildTagUpdate extends GamerEvent {
    public GamerGuildTagUpdate(Gamer gamer) {
        super(gamer);
    }
}
