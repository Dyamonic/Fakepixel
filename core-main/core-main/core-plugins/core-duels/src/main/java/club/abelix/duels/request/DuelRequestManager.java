package club.abelix.duels.request;

import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.util.request.RequestManager;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.duels.mode.DuelMode;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>DuelRequestManager creation on 17.01.2024 at 13:16</p>
 */

@UtilityClass
public class DuelRequestManager {

    public void send(@NonNull Gamer sender, @NonNull Gamer receiver, @NonNull DuelMode duelMode) {
        int requiredPlayers = duelMode.playersPerTeam();

        if (receiver.isSettingSet(Setting.CAN_INVITE_DUEL)) {
            sender.sendLocalizedMessage("duel_receiver_disabled_invite");
            return;
        }

        AbstractServer server = ServerManager.IMP.getBestBySpace(duelMode.mini(), requiredPlayers * 2);

        if (server == null) {
            sender.sendLocalizedMessage("core_no_servers_available_by_mode");
            return;
        }


        RequestManager.IMP.sendRequest(new PrivateDuelRequest(sender, receiver, duelMode));
    }

}
