package club.abelix.core.app.connection;

import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.data.nickname.FakeNickname;
import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.EnvUtility;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.app.Async;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.CommonServer;
import club.abelix.core.app.connection.server.MasterServer;
import club.abelix.core.app.connection.server.Proxy;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.booster.NetworkBoosterManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.nickname.NickManager;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.core.app.data.queue.waiting.GameQueue;
import club.abelix.core.app.data.queue.waiting.GameQueueManager;
import club.abelix.core.app.data.report.ReportManager;
import club.abelix.core.app.data.report.meta.Report;
import club.abelix.core.app.data.stream.StreamManager;
import club.abelix.core.app.event.arena.ArenaChangeStateEvent;
import club.abelix.core.app.event.core.ClientActiveEvent;
import club.abelix.core.app.event.core.GetBestServerEvent;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.GamerLoadEvent;
import club.abelix.core.app.event.gamer.PlayerGameRedirectEvent;
import club.abelix.core.app.event.gamer.PlayerLoginEvent;
import club.abelix.core.app.event.gamer.PlayerRedirectEvent;
import club.abelix.core.app.kubernetes.PodReadyWatcher;
import club.abelix.core.app.util.LanguageUtil;
import club.abelix.network.channel.AbstractRemoteClientChannel;
import club.abelix.network.common.NetworkPlayerLoad;
import club.abelix.network.common.enums.ConnectionType;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.handshake.Result;
import club.abelix.network.packet.handshake.packet.HRequest;
import club.abelix.network.packet.handshake.packet.HResponse;
import club.abelix.network.packet.play.both.BAddServerJoinMessage;
import club.abelix.network.packet.play.both.BBestPlayerDonate;
import club.abelix.network.packet.play.both.BBestPlayerUpdate;
import club.abelix.network.packet.play.both.BBroadcastChat;
import club.abelix.network.packet.play.both.BChat;
import club.abelix.network.packet.play.both.BLicenseStatus;
import club.abelix.network.packet.play.both.BProxyBotCheck;
import club.abelix.network.packet.play.both.BProxyKick;
import club.abelix.network.packet.play.both.BQueueJoinQueue;
import club.abelix.network.packet.play.both.BRemoveProperty;
import club.abelix.network.packet.play.both.BSetProperty;
import club.abelix.network.packet.play.both.BUpdatePlayerLanguage;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import club.abelix.network.packet.play.client.CActivateNetworkBooster;
import club.abelix.network.packet.play.client.CAddPartyMember;
import club.abelix.network.packet.play.client.CArenaCreate;
import club.abelix.network.packet.play.client.CArenaJoinBroadcast;
import club.abelix.network.packet.play.client.CArenaSetGameMap;
import club.abelix.network.packet.play.client.CArenaSetState;
import club.abelix.network.packet.play.client.CChangePlayerNick;
import club.abelix.network.packet.play.client.CConnectExact;
import club.abelix.network.packet.play.client.CConnectMap;
import club.abelix.network.packet.play.client.CConnectMode;
import club.abelix.network.packet.play.client.CConnectToPlayer;
import club.abelix.network.packet.play.client.CCreateParty;
import club.abelix.network.packet.play.client.CDisbandParty;
import club.abelix.network.packet.play.client.CDispatchCommand;
import club.abelix.network.packet.play.client.CGetActiveStreams;
import club.abelix.network.packet.play.client.CGetBestServer;
import club.abelix.network.packet.play.client.CGetReports;
import club.abelix.network.packet.play.client.CListPartyMembers;
import club.abelix.network.packet.play.client.CLoadFriends;
import club.abelix.network.packet.play.client.CNetworkBoosterRequest;
import club.abelix.network.packet.play.client.CNetworkPlayerLoadRequest;
import club.abelix.network.packet.play.client.COnlineFriendsCountRequest;
import club.abelix.network.packet.play.client.CPlayerChangeSkin;
import club.abelix.network.packet.play.client.CPlayerCommand;
import club.abelix.network.packet.play.client.CPlayerConnected;
import club.abelix.network.packet.play.client.CPlayerNickGenerate;
import club.abelix.network.packet.play.client.CProxyPlayerDisconnect;
import club.abelix.network.packet.play.client.CProxyPlayerLoginRequest;
import club.abelix.network.packet.play.client.CQueueCreate;
import club.abelix.network.packet.play.client.CQueueQuitQueue;
import club.abelix.network.packet.play.client.CReconnectPlayers;
import club.abelix.network.packet.play.client.CRemovePartyMember;
import club.abelix.network.packet.play.client.CReportAction;
import club.abelix.network.packet.play.client.CReportCheckExistenceRequest;
import club.abelix.network.packet.play.client.CReportCheckStateRequest;
import club.abelix.network.packet.play.client.CSendPlayerFailed;
import club.abelix.network.packet.play.client.CSendServerCommand;
import club.abelix.network.packet.play.client.CServerConnectRequest;
import club.abelix.network.packet.play.client.CServerRestartScheduled;
import club.abelix.network.packet.play.client.CSetJoinableState;
import club.abelix.network.packet.play.client.CSetSlots;
import club.abelix.network.packet.play.client.CSync;
import club.abelix.network.packet.play.client.CTabcompleteRequest;
import club.abelix.network.packet.play.server.SActiveStreams;
import club.abelix.network.packet.play.server.SBestServer;
import club.abelix.network.packet.play.server.SChangePlayerNickResponse;
import club.abelix.network.packet.play.server.SConnectResponse;
import club.abelix.network.packet.play.server.SFailedReconnectPlayers;
import club.abelix.network.packet.play.server.SNetworkBoosterResponse;
import club.abelix.network.packet.play.server.SNetworkPlayerLoadResponse;
import club.abelix.network.packet.play.server.SOnlineFriendsCountResponse;
import club.abelix.network.packet.play.server.SPartyChange;
import club.abelix.network.packet.play.server.SPartyMemberResponse;
import club.abelix.network.packet.play.server.SPlayerNickGenerateResponse;
import club.abelix.network.packet.play.server.SProxyPlayerLoginResponse;
import club.abelix.network.packet.play.server.SReportCheckExistenceResponse;
import club.abelix.network.packet.play.server.SReportCheckStateResponse;
import club.abelix.network.packet.play.server.SReports;
import club.abelix.network.packet.play.server.SServerCommand;
import club.abelix.network.packet.play.server.SServerConnectResponse;
import club.abelix.network.packet.play.server.STabcompleteResponse;
import club.abelix.network.type.booster.NetworkBooster;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.gamer.FailedReconnectGamer;
import club.abelix.network.type.gamer.ReconnectingGamer;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.util.ModeUtil;
import club.abelix.network.type.report.PlayerReport;
import club.abelix.network.type.report.PlayerReportSender;
import club.abelix.network.type.report.ReportReason;
import club.abelix.network.type.server.state.ServerState;
import club.abelix.network.type.stream.Stream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RemoteClientConnection extends AbstractRemoteClientChannel {

    public static final Set<String> PENDING_LOGINS = ConcurrentHashMap.newKeySet();
    private static final ServerManager serverManager = ServerManager.IMP;

    private final CoreApplication core;
    private final StatusQueryExecutionService queryExecutionService;

    @Getter
    private String host;
    @Getter
    private int port;
    @Getter
    private String serverName;
    @Getter
    private AbstractConnectable server;

    public RemoteClientConnection(
            CoreApplication core,
            SocketChannel channel,
            StatusQueryExecutionService queryExecutionService
    ) {
        super(channel);

        this.core = core;
        this.queryExecutionService = queryExecutionService;
    }

    private String getDisplayName() {
        return server == null ? channel.remoteAddress().toString() : server.getName();
    }

    @Override
    public void active() {
        core.getPluginManager().callEvent(new ClientActiveEvent(this, this));
    }

    @Override
    public void process(CSetJoinableState packet, ChannelProcessorContext ctx) {
        if (server instanceof CommonServer) {
            CommonServer server = (CommonServer) this.server;
            server.setJoinable(packet.isJoinable());
        }
    }

    @Override
    protected void onDisconnect() {
        try {
            if (server instanceof Proxy proxy) {

                log.info("Proxy {} disconnected. It had {} players",
                        proxy.getName(), proxy.getOnline(false));

                ServerManager.IMP.removeProxy(proxy);
            }

            if (server instanceof AbstractServer) {
                log.info("Server {} disconnected. It had {} players (Server state: {})",
                        server.getName(), server.getOnline(false), ((AbstractServer) server).getServerState());

                if (server instanceof MasterServer master) {
                    Set<String> removedServers = master.removeServers();

                    if (!removedServers.isEmpty()) {
                        log.info("All arenas on this master server have been removed...");
                        log.info("Removed servers: {}", removedServers.stream()
                                .sorted().collect(Collectors.joining(", ")));
                    }

                }

                ServerManager.IMP.removeServer((AbstractServer) server);
            }

            GameQueueManager.INSTANCE.unregister(server);
        } finally {
            server = null;
        }
    }

    @Override
    public void process(final CSync packet, final ChannelProcessorContext ctx) {
//        val ids = IntStream.of(packet.getIds())
//                .boxed().collect(Collectors.toList());
//
//        for (val gamer : server.getGamers(false)) {
//            if (!ids.contains(gamer.getPlayerId())) {
//                removeFromCore(gamer);
//            }
//        }
    }

    private void removeFromCore(final Gamer gamer) {
        val name = gamer.getName();

        log.info("Player {} remove from core because it's not exists on proxy", name);

        RemoteClientConnection.PENDING_LOGINS.remove(name.toLowerCase());
        gamer.quit();
    }

    @Override
    public void process(CPlayerConnected packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        // игрок только что зашел, вызываем GamerJoinEvent
        // сразу после добавления мы его не вызвали, потому
        // что когда игрок добавлен в кор, его еще нет в банже
        if (gamer.isJustJoined()) {
            core.getPluginManager().callEvent(new GamerJoinEvent(gamer));

            gamer.setJustJoined(false);
        }

        AbstractServer server = ServerManager.IMP.getServer(packet.getServer());

        if (server instanceof ArenaServer arenaServer) {
            arenaServer.unReservation(1);
        }

        gamer.updateServer(server);
    }

    @Override
    public void process(CSendPlayerFailed packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.getWaitingConnection().set(false); // Снимаем ожидание подключения
    }

    @Override
    public void process(@NonNull Throwable throwable) {
        log.error("An error occured while processing packet from {}", getDisplayName(), throwable);
    }

    @Override
    public void process(HRequest packet, ChannelProcessorContext ctx) {
        // клиент пытается авторизоваться
        if (!packet.getToken().equals(core.getToken())) {
            rejectHandshake(ctx, "Bad Token");
            return;
        }

        boolean isProxy = packet.getType() == ConnectionType.PROXY;

        host = channel.remoteAddress().getHostName();
        serverName = packet.getServerName();
        port = packet.getServerPort();

        if (isProxy) {
            if (ServerManager.IMP.hasProxy(serverName)) {
                Proxy oldProxy = ServerManager.IMP.getProxy(serverName);

                if (oldProxy.getConnection().isActive()) {
                    rejectHandshake(ctx, "Proxy with that name already exists: " + serverName);
                    return;
                }

                log.warn("Прокси {} удален по скольку соединение более не активно",
                        oldProxy.getName());

                oldProxy.getConnection().onDisconnect();
                ServerManager.IMP.removeProxy(oldProxy);
            }
        } else {
            if (ServerManager.IMP.hasServer(serverName)) {
                AbstractServer oldServer = ServerManager.IMP.getServer(serverName);

                if (oldServer.getConnection().isActive()) {
                    rejectHandshake(ctx, "Server with that name already exists: " + serverName);
                    return;
                }

                log.warn("Server {} is being removed because the connection is no longer active",
                        oldServer.getName());

                oldServer.getConnection().onDisconnect();
                ServerManager.IMP.removeServer(oldServer);

            }
        }

        sendPacket(new HResponse(Result.SUCCESS, null));
        upgradeConnection(PacketRegistry.PLAY);

        // баккит должен будет отправить слоты и карту (если это мини-игра)
        // для прокси сразу в плей
        if (isProxy) {
            createProxy();
        } else {
            createServer(packet.getMode());
        }
    }

    private void rejectHandshake(ChannelProcessorContext ctx, String message) {
        ctx.getContext()
                .writeAndFlush(new HResponse(Result.FAILED, message))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void createServer(ServerMode mode) {
        ServerType type = new ServerType(serverName, mode);

        server = type.isMaster()
                ? new MasterServer(this, type)
                : new CommonServer(this, type);
        serverManager.addServer((AbstractServer) server);

        log.info("Registered new server {} Waiting for k8s ready status...", getDisplayName());

        core.getExecutor().submit(() -> {
            val watcher = new PodReadyWatcher(core.getKubernetesClient());
            String namespace = Project.CURRENT.getNamespace();

            log.info("Server {}.{} waiting for running state.", namespace, getDisplayName());

            if  (EnvUtility.getEnvBooleanDefault("IS_DEV", "false")) {
                namespace = "dev";
            }

            String podName = serverName.replace('_', '-').toLowerCase();
            watcher.watchPodReady(namespace, podName, () -> {
                ((AbstractServer) server).setServerState(ServerState.RUNNING);
                log.info("Server {} pod is ready. Server new state: running", getDisplayName());
            });
        });
    }

    private void createProxy() {
        server = new Proxy(this, serverName);

        for (Gamer gamer : Gamer.getGamers()) {
            Proxy proxy = gamer.getProxy();

            if (proxy != null && proxy.getName().equals(serverName)) {
                gamer.quit();
            }
        }

        serverManager.addProxy((Proxy) server);

        log.info("Registered new proxy server {}", getDisplayName());
    }

    @Override
    public void process(CArenaCreate packet, ChannelProcessorContext ctx) {
        if (server instanceof MasterServer) {
            MasterServer server = (MasterServer) this.server;

            ArenaServer arena = new ArenaServer(server,
                    this, new ServerType(packet.getServerName(), packet.getMini()),
                    packet.getMini(), packet.getMap()
            );

            arena.setSlots(packet.getSlots());
            arena.setServerState(ServerState.RUNNING);

            log.info("Server {} registered new arena: {}/{}", getDisplayName(), arena.getName(), arena.getMapName());

            server.addServer(arena);
        }
    }

    @Override
    public void process(CArenaJoinBroadcast packet, ChannelProcessorContext ctx) {
        if (server instanceof MasterServer) {
            MasterServer server = (MasterServer) this.server;
            ArenaServer arena = (ArenaServer) serverManager.getServer(packet.getArenaName());

            if (arena == null) {
                return;
            }

            if (arena.getMasterServer() != server) {
                log.error("Try processing arena({}) join from master {}", arena.getName(), server.getName());
            }

            int space = server.getSpace();

            if (space == 0) {
                return;
            }

            Master master = arena.getMaster();

            ObjectLinkedBuf buf = new ObjectLinkedBuf(
                    "%mode%", arena.getFullModeName(),
                    "%server%", arena.getName()
            );

            Collection<Gamer> gamers;

            // если требуется занять больше двух слотов
            // мы пиарим на весь сервак этот Mini
            if (space > 2) {
                gamers = Gamer.getGamers();
            } else { // иначе только на лобби этого режима
                gamers = new ArrayList<>();

                for (AbstractServer lobby : ServerManager.IMP.getServers(master.getLobby())) {
                    gamers.addAll(lobby.getGamers());
                }
            }

            gamers.forEach(gamer ->
                    gamer.sendLocalizedMessage("game_fill_message_broadcast", buf));
        }
    }

    @Override
    public void process(CSetSlots packet, ChannelProcessorContext ctx) {
        AbstractServer server = (AbstractServer) this.server;
        server.setSlots(packet.getSlots());
    }

    @Override
    public void process(CArenaSetState packet, ChannelProcessorContext ctx) {
        if (server instanceof MasterServer) {
            MasterServer server = (MasterServer) this.server;
            ArenaServer arena = (ArenaServer) serverManager.getServer(packet.getArenaName());

            if (arena == null) {
                return;
            }

            if (arena.getMasterServer() != server) {
                log.error("Try processing arena update state ({}) join from master {}", arena.getName(), server.getName());
            }

            GameState oldState = arena.getState();
            GameState newState = packet.getState();

            arena.setState(newState);

            ArenaChangeStateEvent event = new ArenaChangeStateEvent(arena, oldState, newState);
            core.getPluginManager().callEvent(event);

            log.info("Arena {} Update state {} -> {}", arena.getName(), oldState, newState);
        }
    }

    @Override
    public void process(CArenaSetGameMap packet, ChannelProcessorContext ctx) {
        if (server instanceof MasterServer) {
            MasterServer server = (MasterServer) this.server;
            ArenaServer arena = (ArenaServer) serverManager.getServer(packet.getArenaName());

            if (arena == null) {
                return;
            }

            if (arena.getMasterServer() != server) {
                log.error("Try processing arena set map ({}) join from master {}", arena.getName(), server.getName());
            }

            GameMap newGameMap = packet.getGameMap();
            arena.setMap(newGameMap);
        }
    }

    public List<NetworkPlayerLoad.PlayerHolder> getPlayers(int[] playerIds, boolean loadParty) {
        List<NetworkPlayerLoad.PlayerHolder> result = new ArrayList<>(playerIds.length);

        for (int playerId : playerIds) {
            Gamer gamer = Gamer.getGamer(playerId);
            NetworkPlayerLoad.PlayerHolder player;

            if (gamer == null) {
                player = new NetworkPlayerLoad.PlayerHolder(playerId);
            } else {
                NetworkPlayerLoad.PartyHolder playerParty = null;

                if (loadParty) {
                    Party party = gamer.getParty();

                    if (party != null) {
                        playerParty = new NetworkPlayerLoad.PartyHolder(
                                party.getId(),
                                party.isPrivateGame(),
                                party.getLeader().getPlayerId(),
                                party.getPlayerIds()
                        );
                    }
                }

                val currentlyConnecting = gamer.getCurrentlyConnectingServerName();

                val serverName = currentlyConnecting == null
                        ? gamer.getCurrentServerName()
                        : currentlyConnecting;

                player = new NetworkPlayerLoad.PlayerHolder(
                        gamer.isOnline(),
                        playerId, playerParty, serverName
                );
            }

            result.add(player);
        }

        return result;
    }

    @Override
    public void process(CNetworkPlayerLoadRequest packet, ChannelProcessorContext ctx) {
        ctx.setResponse(new SNetworkPlayerLoadResponse(getPlayers(packet.getPlayerIds(), packet.isLoadParty())));
    }

    @Override
    public void process(CGetBestServer packet, ChannelProcessorContext ctx) {
        GetBestServerEvent event = new GetBestServerEvent(packet.getName(), packet.getAddress());
        CoreApplication.getInstance().getPluginManager().callEvent(event);

        AbstractServer server = event.getBestServer() == null
                ? serverManager.getBestByOnline(packet.getMode(), packet.isMax())
                : event.getBestServer();

        ctx.setResponse(new SBestServer(server == null ? null : server.getName()));
    }

    @Override
    public void process(CPlayerNickGenerate packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        ctx.setResponse(new SPlayerNickGenerateResponse(
                gamer == null ? Collections.emptySet() : NickManager.IMP.fetchRandomNicknames(gamer)
        ));
    }

    @Override
    public void process(CChangePlayerNick packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            ctx.setResponse(new SChangePlayerNickResponse(SChangePlayerNickResponse.ChangeNickStatus.USER_NOT_FOUND));
            return;
        }

        if (packet.isNickReset()) {
            gamer.resetPlayerNick();
            return;
        }

        String nick = packet.getNick();

        SChangePlayerNickResponse.ChangeNickStatus status = null;

        if (NetworkSqlHandler.INSTANCE.getPlayerId(nick) != -1) {
            status = SChangePlayerNickResponse.ChangeNickStatus.ALREADY_REGISTERED;
        } else if (!NickSqlHandler.INSTANCE.checkNicknameAvailability(nick).join()) {
            status = SChangePlayerNickResponse.ChangeNickStatus.ALREADY_USED;
        }

        if (status != null) {
            ctx.setResponse(new SChangePlayerNickResponse(status));
            return;
        }

        ctx.addLatch();

        if (gamer.isNicked()) {
            NickSqlHandler.INSTANCE.clearPlayerNickname(gamer.getFakeNickname());
        }

        val rank = CoreApplication.getInstance().getRanksService()
                .findRankById(packet.getGroupId())
                .orElse(CoreApplication.getInstance().getRanksService().defaultRank());

        NickSqlHandler.INSTANCE.changePlayerNickname(gamer.getPlayerId(), nick, rank)
                .whenComplete((playerNick, cause) -> {
                    if (playerNick != null) {
                        gamer.changePlayerNick(playerNick);

                        FakeNickname nickname = playerNick.getNickname();

                        ctx.setResponse(new SChangePlayerNickResponse(
                                SChangePlayerNickResponse.ChangeNickStatus.SUCCESS,
                                nickname.getNickId(), nickname.getName()
                        ));
                    }

                    ctx.removeLatch();
                });
    }

    @Override
    public void process(BChat packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getReceiver());

        if (gamer == null) {
            return;
        }

        gamer.sendToProxy(new BChat(packet.getReceiver(), packet.getMessage()));
    }

    @Override
    public void process(BBroadcastChat packet, ChannelProcessorContext ctx) {
        serverManager.broadcastChat(packet.getMessage());
    }

    @Override
    public void process(BAddServerJoinMessage packet, ChannelProcessorContext ctx) {
        int id = packet.getJoinMessageId();

        String name = packet.getName();
        String text = packet.getText();
        int cost = packet.getCost();

        boolean freeForElite = packet.isAvailableForElite();
        boolean custom = packet.isCustom();

        JoinMessageSqlHandler.INSTANCE
                .addServerJoinMessage(new JoinMessage(id, name, text, cost, freeForElite, custom));

        // клиент сам добавил сообщение о входе
        // возможно игрок создал для себя кастомное
        // отправляем его на все остальные сервера
        for (AbstractServer server : ServerManager.IMP.getServers()) {
            if (server == this.server) {
                continue;
            }

            server.sendPacket(new BAddServerJoinMessage(id, name, text, cost, freeForElite, custom));
        }
    }

    @Override
    public void process(CConnectExact packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.GAMER_NOT_FOUND));
            return;
        }

        AbstractServer server = ServerManager.IMP.getServer(packet.getServer());

        if (server == null || !server.canJoin()) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.SERVER_NOT_FOUND));
            return;
        }

        AbstractServer targetServer = server;

        gamer.sendTo(targetServer).whenComplete((connectStatus, throwable) -> {
            if (throwable != null) {
                log.error("Unable to send gamer {} to {}", gamer.getName(), targetServer.getName());
                ctx.setResponse(new SConnectResponse(ConnectStatus.FAIL));
                return;
            }

            ctx.setResponse(new SConnectResponse(connectStatus));
        });
    }

    @Override
    public void process(CConnectToPlayer packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());
        Gamer target = Gamer.getGamer(packet.getTargetId());

        if (gamer == null || target == null) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.GAMER_NOT_FOUND));
            return;
        }

        AbstractServer server = target.getServer();

        if (server == null) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.SERVER_NOT_FOUND));
            return;
        }

        gamer.sendTo(server)
                .whenComplete((connectStatus, throwable) -> {
                    if (throwable != null) {
                        log.error("Unable to send gamer {} to {}", gamer.getName(), server.getName());
                        ctx.setResponse(new SConnectResponse(ConnectStatus.FAIL));
                        return;
                    }

                    ctx.setResponse(new SConnectResponse(connectStatus));
                });
    }

    @Override
    public void process(CQueueCreate packet, ChannelProcessorContext ctx) {
        GameQueueManager.INSTANCE.register(server, packet.getMini(), packet.getTrigger());
    }

    @Override
    public void process(CQueueQuitQueue packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            log.error("An error occurred while removing a player from the queue: Gamer not found: {}",
                    getDisplayName());

            return;
        }

        gamer.sendLocalizedMessage("queue_leave");

        GameQueueManager.INSTANCE.quitQueue(gamer);
    }

    @Override
    public void process(CNetworkBoosterRequest packet, ChannelProcessorContext ctx) {
        ctx.setResponse(new SNetworkBoosterResponse(
                NetworkBoosterManager.IMP.toArray(NetworkBooster[]::new)
        ));
    }

    @Override
    public void process(CActivateNetworkBooster packet, ChannelProcessorContext ctx) {
        val booster = packet.getBooster();

        NetworkBoosterManager.IMP.add(packet.getBooster());

        val owner = Gamer.getOfflineGamer(booster.getOwner(), loadedPlayer -> {
            loadedPlayer.loadPlayerName();
            loadedPlayer.loadRanks();
            loadedPlayer.loadPrefix();
        });

        assert owner != null;

        val buf = new ObjectLinkedBuf("%player%", owner.getDisplayName(), "%booster%", booster.toString());

        ServerManager.IMP.broadcastStaff("command.staffchat",
                new LanguageContext("boosters_player_activated", __ -> buf));
    }

    @Override
    public void process(CDispatchCommand packet, ChannelProcessorContext ctx) {
        String server = packet.getServer();
        String bukkitCommand = packet.getBukkitCommand();

        AbstractConnectable connectable = serverManager.getServer(server);

        if (connectable == null) {
            connectable = serverManager.getProxy(serverName);
        }

        if (connectable != null) {
            connectable.sendPacket(new SServerCommand(bukkitCommand));
        }
    }

    @Override
    public void process(CCreateParty packet, ChannelProcessorContext ctx) {
        Gamer leader = Gamer.getGamer(packet.getLeaderId());

        if (leader == null || !leader.isOnline()) {
            return;
        }

        Party party = leader.getParty();
        if (party == null) {
            party = new Party(leader);

            if (!party.isValid()) {
                party.setValid(true);

                leader.setParty(party);

                party.sendToServers(new SPartyChange(party.getId(), leader.getPlayerId(), true, party.isPrivateGame()));
            }
        }
    }

    @Override
    public void process(CAddPartyMember packet, ChannelProcessorContext ctx) {
        Gamer leader = Gamer.getGamer(packet.getLeaderId());
        Gamer member = Gamer.getGamer(packet.getPlayerId());

        if (leader == null || !leader.isOnline() || member == null || !member.isOnline()) {
            return;
        }

        Party party = leader.getParty();
        if (party == null) {
            party = new Party(leader);
        }

        party.addMember(member);
        member.setParty(party);
    }

    @Override
    public void process(CRemovePartyMember packet, ChannelProcessorContext ctx) {
        Gamer leader = Gamer.getGamer(packet.getLeaderId());
        Gamer member = Gamer.getGamer(packet.getPlayerId());

        if (leader == null || !leader.isOnline() || member == null || !member.isOnline()) {
            return;
        }

        Party party = leader.getParty();
        if (party != null) {
            party.removeMember(member);
            party.removeOffline(member);
        }
    }

    @Override
    public void process(CDisbandParty packet, ChannelProcessorContext ctx) {
        Gamer leader = Gamer.getGamer(packet.getLeaderId());

        if (leader == null || !leader.isOnline()) {
            return;
        }

        Party party = leader.getParty();
        PartyManager manager = PartyManager.INSTANCE;
        if (party != null) {
            manager.removeParty(party);
        }
    }

    @Override
    public void process(CListPartyMembers packet, ChannelProcessorContext ctx) {
        PartyManager partyManager = PartyManager.INSTANCE;
        Party party = partyManager.getParty(packet.getPartyId());

        if (party == null) {
            ctx.setResponse(new SPartyMemberResponse(false, "Failed to get party members, party == null", Collections.emptyList(), -1));
            return;
        }

        List<Gamer> members = new ArrayList<>(party.getGamers());

        if (members.isEmpty()) {
            ctx.setResponse(new SPartyMemberResponse(false, "Failed to get party members, members isEmpty", Collections.emptyList(), -1));
            return;
        }

        members.remove(party.getLeader());

        List<Integer> idMembers = new ArrayList<>();
        members.forEach(member -> idMembers.add(member.getPlayerId()));

        ctx.setResponse(new SPartyMemberResponse(true, "Done", idMembers, party.getLeader().getPlayerId()));
    }

    @Override
    public void process(BQueueJoinQueue packet, ChannelProcessorContext ctx) {
        GameQueue queue = GameQueueManager.INSTANCE.getQueue(packet.getMini());

        if (queue == null) {
            log.error("Unable to find queue on server {} for mode {}",
                    getDisplayName(), packet.getMini());

            return;
        }

        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            log.error("An error occurred while adding a player to the queue on server {} for mode {}: Gamer not found",
                    getDisplayName(), packet.getMini());

            return;
        }

        // На всякий удалим из очереди, мб он уже где-то был
        GameQueueManager.INSTANCE.quitQueue(gamer);
        queue.addUser(gamer);
    }

    @Override
    public void process(CConnectMode packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.GAMER_NOT_FOUND));
            return;
        }

        gamer.sendTo(packet.getModes())
                .whenComplete((connectStatus, throwable) -> {
                    if (throwable != null) {
                        log.error("Unable to send gamer {} to {}", gamer.getName(), server.getName());
                        ctx.setResponse(new SConnectResponse(ConnectStatus.FAIL));
                        return;
                    }

                    ctx.setResponse(new SConnectResponse(connectStatus));
                });
    }

    @Override
    public void process(CConnectMap packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            ctx.setResponse(new SConnectResponse(ConnectStatus.GAMER_NOT_FOUND));
            return;
        }

        Mini mini = packet.getMini();
        String map = packet.getMap();

        //вызываем эвент, чтобы он нам вернул игроков которых надо тпшить, а следовательно и кол-во места
        PlayerGameRedirectEvent redirectEvent = new PlayerGameRedirectEvent(gamer);
        core.getPluginManager().callEvent(redirectEvent);

        Set<Gamer> redirectedGamers = redirectEvent.getAdditionalGamerList();

        //необходимое пространство
        int space = redirectedGamers.size() + 1;

        // ищем сервер
        AbstractServer server = serverManager.getBestWithMap(mini, map, space);

        if (server == null) {
            gamer.sendLocalizedMessage("core_no_servers_available_by_map");
            ctx.setResponse(new SConnectResponse(ConnectStatus.SERVER_NOT_FOUND));
            return;
        }

        for (Gamer target : redirectEvent.getAdditionalGamerList()) {
            target.sendTo(server);
        }

        gamer.sendTo(server)
                .whenComplete((connectStatus, throwable) -> {
                    if (throwable != null) {
                        log.error("Unable to send gamer {} to {}", gamer.getName(), server.getName());
                        ctx.setResponse(new SConnectResponse(ConnectStatus.FAIL));
                        return;
                    }

                    ctx.setResponse(new SConnectResponse(connectStatus));
                });
    }

    @Override
    public void process(CServerConnectRequest packet, ChannelProcessorContext ctx) {
        String serverName = packet.getTarget();
        boolean cancelled = false;

        try {
            Gamer gamer = Gamer.getGamer(packet.getPlayerId());

            if (gamer == null) {
                return;
            }

            AbstractServer target = ServerManager.IMP.getServer(serverName);

            if (target == null) {
                AbstractServer server = ServerManager.IMP.getBestByOnline(Lobby.HUB, false);

                if (server == null) {
                    server = ServerManager.IMP.getBestByOnline(Special.LIMBO, false);
                }

                serverName = server.getName();
                return;
            }

            PlayerRedirectEvent event = new PlayerRedirectEvent(gamer, target);
            core.getPluginManager().callEvent(event);

            cancelled = event.isCancelled();
            serverName = event.getTarget().getName();
        } finally {
            ctx.setResponse(new SServerConnectResponse(cancelled, serverName));
        }
    }

    private PlayerLoginEvent callLoginEvent(String name, InetAddress address, PlayerLoginEvent.State state) {
        return core.getPluginManager().callEvent(new PlayerLoginEvent(name, address, state));
    }

    private void asyncReconnectPlayers(CReconnectPlayers packet) {
        Proxy proxy = (Proxy) server;

        log.info("Asynchronously reconnecting {} players after restart...", packet.getGamers().size());

        Map<Gamer, CompletableFuture<AbstractServer>> reconnecting = new HashMap<>();
        Set<FailedReconnectGamer> gamers = new HashSet<>();

        for (ReconnectingGamer reconnectingGamer : packet.getGamers()) {
            String name = reconnectingGamer.getName();
            InetAddress address = reconnectingGamer.getAddress();
            String serverName = reconnectingGamer.getServer();

            Language language;
            if (core.getGeoLocationManager() != null) {
                language = LanguageUtil.getLanguage(core.getGeoLocationManager(), address);
            } else {
                language = Language.defaultLanguage();
            }

            Gamer target = Gamer.getGamer(name);

            if (target != null && target.isOnline()) {
                gamers.add(new FailedReconnectGamer(name, language.getMessage("proxy_already_connect")));
                continue;
            }

            PlayerLoginEvent playerLoginEvent = callLoginEvent(name, address, PlayerLoginEvent.State.RECONNECT);

            if (playerLoginEvent.getKickReason() != null) {
                gamers.add(new FailedReconnectGamer(name, playerLoginEvent.getKickReason()));
                continue;
            }

            Gamer gamer = new Gamer(reconnectingGamer.getName(), reconnectingGamer.getAddress());
            gamer.loadPlayerId();

            if (gamer.getPlayerId() < 0) {
                gamers.add(new FailedReconnectGamer(name, language.getMessage("proxy_player_not_exist")));
                continue;
            }

            gamer.load();
            gamer.setDomain(reconnectingGamer.getDomain());
            gamer.setProtocol(reconnectingGamer.getProtocol());

            gamer.setJustJoined(false);

            gamer.setProxy(proxy);

            // Listening and loading all the connection in GamerLoadEvent
            GamerLoadEvent event = new GamerLoadEvent(gamer);
            core.getPluginManager().callEvent(event);

            Gamer.addGamer(gamer);

            try {
                reconnecting.put(
                        gamer,

                        ServerManager.IMP.getFutureServer(serverName).whenComplete((result, e) -> {
                            if (result == null || e != null) {
                                gamer.localizedKick("core_reconnect_server_not_found");
                            } else {

                                gamer.setServer(result);
                                gamer.sendTitle(
                                        language.getMessage("core_reconnect_success_title"),
                                        language.getMessage("core_reconnect_success_subtitle")
                                );
                            }
                        })
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            core.getPluginManager().callEvent(new GamerJoinEvent(gamer));
        }

        if (!gamers.isEmpty()) {
            sendPacket(new SFailedReconnectPlayers(gamers));
        }

        if (!reconnecting.isEmpty()) {
            for (CompletableFuture<AbstractServer> server : reconnecting.values()) {
                if (server.isDone()) {
                    continue;
                }

                try {
                    server.get(10000, TimeUnit.MILLISECONDS);
                } catch (Throwable t) {
                    server.completeExceptionally(t);
                }
            }
        }
    }

    @Override
    public void process(CReconnectPlayers packet, ChannelProcessorContext ctx) {
        ctx.addLatch();

        Async.run(() -> {
            asyncReconnectPlayers(packet);
            ctx.removeLatch();
        });
    }

    private void asyncProxyPlayerLogin(CProxyPlayerLoginRequest packet, ChannelProcessorContext ctx) {
        log.info("Attempting to log in player {} on proxy {}", packet.getName(), getDisplayName());

        boolean cancelled = false;
        String reason = null;
        String name = packet.getName();

        try {
            InetAddress address = packet.getAddress();

            Language language;
            if (core.getGeoLocationManager() != null) {
                language = LanguageUtil.getLanguage(core.getGeoLocationManager(), address);
            } else {
                language = Language.defaultLanguage();
            }

            if (packet.contains(CProxyPlayerLoginRequest.VERIFY)) {
                List<Gamer> gamers = Gamer.getGamersByIp(address);
                gamers.removeIf(player -> player.getRanks().hasPermission("core.bypass_ip_limit"));

                if (gamers.size() >= 5) {
                    cancelled = true;
                    reason = language.getMessage("proxy_max_accounts");

                    log.info("Canceled {} login attempt from IP {}: exceeded the maximum number of accounts ({}).",
                            name, address.getHostAddress(), gamers.size());

                    return;
                }

                PlayerLoginEvent playerLoginEvent = callLoginEvent(name, address, PlayerLoginEvent.State.VERIFY);

                if (playerLoginEvent.isCancelled()) {
                    cancelled = true;
                    reason = playerLoginEvent.getKickReason();

                    return;
                }
            }

            if (packet.contains(CProxyPlayerLoginRequest.FINISH)) {
                if (address == null) {
                    cancelled = true;
                    reason = "§cFailed to get address [CORE]";
                    log.error("Player {} FINISH login failed: Address is null", name);

                    return;
                }

                log.info("Processing FINISH login for player {} from {}. Domain: {}, Protocol: {}, Target Server: {}",
                        name, address.getHostAddress(), packet.getDomain(), packet.getProtocol(), packet.getServerName());

                PlayerLoginEvent playerLoginEvent = callLoginEvent(name, address, PlayerLoginEvent.State.POST);

                if (playerLoginEvent.isCancelled()) {
                    cancelled = true;
                    reason = playerLoginEvent.getKickReason();
                    log.warn("PlayerLoginEvent (POST) cancelled for player {}. Reason: {}", name, reason);

                    return;
                }

                Proxy proxy = (Proxy) server;

                Gamer gamer = new Gamer(name, address);
                gamer.loadPlayerId();

                if (gamer.getPlayerId() < 0) {
                    cancelled = true;
                    reason = language.getMessage("proxy_failed_load_data") + "[CORE]";
                    log.error("Failed to load player ID for {} during FINISH login", name);
                    removeFromCore(gamer);

                    return;
                }


                gamer.load();

                gamer.setDomain(packet.getDomain());
                gamer.setProtocol(packet.getProtocol());

                log.info("Gamer {} data loaded. Calling GamerLoadEvent.", name);
                GamerLoadEvent event = new GamerLoadEvent(gamer);
                core.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    cancelled = true;
                    reason = event.getCancelReason();
                    log.warn("GamerLoadEvent cancelled for player {}. Reason: {}", name, reason);
                    removeFromCore(gamer);

                    return;
                }

                Gamer offline = Gamer.getOfflineGamer(name);

                if (offline != null) {
                    log.info("Invalidating offline gamer object for {}", name);
                    Gamer.invalidateOfflinePlayer(offline);
                }

                log.info("Adding gamer {} to online list on Core.", name);
                Gamer.addGamer(gamer);

                gamer.setProxy(proxy);

                String newServerName = packet.getServerName();

                if (!newServerName.isEmpty()) {
                    AbstractServer newServer = ServerManager.IMP.getServer(newServerName);

                    if (newServer != null) {
                        log.info("Setting server for gamer {} to {}", name, newServerName);
                        gamer.setServer(newServer);
                    } else {
                        log.warn("Gamer {} join but new server {} is null", name, newServerName);
                        cancelled = true;
                        reason = language.getMessage("proxy_login_server_not_found") + "[Core/PLF]";
                        removeFromCore(gamer);
                    }
                }
            }
        } finally {
            if (cancelled) {
                log.warn("Canceled login attempt for player {}: {}", name, reason);

                PENDING_LOGINS.remove(name);

                Gamer gamer = Gamer.getGamer(name);

                if (gamer != null) {
                    removeFromCore(gamer);
                }
            } else {
                log.info("Player {} logged in successfully.", name);
            }

            ctx.setResponse(new SProxyPlayerLoginResponse(cancelled, reason));
        }
    }


    @Override
    public void process(CProxyPlayerLoginRequest packet, ChannelProcessorContext ctx) {
        val name = packet.getName().toLowerCase();
        val gamer = Gamer.getGamer(name);

        Language adressLanguage;

        if (core.getGeoLocationManager() != null) {
            adressLanguage = LanguageUtil.getLanguage(core.getGeoLocationManager(), packet.getAddress());
        } else {
            adressLanguage = Language.defaultLanguage();
        }

        if ((gamer != null && gamer.isOnline()) || (!packet.contains(CProxyPlayerLoginRequest.FINISH) && !PENDING_LOGINS.add(name))) {
            log.warn("Player {} login attempt rejected. Already online: {}, pending login: {}, proxy: {}",
                    name, (gamer != null && gamer.isOnline()), PENDING_LOGINS.contains(name), getDisplayName());

            ctx.setResponse(new SProxyPlayerLoginResponse(true,
                    adressLanguage.getMessage("proxy_previous_session")));
            return;
        }


        ctx.addLatch();

        try {
            asyncProxyPlayerLogin(packet, ctx);
        } catch (Exception exception) {
            log.error("An error occurred while player logging in on proxy {}", getDisplayName(), exception);

            PENDING_LOGINS.remove(name);
            ctx.setResponse(new SProxyPlayerLoginResponse(true, adressLanguage.getMessage("internal_error")));
        } finally {
            ctx.removeLatch();

            if (packet.contains(CProxyPlayerLoginRequest.FINISH)) {
                PENDING_LOGINS.remove(name);
            } else if (PENDING_LOGINS.contains(name)) {
                CoreApplication.getInstance().getScheduler().schedule(() -> {
                    if (PENDING_LOGINS.remove(name)) {
                        log.warn("Login timeout for player {} on proxy {}", name, getDisplayName());
                    }
                }, 35, TimeUnit.SECONDS);
            }
        }

    }

    @Override
    public void process(CProxyPlayerDisconnect packet, ChannelProcessorContext ctx) {
        if (PENDING_LOGINS.remove(packet.getPlayerName().toLowerCase())) {
            log.warn("Player {} disconnect, but is pending", packet.getPlayerName());
        }

        Gamer gamer = Gamer.getGamer(packet.getPlayerId());
        if (gamer != null) {
            gamer.quit(false);
        }

        log.info("Player {} disconnected from {}", gamer, getDisplayName());
    }

    @Override
    public void process(BProxyKick packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            gamer.sendToProxy(packet);
        }
    }

    @Override
    public void process(CPlayerCommand packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            core.executeCommand(gamer, packet.getLine());
        }
    }

    @Override
    public void process(CSendServerCommand packet, ChannelProcessorContext ctx) {
        boolean isExact = packet.isExact();
        String line = packet.getCommand();

        if (isExact) {
            String id = packet.getName();

            AbstractConnectable server = serverManager.getServer(id);

            if (server == null) {
                server = serverManager.getProxy(id);
            }

            if (server != null) {
                server.sendPacket(new SServerCommand(line));
            }
        } else {
            try {
                SServerCommand broadcastPacket = new SServerCommand(line);

                for (AbstractConnectable server : serverManager.findServers(packet.getName())) {
                    server.sendPacket(broadcastPacket);
                }
            } catch (Exception e) {
                log.error("Invalid pattern in {}", packet);
            }
        }
    }

    @Override
    public void process(BUserRankRefreshPacket packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        //обновляем группу игрока, игрок должен быть онлайн
        if (gamer == null) {
            return;
        }

        gamer.loadRanks();

        if (server instanceof Proxy) {
            gamer.sendToServer(packet);
        } else {
            gamer.sendToProxy(packet);
        }

        gamer.sendLocalizedMessage("core_rank_updated");
    }

    @Override
    public void process(CTabcompleteRequest packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        List<String> suggests = core.tabComplete(gamer, packet.getLine());

        if (suggests == null) {
            return;
        }

        ctx.setResponse(new STabcompleteResponse(suggests));
    }

    @Override
    public void process(BSetProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.updateProperty(packet.getProperty(), packet.getValue());

        if (server instanceof AbstractServer) {
            gamer.sendToProxy(packet);
        } else {
            gamer.sendToServer(packet);
        }
    }

    @Override
    public void process(BRemoveProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.dropProperty(packet.getProperty());

        if (server instanceof AbstractServer) {
            gamer.sendToProxy(packet);
        } else {
            gamer.sendToServer(packet);
        }
    }

    @Override
    public void process(CLoadFriends packet, ChannelProcessorContext ctx) {
        //немножко оптимизации. это пакет, чтобы получить сразу всех онлайн друзей игрока,
        //а не отправлять запрос GET_PLAYER на каждого;
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            ctx.setResponse(new SNetworkPlayerLoadResponse(Collections.emptyList()));
            return;
        }

        ctx.setResponse(new SNetworkPlayerLoadResponse(getPlayers(gamer.getFriends().toArray(), false)));
    }

    @Override
    public void process(BUpdatePlayerLanguage packet, ChannelProcessorContext ctx) {
        int playerId = packet.getPlayerId();
        Gamer gamer = Gamer.getGamer(playerId);

        if (gamer == null) {
            return;
        }

        Language language = Language.getById(packet.getLanguageId());

        // меняем игроку язык
        gamer.setLanguage(language);

        // обновляем язык в базе данных
        NetworkSqlHandler.INSTANCE.setPlayerLanguage(gamer.getPlayerId(), language);

        if (server instanceof Proxy) { // пакет об изменении языка пришёл от прокси, значит мы должны обновить его ещё и на баките
            gamer.sendToServer(packet);
        } else { // пакет об изменении пришёл от бакита, значит мы должны обновить его ещё и на прокси
            gamer.sendToProxy(packet);
        }

        gamer.updateTablist();
    }

    @Override
    public void process(CReportCheckExistenceRequest packet, ChannelProcessorContext ctx) {
        ctx.setResponse(new SReportCheckExistenceResponse(ReportManager.INSTANCE.hasReport(packet.getPlayerId())));
    }

    @Override
    public void process(CReportCheckStateRequest packet, ChannelProcessorContext ctx) {
        // отправитель жалобы
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());
        if (gamer == null)
            return;

        // тот на кого жалуются
        Gamer target = Gamer.getGamer(packet.getTargetId());
        if (target == null)
            return;

        ctx.setResponse(new SReportCheckStateResponse(ReportManager.INSTANCE.checkReportSendState(gamer, target)));
    }

    @Override
    public void process(CReportAction packet, ChannelProcessorContext ctx) {
        // отправитель жалобы
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        // действие в репорте
        CReportAction.Action action = packet.getAction();

        // тот на кого жалуются
        Gamer target = Gamer.getGamer(packet.getTargetId());
        if (target == null)
            return;

        // причина, по которой жалобу одобрять или отклонять
        ReportReason reason = packet.getReason();

        switch (action) {
            case SEND -> ReportManager.INSTANCE.sendReport(gamer, target, reason);
            case CLOSE -> ReportManager.INSTANCE.closeReport(gamer, target, reason);
            case START_SPECTATING -> ReportManager.INSTANCE.spectate(gamer, target);
        }
    }

    @Override
    public void process(CGetReports packet, ChannelProcessorContext ctx) {
        Set<PlayerReport> playerReports = new HashSet<>();

        List<Report> reports = ReportManager.INSTANCE.getReports();
        reports.removeIf(report -> !report.getGamer().isOnline()); // TODO: fix?

        reports.forEach(report -> {
            Gamer gamer = report.getGamer();

            Set<PlayerReportSender> senders = report.getSenders().entrySet().stream()
                    .map(entry -> new PlayerReportSender(
                            entry.getKey().getPlayerId(),
                            entry.getValue()
                    ))
                    .collect(Collectors.toSet());

            playerReports.add(new PlayerReport(
                    gamer.getPlayerId(),
                    gamer.getServer() == null ? "" : gamer.getServer().getName(),
                    report.getLastReportTime(),
                    senders
            ));
        });

        ctx.setResponse(new SReports(playerReports));
    }

    @Override
    public void process(CGetActiveStreams packet, ChannelProcessorContext ctx) {
        Set<Stream> streams = new HashSet<>();

        StreamManager.INSTANCE.getActiveStreams().forEachEntry((playerId, details) -> {
            if (!details.isActual()) {
                return true;
            }

            String formattedTitle = details.getTitle();

            // обрезаем оч длинные заголовки
            if (formattedTitle.length() > 32) {
                formattedTitle = formattedTitle.substring(0, 32);
            }

            streams.add(new Stream(
                    playerId,
                    details.getStreamPlatform().getDisplayName(),
                    formattedTitle,
                    details.getViewers(),
                    details.getStartedAtServiceTime(),
                    details.getStreamPlatform().makeBeautifulUrl(details))
            );

            return true;
        });

        ctx.setResponse(new SActiveStreams(streams));
    }

    @Override
    public void process(BBestPlayerDonate packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getOfflineGamer(packet.getPlayerId(), loadedPlayer -> {
        });

        Gamer donater = Gamer.getOfflineGamer(packet.getDonaterId(), loadedPlayer -> {
            loadedPlayer.loadPlayerName();
            loadedPlayer.loadRanks();
            loadedPlayer.loadPrefix();
        });

        int golds = packet.getGolds();

        if (gamer != null && donater != null) {
            gamer.addGold(golds);

            if (gamer.isOnline()) {
                Language language = gamer.getLanguage();

                gamer.sendLocalizedMessage("best_player_donation_received",
                        "%donater%", donater.getDisplayName(),
                        "%format%", CommonWords.GOLD.get(language).withNumber(golds));
            }

            Set<AbstractConnectable> servers = new HashSet<>();

            for (Lobby lobby : Lobby.values()) {
                servers.addAll(serverManager.getServers(lobby));
            }

            for (Uncommon uncommon : Uncommon.values()) {
                servers.addAll(serverManager.getServers(uncommon));
            }

            servers.remove(server);

            for (AbstractConnectable server : servers) {
                server.sendPacket(new BBestPlayerDonate(packet.getPlayerId(), packet.getDonaterId(), packet.getGolds()));
            }
        }
    }

    @Override
    public void process(BBestPlayerUpdate packet, ChannelProcessorContext ctx) {
        Set<AbstractConnectable> servers = new HashSet<>();

        for (Lobby lobby : Lobby.values()) {
            servers.addAll(serverManager.getServers(lobby));
        }

        for (Uncommon uncommon : Uncommon.values()) {
            servers.addAll(serverManager.getServers(uncommon));
        }

        servers.remove(server);

        for (AbstractConnectable server : servers) {
            server.sendPacket(new BBestPlayerUpdate(packet.getPlayerId()));
        }
    }

    @Override
    public void process(CPlayerChangeSkin packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) return;

        SkinsApi.getPlayerSkin(gamer.getPlayerId(), gamer.getName()).whenComplete((skin, throwable) -> {
            if (throwable != null) {
                log.error("Failed update gamer {} skin. Details:", gamer.getName(), throwable);
                return;
            }

            gamer.setSkin(skin);
        });

    }

    @Override
    public void process(BProxyBotCheck packet, ChannelProcessorContext ctx) {
        log.info("User {} successfully passed sonar bot check", packet.getPlayerName());
        for (Proxy proxy : ServerManager.IMP.getProxies()) {
            if (proxy == this.server) {
                continue;
            }


            proxy.sendPacket(packet);
        }
    }

    @Override
    public void process(CServerRestartScheduled packet, @NotNull ChannelProcessorContext ctx) {
        val server = ServerManager.IMP.getServer(packet.getServerName());

        if (server == null) {
            log.warn("Received restart schedule for unknown server: {}", packet.getServerName());
            return;
        }

        server.setServerState(ServerState.SCHEDULED_RESTART);

        log.info("Server {} scheduled to restart in {} seconds", server.getName(), packet.getSecondsUntilRestart());

        ServerManager.IMP.broadcastGroup("command.staffchat", "server_restart_scheduled",
                "%server%", server.getName(),
                "%seconds%", String.valueOf(packet.getSecondsUntilRestart()));
    }

    @Override
    public void process(COnlineFriendsCountRequest cOnlineFriendsCountRequest, @NotNull ChannelProcessorContext ctx) {
        val id = cOnlineFriendsCountRequest.getPlayerId();
        val gamer = Gamer.getOfflineGamer(id, Gamer::load);

        if (gamer == null) {
            throw new RuntimeException("Received online friends request for unknown gamer id#" + id);
        }

        ctx.setResponse(
                new SOnlineFriendsCountResponse(gamer.getOnlineFriends().size())
        );
    }

    @Override
    public void process(BLicenseStatus packet, ChannelProcessorContext ctx) {
        // на коре это не хандлится никак
        // так что передаем это просто другим банжам
        for (val proxy : ServerManager.IMP.getProxies()) {
            if (proxy == server) {
                continue;
            }

            proxy.sendPacket(packet);
        }
    }
}
