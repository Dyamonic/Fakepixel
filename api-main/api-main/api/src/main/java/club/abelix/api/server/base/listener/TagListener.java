package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.team.FakeTeam;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collection;

public abstract class TagListener extends YListener {

    protected final TIntObjectMap<FakeTeam> playerTagMap = new TIntObjectHashMap<>();
    private static final int MAX_RANK_PRIORITY = 999;

    public TagListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    public void addTeam(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        if (gamer != null) {
            addTeam(gamer);
        }
    }

    protected void sendCreateTeams(@NonNull Player player) {
        for (FakeTeam team : playerTagMap.valueCollection()) {
            team.addReceiver(player);
        }
    }

    protected void sendRemoveTeams(@NonNull Player player) {
        for (FakeTeam team : playerTagMap.valueCollection()) {
            team.removeReceiver(player);
        }
    }

    protected void addTeam(@NonNull Gamer gamer) {
        //удалим старый тэг если он есть
        removeTeam(gamer);

        val group = gamer.getShownGroup();

        int order = MAX_RANK_PRIORITY - group.getPriority();

        String uid = String.format("%03d", order)
                + "/" + gamer.getPlayerName().substring(0, 3)
                + "/" + gamer.getPlayerId();

        String suffix = gamer.getSuffixText();

        if (!suffix.isEmpty() && suffix.charAt(0) == '§') {
            String color = suffix.substring(0, 2);

            suffix = color + " " + suffix.substring(2);
        } else {
            suffix = " " + suffix;
        }

        FakeTeam team = FalconAPI.getNmsApi().getFakeTeamApi().createTeam(uid, gamer.getTabListName(), suffix);
        team.addPlayer(gamer.getName());
        team.setColor(gamer.getPrefixColor());
        team.addReceivers(getPlayers());

        gamer.setPlayerTag(team);

        playerTagMap.put(gamer.getPlayerId(), team);
    }

    protected void removeTeam(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        if (gamer != null) {
            removeTeam(gamer);
        }
    }

    protected void removeTeam(@NonNull Gamer gamer) {
        FakeTeam team = playerTagMap.remove(gamer.getPlayerId());

        if (team != null) {
            gamer.setPlayerTag(null);

            team.remove();
        }
    }

    protected abstract Collection<Player> getPlayers();

}