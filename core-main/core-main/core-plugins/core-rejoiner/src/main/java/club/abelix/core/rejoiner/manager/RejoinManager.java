package club.abelix.core.rejoiner.manager;

import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.type.mode.Master;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class RejoinManager {

    private static final List<Master> REJOINABLE_GAMEMODES = new ArrayList<>() {{
        add(Master.BW);
        //add(Master.EW);
    }};

    /**
     * Сервера, на которые игрок может перезайти
     * <p>
     * Если он зайдет на другой сервер бв и там начнется игра - сервер сбросится
     * Если на его сервере закончится игра - сервер тоже сбросится
     * Перезайти можно в любой момент пока на сервере идет игра
     */
    private final TIntObjectMap<String> rejoinServerMap = new TIntObjectHashMap<>();

    /**
     * Получить сервер, на который игрок может перезайти на данный момент
     *
     * @param player - игрок, чей сервер получаем
     * @return - сервер игрока, на который можно перезайти
     */
    public AbstractServer getRejoinableServer(@NonNull Gamer player) {
        String serverName = rejoinServerMap.get(player.getPlayerId());

        if (serverName == null) {
            return null;
        }

        AbstractServer server = ServerManager.IMP.getServer(serverName);

        if (server == null) {
            rejoinServerMap.remove(player.getPlayerId());
        }

        return server;
    }

    /**
     * Изменить сервер, на который игрок может реджоинуться
     *
     * @param player - игрок, которому ставим
     * @param server - сервер, на который игрок сможет перезайти
     */
    public void setRejoinableServer(@NonNull Gamer player, @NonNull AbstractServer server) {
        rejoinServerMap.put(player.getPlayerId(), server.getName());
    }

    /**
     * Удалить игроков, которые могут перезайти на указанный сервер
     *
     * @param server - сервер, на который можно перезайти
     */
    public void clearRejoinableGamers(@NonNull AbstractServer server) {
        TIntList playersToRemove = new TIntArrayList();

        //сначала получаем всех у кого этот сервер
        for (int playerId : rejoinServerMap.keys()) {
            if (rejoinServerMap.get(playerId).equalsIgnoreCase(server.getName())) {
                playersToRemove.add(playerId);
            }
        }

        //а теперь удаляем их
        playersToRemove.forEach(id -> {
            rejoinServerMap.remove(id);
            return true;
        });
    }

    /**
     * Узнать, можно ли перезаходить на указанный режим
     *
     * @param master - предполагаемый режим
     * @return - можно ли перезайти
     */
    public boolean canRejoin(@NonNull Master master) {
        return REJOINABLE_GAMEMODES.contains(master);
    }

}
