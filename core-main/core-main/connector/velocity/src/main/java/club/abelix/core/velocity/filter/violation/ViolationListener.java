package club.abelix.core.velocity.filter.violation;

import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.filter.ChatFilterManager;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.network.packet.play.client.CMutePlayer;
import com.velocitypowered.api.event.Subscribe;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ViolationListener {

    private final ChatFilterManager chatFilterManager;

    @Subscribe
    public void onChatViolation(ChatViolationEvent e) {
        ViolationType violationType = e.type();

        if (violationType == ViolationType.ADVERTISING || violationType == ViolationType.SWEARING) {
            return;
        }

        Gamer gamer = Gamer.getGamerOptional(e.player()).orElseThrow();

        double vl = e.data().getTotalViolations();

        int violationThreshold = chatFilterManager.getViolationThreshold();

        if (vl > violationThreshold) {
            // проверяем на мут что бы не слать лишнюю хуйню на кор
            if (chatFilterManager.getStarship().getPunishmentsManager().getActiveMute(gamer.getPlayerId()) != null) {
                return;
            }

            if (gamer.hasPermission("chatfilter.punishment.bypass")) {
                return;
            }

            Connection.sendPacket(new CMutePlayer(gamer.getPlayerId(), "Console",
                    "filter_chat_detection", TimeUnit.MINUTES.toMillis(30)));

            return;
        }

        switch (e.type()) {
            case FLOOD, FAST_PACKETS, FAST_CHAT -> gamer.sendLocalizedMessage("filter_" + e.type().name() + "_messages",
                    "%vl_level%", vl,
                    "%vl_threshold%", violationThreshold);
        }
    }

}
