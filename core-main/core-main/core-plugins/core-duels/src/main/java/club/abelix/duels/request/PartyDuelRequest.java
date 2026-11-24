package club.abelix.duels.request;

import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.request.RequestException;
import club.abelix.core.app.api.request.CoreRequest;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.duels.mode.DuelMode;
import lombok.val;

/**
 * @author artem
 */
public class PartyDuelRequest extends CoreRequest {

    private final DuelMode mode;

    public PartyDuelRequest(Gamer sender, Gamer receiver, DuelMode mode) {
        super(DuelRequestType.INSTANCE, sender, receiver);

        this.mode = mode;
    }

    @Override
    protected ObjectLinkedBuf getSendTranslations() {
        return new ObjectLinkedBuf("%mode%", mode.mini().getDisplayName());
    }

    @Override
    public void accept() throws RequestException {
        Party senderParty = sender.getParty();
        Party receiverParty = receiver.getParty();

        int requiredPlayers = mode.playersPerTeam();

        if (senderParty == null || receiverParty == null
                || senderParty.getSize() != requiredPlayers || receiverParty.getSize() != requiredPlayers) {

            sender.sendLocalizedMessage("duel_command_party_request_no_party", "%players%", requiredPlayers);
            receiver.sendLocalizedMessage("duel_command_party_request_no_party", "%players%", requiredPlayers);

            return;
        }

        int space = requiredPlayers * 2;
        val server = ServerManager.IMP.getBestBySpace(mode.mini(), requiredPlayers * 2);

        if (server != null) {
            server.newReservation(space);

            senderParty.getLeader().sendTo(false, server);
            receiverParty.getLeader().sendTo(false, server);
        } else {
            sender.sendLocalizedMessage("core_no_servers_available_by_mode");
            receiver.sendLocalizedMessage("core_no_servers_available_by_mode");
        }
    }

}
