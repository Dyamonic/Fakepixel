package club.abelix.core.app.data.queue.waiting;

import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.both.BQueueJoinQueue;
import club.abelix.network.type.mode.Mini;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author WhileInside
 */
public final class GameQueue {

    private final Mini mini;
    private final Set<Gamer> queuedUsers;

    private final int trigger;

    private final Set<AbstractConnectable> registrars;

    public GameQueue(Mini mini, int trigger) {
        this.mini = mini;
        this.queuedUsers = new HashSet<>();
        this.registrars = new HashSet<>();

        this.trigger = trigger;
    }

    private Stream<Gamer> getFromQueuedServers() {
        return ServerManager.IMP.getServers(mini).stream()
                .map(server -> ((ArenaServer) server))
                .filter(arena -> arena.getState() == GameState.STAND_BY)
                .flatMap(server -> server.getGamers().stream());
    }

    public Mini getMini() {
        return mini;
    }

    public Collection<Gamer> getQueuedUsers() {
//        Collection<Gamer> fromServers = getFromQueuedServers()
//                .collect(Collectors.toSet());

        Collection<Gamer> total;

        synchronized (queuedUsers) {
            total = new HashSet<>(queuedUsers.size());
            total.addAll(queuedUsers);
//            total.addAll(fromServers);
        }

        return total;
    }

    public int getQueuedUsersCount() {
        synchronized (queuedUsers) {
            return (int) (queuedUsers.size() + getFromQueuedServers().count());
        }
    }

    public void addRegistrar(AbstractConnectable server) {
        synchronized (registrars) {
            registrars.add(server);
        }
    }

    public void removeRegistrar(AbstractConnectable server) {
        synchronized (registrars) {
            registrars.remove(server);
        }
    }

    public boolean hasRegistrars() {
        synchronized (registrars) {
            return !registrars.isEmpty();
        }
    }

    private AbstractServer findServer(Collection<Gamer> gamers) {
        List<ArenaServer> servers = ServerManager.IMP.getServers(mini)
                .stream().map(server -> ((ArenaServer) server))
                .filter(server -> server.getState() == GameState.STAND_BY && server.getSlots() >= trigger)
                .sorted((o1, o2) -> -Integer.compare(o1.getOnline(true), o2.getOnline(true)))
                .collect(Collectors.toList());

        for (ArenaServer server : servers) {
            Collection<Gamer> onServer = new HashSet<>(server.getGamers(true));
            onServer.removeAll(gamers);

            int slots = server.getSlots();
            int online = onServer.size();

            if (slots - online >= trigger) {
                return server;
            }
        }

        return null;
    }


    public void trigger() {
        Collection<Gamer> queuedUsers = getQueuedUsers();

        while (queuedUsers.size() >= trigger) {
            int cnt = trigger;

            Collection<Gamer> gamers = new HashSet<>(trigger);
            Iterator<Gamer> iterator = queuedUsers.iterator();

            while (iterator.hasNext() && cnt-- > 0) {
                Gamer gamer = iterator.next();
                gamers.add(gamer);

                iterator.remove();
                synchronized (this.queuedUsers) {
                    this.queuedUsers.remove(gamer);
                }
            }

            if (gamers.size() != trigger) {
                this.queuedUsers.addAll(gamers);
                continue;
            }

            Logging.IMP.debug("[GAMER QUEUE] Related gamer: {}", gamers.toString());
//            ArenaServerQueueManager.IMP.addQueueUnitToMiniQueue(mini, new QueueRelatedGamersUnit(new ArrayList<>(gamers)));
            AbstractServer server = findServer(gamers);

            if (server == null) {
                continue;
            }

            for (Gamer gamer : gamers) {
                gamer.sendTo(server);
            }

        }
    }

    public boolean removeUser(Gamer user) {
        synchronized (queuedUsers) {
            return queuedUsers.remove(user);
        }
    }

    private void _addUser(Gamer user) {
        synchronized (queuedUsers) {
            queuedUsers.add(user);

            user.sendLocalizedMessage("queue_players", "%mode%", mini.getDisplayName(),
                    "%total%", queuedUsers.size(), "%label%", CommonWords.PLAYER.get(user.getLanguage())
                            .withNumber(queuedUsers.size()));
        }
    }

    public void addUser(Gamer user) {
        _addUser(user);

        Party party = user.getParty();

        if (party != null && party.isLeader(user)) {
            for (Gamer member : party.getGamers()) {
                if (member == user) {
                    continue;
                }

                AbstractServer server = member.getServer();

                if (server == null) {
                    continue;
                }

                if (!registrars.contains(server)) {
                    continue;
                }

                boolean quit = GameQueueManager.INSTANCE.quitQueue(member);

                if (quit) {
                    member.sendLocalizedMessage("queue_quit_by_party_leader");
                }

                _addUser(member);

                user.sendToServer(new BQueueJoinQueue(user.getPlayerId(), mini));
            }
        }

        trigger();
    }

}
