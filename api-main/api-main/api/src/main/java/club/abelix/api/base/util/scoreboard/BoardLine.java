package club.abelix.api.base.util.scoreboard;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.scoreboard.score.ScoreActionMode;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;

class BoardLine {

    private final NmsApi nmsApi = FalconAPI.getNmsApi();

    private final Board board;
    private final int index;

    private String text;

    /**
     * Конструктор линии.
     *
     * @param index - индекс линию, чем выше индекс, тем выше линия
     * @param text  - текст линии
     * @param board - текущий скорборд
     */
    BoardLine(int index, @NonNull String text, @NonNull Board board) {
        this.index = index;
        this.text = text;
        this.board = board;

        Preconditions.checkArgument(board.getObjective() != null, "Objective cannot be null.");
    }

    /**
     * Установка текста линии.
     *
     * @param text - строка
     */
    public void setText(@NonNull String text) {
        if (this.text.equals(text)) {
            return;
        }

        this.text = text;

        PacketContainer teamPacket = getTeamPacket(TeamAction.UPDATE);
        PacketContainer scorePacket = getScorePacket(ScoreActionMode.CHANGE);

        broadcastPackets(board.getReceivers(), teamPacket, scorePacket);
    }

    /**
     * Спрятать линию
     */
    public void hide(@NonNull Collection<Player> players) {
        PacketContainer teamPacket = getTeamPacket(TeamAction.REMOVE);
        PacketContainer scorePacket = getScorePacket(ScoreActionMode.REMOVE);

        broadcastPackets(players, teamPacket, scorePacket);
    }

    /**
     * Показать линию
     */
    public void show(@NonNull Collection<Player> players) {
        PacketContainer teamPacket = getTeamPacket(TeamAction.CREATE);
        PacketContainer addPlayersPacket = getTeamPacket(TeamAction.PLAYERS_ADD);

        PacketContainer scorePacket = getScorePacket(ScoreActionMode.CHANGE);

        broadcastPackets(players, teamPacket, addPlayersPacket, scorePacket);
    }

    private PacketContainer getTeamPacket(@NonNull TeamAction teamAction) {
        //Чтобы не дубрировались строки
        String result = ChatColor.values()[index].toString();

        FakeTeam team = nmsApi.getFakeTeamApi().createTeam("text-" + index);
        team.addPlayers(Lists.newArrayList(result));

        if (teamAction == TeamAction.CREATE || teamAction == TeamAction.UPDATE) {
            Iterator<String> iterator = Splitter.fixedLength(16).split(text).iterator();
            String prefix = iterator.next();

            team.setPrefix(prefix);
            team.setSuffix("");

            //Магия, не трогать
            if (text.length() > 16) {
                String prefixColor = ChatColor.getLastColors(prefix);
                String suffix = iterator.next();

                if (prefix.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                    prefix = prefix.substring(0, prefix.length() - 1);
                    team.setPrefix(prefix);
                    prefixColor = ChatColor.getByChar(suffix.charAt(0)).toString();
                    suffix = suffix.substring(1);
                }

                if (prefixColor == null)
                    prefixColor = "";

                if (suffix.length() > 16) {
                    suffix = suffix.substring(0, (13 - prefixColor.length()));
                }

                team.setSuffix((prefixColor.equals("") ? ChatColor.RESET : prefixColor) + suffix);
            }
        }

        return nmsApi.getPacketLibraryApi().getTeamPacket(teamAction, team, team.getPlayers());
    }

    private PacketContainer getScorePacket(ScoreActionMode action) {
        return nmsApi.getPacketLibraryApi().getScoreboardScorePacket(board.getObjective(), ChatColor.values()[index].toString(), action, index);
    }

    private void broadcastPackets(@NonNull Collection<Player> players, @NonNull PacketContainer... packetContainers) {
        PacketLibraryApi packetLibraryApi = nmsApi.getPacketLibraryApi();

        players.forEach(player -> {
            for (PacketContainer packetContainer : packetContainers) {
                packetLibraryApi.sendPacket(player, packetContainer);
            }
        });
    }

}
