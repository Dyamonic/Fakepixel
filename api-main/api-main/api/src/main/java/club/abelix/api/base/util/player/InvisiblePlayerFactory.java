package club.abelix.api.base.util.player;

import club.abelix.api.AbstractPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class InvisiblePlayerFactory {

    /**
     * Список в котором будут игроки которых мы не собираемся
     * прятать в табе, но собираемся скрыть от других
     */
    private final List<String> ignoredPlayerList = new ArrayList<>();

    public void initializePacketListener(@NonNull AbstractPlugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                List<PlayerInfoData> playerInfoList = event.getPacket().getPlayerInfoDataLists().read(0);

                if (playerInfoList.isEmpty()) {
                    return;
                }

                PlayerInfoData playerInfoData = playerInfoList.get(0);

                if (ignoredPlayerList.contains(playerInfoData.getProfile().getName())) {
                    event.setCancelled(true);
                }
            }
        });
    }

    /**
     * Показать игрока для всех остальных и при этом
     * не продублировать его в табе
     *
     * @param player - игрок, которого будем показывать
     */
    public void showPlayer(Player player) {
        ignoredPlayerList.add(player.getPlayerListName());

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.showPlayer(player);
        }

        ignoredPlayerList.remove(player.getPlayerListName());
    }

    /**
     * Скрыть игрока ото всех, но оставить его в табе
     *
     * @param player - игрок, которого будем скрывать
     */
    public void hidePlayer(Player player) {
        ignoredPlayerList.add(player.getPlayerListName());

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.hidePlayer(player);
        }

        ignoredPlayerList.remove(player.getPlayerListName());
    }

    /**
     * Показать игрока для всех остальных и при этом
     * не продублировать его в табе
     *
     * @param player - игрок, которого будем показывать
     * @param target - игроки которых показываем
     */
    public void showPlayer(Player player, Player... target) {
        ignoredPlayerList.add(player.getPlayerListName());

        for (Player targetPlayer : target) {
            targetPlayer.showPlayer(player);
        }

        ignoredPlayerList.remove(player.getPlayerListName());
    }

    /**
     * Скрыть игрока ото всех, но оставить его в табе
     *
     * @param player - игрок, которого будем скрывать
     * @param target - игроки которых скрываем
     */
    public void hidePlayer(Player player, Player... target) {
        ignoredPlayerList.add(player.getPlayerListName());

        for (Player targetPlayer : target) {
            targetPlayer.hidePlayer(player);
        }

        ignoredPlayerList.remove(player.getPlayerListName());
    }

}