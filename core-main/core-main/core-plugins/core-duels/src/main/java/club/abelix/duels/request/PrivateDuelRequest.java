package club.abelix.duels.request;

import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.core.app.api.request.CoreRequest;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.duels.mode.DuelMode;
import lombok.val;

/**
 * @author artem
 */
public class PrivateDuelRequest extends CoreRequest {

    private final DuelMode mode;

    public PrivateDuelRequest(Gamer sender, Gamer receiver, DuelMode mode) {
        super(DuelRequestType.INSTANCE, sender, receiver);

        this.mode = mode;
    }

    @Override
    protected ObjectLinkedBuf getSendTranslations() {
        return new ObjectLinkedBuf("%mode%", mode.mini().getDisplayName());
    }

    @Override
    public void accept() {
//        if (sender.getParty() != null || receiver.getParty() != null) {
//            sender.sendLocalizedMessage("duel_command_solo_party");
//            receiver.sendLocalizedMessage("duel_command_solo_party");
//            return;
//        }

        val server = ServerManager.IMP.getBestBySpace(mode.mini(), 2);

        if (server != null) {
            server.newReservation(2);

            sender.sendTo(false, server);
            receiver.sendTo(false, server);

            server.disableStatistic(sender, receiver);
        } else {
            sender.sendLocalizedMessage("core_no_servers_available_by_mode");
            receiver.sendLocalizedMessage("core_no_servers_available_by_mode");
        }
    }

}
