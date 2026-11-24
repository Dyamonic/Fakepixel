package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.event.skin.SkinChangeEvent;
import club.abelix.api.base.event.skin.SkinChangedEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.commons.data.skin.Skin;
import club.abelix.core.bukkit.HeartBeat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class SkinListener extends YListener implements PluginMessageListener {

    private final FakeEntityApi entityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    public SkinListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onSkinChange(SkinChangedEvent event) {
        Player player = event.getPlayer();

        for (FakeEntity entity : entityApi.getTrackedEntities()) {
            if ((entity.isPublicStatus() || entity.hasReceiver(player))
                    && !entity.hasViewer(player)) {

                entity.addViewer(player);
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

        try {
            String skinName = in.readUTF();
            String value = in.readUTF();
            String signature = in.readUTF();

            boolean force = in.readBoolean();

            HeartBeat.callEvent(new SkinChangeEvent(player, value, signature), result -> {
                if (!force && result.isCancelled()) {
                    return;
                }

                Gamer gamer = Gamer.getGamer(player);
                gamer.setSkin(new Skin(skinName.toLowerCase(), "", value, signature));

                FalconAPI.getNmsApi().getPlayerApi().changePlayerSkin(player, value, signature);

                HeartBeat.callEvent(new SkinChangedEvent(player, value, signature));
            });
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
