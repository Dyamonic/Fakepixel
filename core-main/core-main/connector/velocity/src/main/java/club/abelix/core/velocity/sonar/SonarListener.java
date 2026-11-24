package club.abelix.core.velocity.sonar;

import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.event.ServerActiveEvent;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BProxyBotCheck;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import com.velocitypowered.api.event.Subscribe;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.database.model.VerifiedPlayer;
import xyz.jonesdev.sonar.api.event.SonarEvent;
import xyz.jonesdev.sonar.api.event.SonarEventListener;
import xyz.jonesdev.sonar.api.event.impl.UserVerifySuccessEvent;

public final class SonarListener implements SonarEventListener, PlayServerProcessor {

    @Subscribe
    public void onConnect(ServerActiveEvent event) {
        event.processor().registerProcessor(this);
    }

    @Override
    public void handle(@NotNull SonarEvent sonarEvent) {
        if (!Connection.isConnected()) return;

        if (!(sonarEvent instanceof UserVerifySuccessEvent event)) return;

        val name = event.getUser().getUsername();
        val fingerprint = event.getUser().getFingerprint();

        Logging.IMP.info("[Connector] Player {} ({}) passed bot check, sending packet to Core",
                name, fingerprint);

        Connection.sendPacket(new BProxyBotCheck(name, fingerprint));

    }

    @Override
    public void process(BProxyBotCheck packet, ChannelProcessorContext ctx) {
        val name = packet.getPlayerName();
        val fingerprint = packet.getFingerprint();

        try {
            Sonar.get().getVerifiedPlayerController()
                    .add(new VerifiedPlayer(fingerprint, System.currentTimeMillis()));

            Logging.IMP.info("[Connector] Saved user {} ({}) from another velocity", name, fingerprint);
        } catch (Exception exception) {
            Logging.IMP.error("[Connector] Unable to save user {} ({}) from another bungee: {}",
                    name, fingerprint, exception);

            exception.printStackTrace();
        }
    }
}

