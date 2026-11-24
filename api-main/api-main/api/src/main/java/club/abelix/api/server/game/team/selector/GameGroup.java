package club.abelix.api.server.game.team.selector;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.math.MathUtil;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class GameGroup implements Comparable<GameGroup> {

    private final Game game;
    private final Set<Gamer> gamers;
    private Gamer leader;

    public GameGroup(Game game, Gamer leader, Set<Gamer> gamers) {
        this.game = game;
        this.leader = leader;
        this.gamers = gamers;
    }

    public GameGroup(Game game, Gamer leader) {
        this(game, leader, Sets.newHashSet(leader));
    }

    public int getGamerCount() {
        return gamers.size();
    }

    public GameGroup[] splitPlayers(int n) {
        GameGroup[] split = new GameGroup[MathUtil.divideCeil(gamers.size(), n)];

        int cnt = split.length;
        Iterator<Gamer> iter = gamers.iterator();

        while (iter.hasNext()) {
            GameGroup group = null;

            for (int i = 0; i < n; i++) {
                Gamer gamer = iter.next();

                if (i == 0) {
                    group = split[--cnt] = new GameGroup(game, gamer);
                } else {
                    group.addPlayer(gamer);
                }
            }
        }

        return split;
    }

    public Game getGame() {
        return game;
    }

    public Gamer getLeader() {
        return leader;
    }

    public void setLeader(Gamer leader) {
        this.leader = leader;
    }

    public Set<Gamer> getGamers() {
        return gamers;
    }

    public void broadcast(String message) {
        gamers.forEach(gamer -> gamer.sendMessage(message));
    }

    public void broadcastLocalized(String key, Function<Gamer, ObjectLinkedBuf> objectSupplier) {
        gamers.forEach(gamer -> game.sendLocalizedMessage(gamer, key, objectSupplier.apply(gamer).toArray()));
    }

    public void addPlayer(Gamer gamer) {
        gamers.add(gamer);

        game.getSelectorManager().updateGuiExclude(Collections.emptyList());
    }

    public void removePlayer(Gamer gamer) {
        gamers.remove(gamer);

        game.getSelectorManager().updateGuiExclude(Collections.emptyList());
    }

    public boolean hasPlayer(Gamer gamer) {
        return gamers.contains(gamer);
    }

    @Override
    public int compareTo(GameGroup o) {
        return Integer.compare(getGamerCount(), o.getGamerCount());
    }
}
