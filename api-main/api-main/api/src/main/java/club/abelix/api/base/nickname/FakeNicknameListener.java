package club.abelix.api.base.nickname;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.util.string.StringUtil;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.comphenix.protocol.PacketType.Play.Server.*;

public class FakeNicknameListener extends PacketAdapter implements Listener {

    public FakeNicknameListener(@NonNull Plugin plugin) {
        super(plugin, SCOREBOARD_TEAM, SCOREBOARD_SCORE, PLAYER_INFO);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static String getNickname(final String name) {
        Gamer gamer = Gamer.getGamer(name);
        if (gamer == null)
            return null;

        FakePlayerNickname playerNick = gamer.getFakeName();
        if (playerNick == null)
            return null;

        return playerNick.getNickname().getName();
    }

    @EventHandler
    public void onTabcomplete(final PlayerChatTabCompleteEvent event) {
        val list = ((List<String>) event.getTabCompletions());
        val iterator = list.listIterator();

        val gamerMap = Gamer.getGamers().stream()
                .filter(gamer -> gamer.getFakeName() != null)
                .collect(Collectors.toMap(
                        Gamer::getName,
                        Gamer::getPlayerName
                ));

        while (iterator.hasNext()) {
            val nickname = gamerMap.get(iterator.next());

            if (nickname == null)
                continue;

            if (!StringUtil.startsWith(nickname, event.getLastToken(), true)) {
                iterator.remove();
                continue;
            }

            iterator.set(nickname);
        }

        for (val fakeName : gamerMap.values()) {
            if (StringUtil.startsWith(fakeName, event.getLastToken(), true)) {
                list.add(fakeName);
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer container = event.getPacket();

        Player player = event.getPlayer();
        PacketType type = container.getType();

        if (type == PLAYER_INFO) {
            processPlayerInfoPacket(container);
        } else if (type == SCOREBOARD_SCORE) {
            processScorePacket(container);
        } else if (type == SCOREBOARD_TEAM) {
            processTeamPacket(player, container);
        }
    }

    private void processScorePacket(@NonNull PacketContainer container) {
        StructureModifier<String> strings = container.getStrings();
        String name = strings.read(0);

        Gamer gamer = Gamer.getGamer(name);
        if (gamer == null)
            return;

        FakePlayerNickname playerNick = gamer.getFakeName();
        if (playerNick == null)
            return;

        strings.write(0, playerNick.getNickname().getName());
    }

    protected void processPlayerInfoPacket(@NonNull PacketContainer container) {
        StructureModifier<EnumWrappers.PlayerInfoAction> actions = container.getPlayerInfoAction();
        if (actions.read(0) != EnumWrappers.PlayerInfoAction.ADD_PLAYER)
            return;

        StructureModifier<List<PlayerInfoData>> playerInfos = container.getPlayerInfoDataLists();
        List<PlayerInfoData> playerInfoList = playerInfos.read(0);

        int listSize = playerInfoList.size();
        for (int i = 0; i < listSize; i++) {
            PlayerInfoData playerInfo = playerInfoList.get(i);
            WrappedGameProfile oldProfile = playerInfo.getProfile();

            val nickname = getNickname(oldProfile.getName());

            if (nickname == null)
                continue;

            WrappedGameProfile fixedProfile = oldProfile.withName(nickname);
            fixedProfile.getProperties().putAll(oldProfile.getProperties());

            playerInfoList.set(i, new PlayerInfoData(
                    fixedProfile,
                    playerInfo.getLatency(),
                    playerInfo.getGameMode(),
                    null
            ));
        }

        playerInfos.write(0, playerInfoList);
    }

    @SuppressWarnings("unchecked")
    protected void processTeamPacket(@NonNull Player player, @NonNull PacketContainer container) {
        int action = container.getIntegers().read(1);

        if (action != 0 && action != 3 && action != 4)
            return;

        List<String> list = (List<String>) container.getSpecificModifier(Collection.class)
                .read(0);

        if (!list.isEmpty()) {
            int listSize = list.size();

            for (int i = 0; i < listSize; i++) {
                val name = list.get(i);

                val nickname = getNickname(name);

                if (nickname == null)
                    continue;

                if (name.equals(player.getName())) {
                    list.add(name);
                }

                list.set(i, nickname);
            }

            container.getSpecificModifier(Collection.class).write(0, list);
        }
    }

}
