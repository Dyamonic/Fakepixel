package club.abelix.core.velocity.auth.listener;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.auth.bans.AuthBanManager;
import club.abelix.core.velocity.event.ServerActiveEvent;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.STelegramAuth;
import com.velocitypowered.api.event.Subscribe;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class AuthTwoFactorListener implements PlayServerProcessor {

    private final StarshipVelocity velocityCore;

    @Subscribe
    public void onConnect(final @NonNull ServerActiveEvent event) {
        event.processor().registerProcessor(this);
    }

    @Override
    public void process(final @NonNull STelegramAuth packet, final @NonNull ChannelProcessorContext ctx) {
        val securityUser = packet.getSecurityUser();

        val gamer = Gamer.getGamer(securityUser.getPlayerId());
        val online = gamer != null && gamer.isOnline();

        val address = securityUser.getAddress();

        if (!packet.isConfirmed()) {
            AuthBanManager.addBannedAddress(address.getHostAddress());

            if (online) {
                val language = gamer.getLanguage();
                gamer.getPlayer().disconnect(Component.text(language.getMessage("core_telegram_login_declined_kick")));
            }

            return;
        }

        if (online) {
            gamer.login();
            gamer.sendLocalizedMessage("auth_login_success");

            velocityCore.getAuthManager().afterAuthenticationStage(gamer);
        }
    }
}
