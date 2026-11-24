package club.abelix.core.velocity.listener.player.feature;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LinksFeature {

    StarshipVelocity starship;


    @Subscribe(priority = Short.MIN_VALUE)
    public void onPostLogin(PostLoginEvent e) {
        val gamer = Gamer.getGamer(e.getPlayer());

        if (gamer.getPlayer().getProtocolVersion().getProtocol()
                >= ProtocolVersion.MINECRAFT_1_21.getProtocol()) {
            gamer.getPlayer().setServerLinks(starship.getServerLinks());
            Logging.IMP.info("[Feature] Send links to {}", gamer.getName());
        }

    }
}
