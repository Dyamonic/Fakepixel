package club.abelix.core.app.connection.server;

import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.queue.waiting.GameQueue;
import club.abelix.core.app.data.queue.waiting.GameQueueManager;
import club.abelix.core.app.event.gamer.StaffMessageEvent;
import club.abelix.core.app.event.proxy.ProxyConnectEvent;
import club.abelix.core.app.event.proxy.ProxyDisconnectEvent;
import club.abelix.core.app.event.server.ServerConnectEvent;
import club.abelix.core.app.event.server.ServerDisconnectEvent;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.thread.ThreadUncaughtExceptionHandler;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.play.both.BBroadcastChat;
import club.abelix.network.packet.play.both.BBroadcastChatLocalized;
import club.abelix.network.packet.play.server.SRegisterCommands;
import club.abelix.network.packet.play.server.SRegisterServers;
import club.abelix.network.packet.play.server.SUnregisterServer;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.type.server.ServerConnectionInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ServerManager {

    @Dependency
    public static final ServerManager IMP = new ServerManager();

    private final Map<String, AbstractServer> servers
            = new ConcurrentHashMap<>();

    private final Map<String, Proxy> proxies
            = new ConcurrentHashMap<>();

    private final Map<ServerMode, Set<AbstractServer>> serversByMode
            = new ConcurrentHashMap<>();

    private final Map<String, CompletableFuture<AbstractServer>> serverFutures
            = new ConcurrentHashMap<>();

    private final Predicate<ArenaServer> STAND_BY = s -> s.getState() == GameState.STAND_BY;
    private final Predicate<ArenaServer> STARTING = s -> s.getState() == GameState.STARTING;
    private final Predicate<ArenaServer> NOT_PRIVATE = s -> !s.isPrivateGame();

    private final Cache<String, AbstractConnectable> awaitingDisconnect = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .removalListener(this::handleRemove)
            .build();

    private final ScheduledExecutorService shuffleScheduler = Executors.newScheduledThreadPool(3,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("[Core/FP] Shuffle server manager #%1$d")
                    .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
                    .build()
    );

    public void awaitDisconnect(AbstractConnectable connectable) {
        awaitingDisconnect.put(connectable.getName(), connectable);
    }

    private void handleRemove(Object key, Object server, RemovalCause cause) {
//        if (cause == RemovalCause.EXPIRED && server instanceof AbstractConnectable connectable) {
//            log.warn("Server {} was removed from the pool after reaching ping timeout", connectable.getName());

//            switch (connectable) {
//                case AbstractServer s -> removeServer(s);
//                case Proxy p -> removeProxy(p);
//                default -> throw new IllegalStateException("Unexpected value: " + connectable);
//            }
//        }
    }

    public CompletableFuture<AbstractServer> getFutureServer(@NotNull String name) {
        AbstractServer server = getServer(name);

        if (server != null) {
            return CompletableFuture.completedFuture(server);
        }

        return serverFutures.computeIfAbsent(name.toLowerCase(), x -> new CompletableFuture<>());
    }

    public boolean hasProxy(String name) {
        Proxy proxy = this.proxies.get(name.toLowerCase());
        return proxy != null && proxy.getConnection().isActive();
    }

    public boolean hasServer(String name) {
        AbstractServer server = this.servers.get(name.toLowerCase());
        return server != null && server.getConnection().isActive();
    }

    public AbstractServer getServer(@NonNull String name) {
        return servers.get(name.toLowerCase());
    }

    public AbstractServer getServer(@NonNull ServerMode mode) {
        return Iterables.getFirst(getConnectableServers(mode), null);
    }

    public int getServerCount() {
        return servers.size();
    }

    public Collection<AbstractServer> getServers() {
        return Collections.unmodifiableCollection(new HashSet<>(servers.values()));
    }

    public Collection<AbstractServer> getServers(@NonNull ServerMode mode) {
        return Collections.unmodifiableCollection(new HashSet<>(serversByMode.get(mode)));
    }

    public Collection<AbstractServer> getConnectableServers(@NonNull ServerMode mode) {
        val result = new HashSet<>(serversByMode.get(mode));

        result.removeIf(s -> !s.canJoin());

        return Collections.unmodifiableCollection(result);
    }

    public Set<String> findServerNames(@NonNull String regexp) {
        Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);

        return servers.keySet().stream()
                .filter(serverName -> pattern.matcher(serverName).matches())
                .collect(Collectors.toSet());
    }

    public Set<AbstractServer> findServers(@NonNull String regexp) {
        Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);

        return servers.values().stream()
                .filter(server -> pattern.matcher(server.getName()).matches())
                .collect(Collectors.toSet());
    }

    public Proxy getProxy(@NonNull String name) {
        return proxies.get(name.toLowerCase());
    }

    public Collection<Proxy> getProxies() {
        return Collections.unmodifiableCollection(proxies.values());
    }

    public void addServer(@NonNull AbstractServer server) {
        String serverName = server.getName().toLowerCase();

        AbstractServer previousServer = servers.put(serverName, server);

        if (previousServer != null && previousServer.getMode() != null) {
            val previousQueue = serversByMode.get(previousServer.getMode());

            if (previousQueue != null) {
                previousQueue.remove(previousServer);

                if (previousQueue.isEmpty()) {
                    serversByMode.remove(previousServer.getMode(), previousQueue);
                }
            }
        }

        if (server.getMode() != null) {
            val servers = serversByMode.get(server.getMode());

            // todo: fix it
            if (servers != null) {
                AbstractServer removeServer = null;
                for (val iterServer : servers) {
                    if (iterServer.getName().equalsIgnoreCase(server.getName())) {
                        removeServer = iterServer;
                        break;
                    }
                }

                val remove = removeServer != null && servers.remove(removeServer);

                if (remove) {
                    log.info("Removed old server from pool: {}", server.getName());
                }
            }


            serversByMode.computeIfAbsent(server.getMode(), m -> ConcurrentHashMap.newKeySet())
                    .add(server);
        }

        CompletableFuture<AbstractServer> serverFuture = serverFutures.remove(serverName);

        if (serverFuture != null) {
            serverFuture.complete(server);
        }

        if (!(server instanceof MasterServer)) {
            sendToProxies(new SRegisterServers(server.getName(), server.getHost(), server.getPort()));
        }

        if (server instanceof MiniServer) {
            GameQueue queue = GameQueueManager.INSTANCE.getQueue(server.getType().getMini());

            if (queue != null) {
                queue.trigger();
            }
        }

        CoreApplication.getInstance().getPluginManager().callEvent(
                new ServerConnectEvent(server)
        );
    }

    public void addProxy(@NonNull Proxy proxy) {
        String serverName = proxy.getName().toLowerCase();

        if (awaitingDisconnect.asMap().containsKey(serverName)) {
            awaitingDisconnect.invalidate(serverName);
            log.info("Server {} returned to the pool after reaching ping timeout", proxy.getName());
            return;
        }

        proxies.put(proxy.getName().toLowerCase(), proxy);

        proxy.sendPacket(new SRegisterServers(
                servers.values().stream()
                        .filter(AbstractServer::isAddingToProxy)
                        .map(AbstractServer::getInfo)
                        .toArray(ServerConnectionInfo[]::new)
        ));

        proxy.sendPacket(new SRegisterCommands(CoreApplication.getInstance().getCommands()));

        CoreApplication.getInstance().getPluginManager().callEvent(
                new ProxyConnectEvent(proxy)
        );
    }

    public int getTotalOnline() {
        int total = 0;

        for (Proxy proxy : proxies.values()) {
            for (Gamer gamer : proxy.getGamers()) {
                if (gamer.isVanished()) {
                    continue;
                }

                total++;
            }
        }

        return total;
    }

    public int getOnlineByMode(ServerMode mode) {
        int total = 0;

        for (AbstractServer server : getServers(mode)) {
            for (Gamer gamer : server.getGamers()) {
                if (gamer.isVanished()) {
                    continue;
                }

                total++;
            }
        }

        return total;
    }

    public void broadcastChatLocalized(@NonNull final String key, final String... objects) {
        sendToProxies(new BBroadcastChatLocalized(key, objects));
    }

    public void broadcastChatLocalized(@NonNull Language language, @NonNull final String key, final String... objects) {
        sendToProxies(new BBroadcastChatLocalized(language.getId(), key, objects));
    }

    public void broadcastChat(@NonNull String message) {
        sendToProxies(new BBroadcastChat(message));
    }

    public void broadcastStaff(@NonNull String localizationKey, @NonNull Object... params) {
        broadcastStaff("command.staffchat", localizationKey, params);
    }

    public void broadcastStaff(@NonNull LanguageContext context) {
        broadcastStaff("command.staffchat", context);
    }

    public void broadcastStaff(@NonNull String permission, @NonNull String localizationKey,
                               @NotNull Function<Language, ObjectLinkedBuf> function) {
        broadcastStaffPredicated(permission, new LanguageContext(localizationKey, function), (gamer) -> true);
    }

    public void broadcastStaff(@NonNull String permission, @NonNull String localizationKey, @NonNull Object... params) {
        broadcastStaffPredicated(permission, localizationKey, (gamer) -> true, params);
    }

    public void broadcastStaffPredicated(
            final @NonNull String permission,
            final @NonNull String localizationKey,
            final @NonNull Predicate<Gamer> predicate,
            final @NonNull Object... params
    ) {
        ObjectLinkedBuf array = new ObjectLinkedBuf(params);

        broadcastStaffPredicated(permission, new LanguageContext(localizationKey, language -> array), predicate);
    }

    public void broadcastStaff(@NonNull String permission, @NonNull LanguageContext context) {
        broadcastStaffPredicated(permission, context, gamer -> true);
    }

    public void broadcastStaffPredicated(
            final @NonNull String permission,
            final @NonNull LanguageContext context,
            final @NonNull Predicate<Gamer> predicate
    ) {

        StaffMessageEvent event = new StaffMessageEvent(context);
        CoreApplication.getInstance().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        predicatedGroupBroadcast(permission, context, predicate);

        ConsoleCommandSender.INSTANCE.sendMessage(context.format(Language.RUSSIAN));
    }

    public void broadcastGroup(@NonNull String permission, @NonNull String localizationKey, @NonNull Object... params) {
        ObjectLinkedBuf array = new ObjectLinkedBuf(params);

        broadcastGroup(permission, new LanguageContext(localizationKey, language -> array));
    }

    public void broadcastGroup(@NonNull String permission, @NonNull LanguageContext context) {
        predicatedGroupBroadcast(permission, context, (gamer) -> true);
    }

    public void predicatedGroupBroadcast(@NonNull String permission, @NonNull Predicate<Gamer> predicate,
                                         @NonNull String localizationKey, @NonNull Object... params) {

        ObjectLinkedBuf array = new ObjectLinkedBuf(params);
        predicatedGroupBroadcast(permission, new LanguageContext(localizationKey, language -> array), predicate);
    }

    public void predicatedGroupBroadcast(
            final @NonNull String permission,
            final @NonNull LanguageContext ctx,
            final @NonNull Predicate<Gamer> predicate
    ) {

        Gamer.getGamers().forEach(g -> {
            if (g.hasPermission(permission)) {
                if (predicate.test(g) && g.isActivePersonalChat()) {
                    g.sendMessage(ctx.format(g.getLanguage()));
                }
            }
        });
    }

    public void removeServer(@NonNull AbstractServer server) {
        String serverName = server.getName().toLowerCase();

        servers.remove(serverName);

        if (server.getMode() != null) {
            synchronized (serversByMode) {
                val servers = serversByMode.get(server.getMode());

                if (servers != null) {
                    servers.remove(server);

                    if (servers.isEmpty()) {
                        serversByMode.remove(server.getMode(), servers);
                    }
                }
            }
        }

        for (Gamer gamer : new ArrayList<>(server.getGamers())) {
            gamer.setServer(null);
            if (server.getMode() == Uncommon.SKYBLOCK) {
                gamer.sendTo(Uncommon.SKYBLOCK);
            } else {
                gamer.sendTo(Lobby.HUB);
            }
        }

        if (server.isAddingToProxy()) {
            sendToProxies(new SUnregisterServer(serverName));
        }

        CoreApplication.getInstance().getPluginManager().callEvent(
                new ServerDisconnectEvent(server)
        );

        server.close();
    }

    public void removeProxy(@NonNull Proxy proxy) {
        proxies.remove(proxy.getName().toLowerCase());

        for (Gamer gamer : new HashSet<>(proxy.getGamers())) {
            gamer.quit();
        }

        CoreApplication.getInstance().getPluginManager().callEvent(
                new ProxyDisconnectEvent(proxy)
        );

        proxy.close();
    }

    public void sendToServers(@NonNull Packet<?> packet) {
        for (AbstractServer server : servers.values()) {
            server.sendPacket(packet);
        }
    }

    public void sendToProxies(@NonNull Packet<?> packet) {
        for (Proxy proxy : proxies.values()) {
            proxy.sendPacket(packet);
        }
    }

    private AbstractServer selectServer(@NonNull Predicate<AbstractServer> filter,
                                        @NonNull BinaryOperator<AbstractServer> reducer,
                                        @NonNull Collection<ServerMode> modes) {
        AbstractServer best = null;

        synchronized (serversByMode) {
            // Если указаны префиксы, чекаем только указанные префиксы
            // Иначе чекаем вообще все сервера, что есть
            for (ServerMode mode : (modes.isEmpty() ? serversByMode.keySet() : modes)) {
                Collection<AbstractServer> servers = serversByMode.get(mode);

                for (AbstractServer server : servers) {
                    if (!server.canJoin() || !filter.test(server)) {
                        continue;
                    }

                    if (best == null) {
                        best = server;
                    } else {
                        best = reducer.apply(best, server);
                    }
                }
            }
        }

        return best;
    }

    private ArrayList<AbstractServer> selectServers(@NonNull Predicate<AbstractServer> filter,
                                                    @NonNull Comparator<AbstractServer> comparator,
                                                    @NonNull Collection<ServerMode> modes) {
        ArrayList<AbstractServer> result = new ArrayList<>();

        synchronized (serversByMode) {
            // Если указаны префиксы, чекаем только указанные префиксы
            // Иначе чекаем вообще все сервера, что есть
            for (ServerMode mode : (modes.isEmpty() ? serversByMode.keySet() : modes)) {
                Collection<AbstractServer> servers = serversByMode.get(mode);

                for (AbstractServer server : servers) {
                    if (!server.canJoin() || !filter.test(server)) {
                        continue;
                    }
                    result.add(server);
                }
            }
        }

        result.sort(comparator);

        return result;
    }

    public ArenaServer selectArenaServer(@NonNull Predicate<ArenaServer> filter,
                                         @NonNull BinaryOperator<ArenaServer> reducer,
                                         @NonNull Collection<Mini> minis) {
        ArenaServer bestServer = null;

        // Если указаны префиксы, чекаем только указанные префиксы
        // Иначе чекаем вообще все сервера, что есть
        for (val mini : minis) {
            synchronized (serversByMode) {
                List<ArenaServer> serverList =
                        getConnectableServers(mini).stream()
                                .map(server -> (ArenaServer) server)
                                .filter(MiniServer::canJoin)
                                .filter(filter)
                                .collect(Collectors.toList());

                Collections.shuffle(serverList, ThreadLocalRandom.current());

                for (val server : serverList) {
                    if (bestServer == null) {
                        bestServer = server;
                    } else {
                        bestServer = reducer.apply(bestServer, server);
                    }
                }

            }
        }

        return bestServer;
    }

    // Выбираем сервер по префиксу с максимальным/минимальным онлайном
    public AbstractServer getBestByOnline(@NonNull ServerMode mode, boolean max) {
        BinaryOperator<AbstractServer> reducer = max
                ? (best, server) -> (best.getGamers().size() > server.getGamers().size()) ? best : server
                : (best, server) -> (best.getGamers().size() < server.getGamers().size()) ? best : server;

        return selectServer(server -> true, reducer, Collections.singletonList(mode));
    }

    public ArenaServer getBestBySpace(@NonNull Collection<Mini> minis, int space) {
        return selectArenaServer(
                STAND_BY.and(NOT_PRIVATE).and(server -> server.getSpace() >= space),
                (best, server) -> best.getFillRate() > server.getFillRate() ? best : server,
                minis
        );
    }

    // Игрок прыгает в портал на лобби
    public ArenaServer getBestBySpace(@NonNull Master mode, int space) {
        return getBestBySpace(mode.getMinis(), space);
    }

    // Игрок выбрал сабмод, но не выбрал карту
    public ArenaServer getBestBySpace(@NonNull Mini mini, int space) {
        return getBestBySpace(Collections.singleton(mini), space);
    }

    // Игрок выбрал определенную карту
    private static String normalizeMapName(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public ArenaServer getBestWithMap(@NonNull Mini mini, @NonNull String map, int space) {
        String want = normalizeMapName(map);

        return selectArenaServer(
                STAND_BY.and(NOT_PRIVATE).and(server -> {
                    if (server.getSpace() < space) return false;

                    GameMap gm = server.getMap();
                    return want.equals(normalizeMapName(gm.getTechnicalName()))
                            || want.equals(normalizeMapName(gm.getDisplayName()));
                }),
                (best, server) -> best.getGamers().size() > server.getGamers().size() ? best : server,
                Collections.singleton(mini)
        );
    }

    public ArrayList<AbstractServer> getBestByOnlineServers(@NonNull ServerMode mode, boolean max) {
        Comparator<AbstractServer> comparator = max
                ? Comparator.comparingInt(AbstractServer::getSpace)
                : Comparator.comparingInt(AbstractServer::getOnline);

        return selectServers(server -> true,
                comparator,
                Collections.singletonList(mode));
    }

    public ArrayList<AbstractServer> getBestBySpaceServers(@NonNull ServerMode mode, int space) {
        return selectServers(server -> server.getSpace() >= space,
                Comparator.comparingInt(AbstractServer::getOnline),
                Collections.singletonList(mode));
    }

    public ArrayList<AbstractServer> getFullestServers(@NonNull ServerMode mode, int space) {
        return selectServers(server -> server.getSpace() >= space,
                Comparator.<AbstractServer>comparingInt(AbstractServer::getOnline).reversed(),
                Collections.singletonList(mode));
    }

    public Set<Mini> findAvailableMinis() {
        return servers.values()
                .stream()
                .map(AbstractServer::getMode)
                .filter(ServerMode::isMini)
                .map(s -> (Mini) s)
                .collect(Collectors.toUnmodifiableSet());
    }

}
