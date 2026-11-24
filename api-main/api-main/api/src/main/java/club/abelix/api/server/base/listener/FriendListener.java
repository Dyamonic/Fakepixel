package club.abelix.api.server.base.listener;

import club.abelix.api.base.event.friend.FriendAddEvent;
import club.abelix.api.base.event.friend.FriendRemoveEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SFriendAction;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

public class FriendListener extends YListener implements PlayServerProcessor {

    public FriendListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public void process(SFriendAction packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        int friendId = packet.getFriendId();

        switch (packet.getAction()) {
            case ADD_FRIEND -> {
                gamer.addFriend(friendId);
                HeartBeat.callEvent(new FriendAddEvent(gamer, friendId));
            }
            case REMOVE_FRIEND -> {
                gamer.removeFriend(friendId);
                HeartBeat.callEvent(new FriendRemoveEvent(gamer, friendId));
            }
        }
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }
}
