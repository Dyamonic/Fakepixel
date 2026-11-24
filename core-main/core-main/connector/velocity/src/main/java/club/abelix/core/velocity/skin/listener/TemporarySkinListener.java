package club.abelix.core.velocity.skin.listener;

import club.abelix.commons.data.skin.Skin;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.skin.SkinsManager;
import club.abelix.network.common.logging.Logging;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TemporarySkinListener {

    StarshipVelocity starship;

    Map<Player, Skin> oldProfiles = new ConcurrentHashMap<>();

    @Subscribe
    public void onPluginMessage(final PluginMessageEvent event) {
        if (event.getSource() instanceof ProxyServer && event.getIdentifier() != SkinsManager.tempSkinChannel) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());

            Player player = (Player) event.getSource();

            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
                String value = in.readUTF();
                String signature = in.readUTF();

                if (!this.oldProfiles.containsKey(player)) {
                    this.oldProfiles.put(player, SkinsManager.getCurrentSkin(player));
                    Logging.IMP.info("[TemporarySkinListener] Remembered old profile");
                }

                Skin skin = new Skin("", "", value, signature);

                SkinsManager.setTexturesProperty(player, skin);
                starship.getSkinsManager().updateSkinOnBukkit(player, skin, true);

                Logging.IMP.info("[TemporarySkins] Set temp skin of " + player.getUsername());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void resetProfile(@NonNull Player player) {
        Skin oldSkin = this.oldProfiles.remove(player);

        if (oldSkin != null) {
            SkinsManager.setTexturesProperty(player, oldSkin);
            starship.getSkinsManager().updateSkinOnBukkit(player, oldSkin, true);
            this.oldProfiles.remove(player);

            Logging.IMP.info("[TemporarySkins] Reset skin of " + player.getUsername());
        }
    }

    @Subscribe
    public void onServerConnected(final ServerConnectedEvent event) {
        resetProfile(event.getPlayer());
    }

    @Subscribe
    public void onPlayerDisconnect(final DisconnectEvent event) {
        this.oldProfiles.remove(event.getPlayer());
    }

}
