package club.abelix.core.velocity.connection;

import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.parser.I18nParserController;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.auth.bans.AuthBanManager;
import club.abelix.core.velocity.event.CorePingRequestEvent;
import club.abelix.core.velocity.event.ServerActiveEvent;
import club.abelix.core.velocity.event.ServerInactiveEvent;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.util.PlayerTryServerConnect;
import club.abelix.core.velocity.util.ServerUtil;
import club.abelix.network.channel.AbstractRemoteServerChannel;
import club.abelix.network.common.enums.ConnectionType;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.PacketRegistry;
import club.abelix.network.packet.handshake.packet.HRequest;
import club.abelix.network.packet.handshake.packet.HResponse;
import club.abelix.network.packet.play.both.*;
import club.abelix.network.packet.play.client.CPlayerPingResponse;
import club.abelix.network.packet.play.client.CLookupGlobalOnline;
import club.abelix.network.packet.play.client.CPingResponse;
import club.abelix.network.packet.play.client.CReconnectPlayers;
import club.abelix.network.packet.play.client.CServerPingResponse;
import club.abelix.network.packet.play.client.CSyncResponse;
import club.abelix.network.packet.play.server.SPlayerPingRequest;
import club.abelix.network.packet.play.server.SFailedReconnectPlayers;
import club.abelix.network.packet.play.server.SMuteChangeMuteState;
import club.abelix.network.packet.play.server.SPingRequest;
import club.abelix.network.packet.play.server.SRefreshRanksPacket;
import club.abelix.network.packet.play.server.SRegisterCommands;
import club.abelix.network.packet.play.server.SRegisterServers;
import club.abelix.network.packet.play.server.SReloadLanguages;
import club.abelix.network.packet.play.server.SSendPlayer;
import club.abelix.network.packet.play.server.SSendTitle;
import club.abelix.network.packet.play.server.SServerCommand;
import club.abelix.network.packet.play.server.SServerPingRequest;
import club.abelix.network.packet.play.server.SSyncRequest;
import club.abelix.network.packet.play.server.STablistHeaderFooter;
import club.abelix.network.packet.play.server.STwoFactorAuthenticationResult;
import club.abelix.network.packet.play.server.SUnmute;
import club.abelix.network.packet.play.server.SUnregisterServer;
import club.abelix.network.type.gamer.FailedReconnectGamer;
import club.abelix.network.type.gamer.ReconnectingGamer;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.server.ServerConnectionInfo;
import club.abelix.network.util.ModeUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import w.util.Env;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RemoteServerConnection extends AbstractRemoteServerChannel {

    private final StarshipVelocity core;

    public RemoteServerConnection(StarshipVelocity core, SocketChannel channel) {
        super(core.getConnection(), channel);

        this.core = core;
    }

    @Override
    public void active() {
        InetSocketAddress inetSocketAddress = core.getProxy().getBoundAddress();

        sendPacket(new HRequest(
                ConnectionType.PROXY,
                Env.getString("CORE_TOKEN", "milanskyy_gay"),
                core.getProxyName(),
                inetSocketAddress.getPort(), null)
        );

        for (Gamer gamer : Gamer.getGamers()) {
            gamer.sendLocalizedMessage("core_reconnect_success_title");
        }
    }

    @Override
    protected void onServerDisconnect() {
        core.getProxy().getEventManager().fireAndForget(new ServerInactiveEvent());
    }

    @Override
    public void process(SServerPingRequest sServerPingRequest, @NotNull ChannelProcessorContext ctx) {
        ctx.setResponse(new CServerPingResponse(sServerPingRequest.getRandom()));

        core.getProxy().getEventManager().fireAndForget(new CorePingRequestEvent());
    }

    @Override
    public void process(HResponse packet, ChannelProcessorContext ctx) {
        if (!packet.isSuccess()) {
            log.error("[HANDSHAKE] Unable to connect to core: {}", packet.getMessage());
            core.getConnection().closeConnection();
            core.getConnection().reconnect();
            return;
        }

        upgradeConnection(PacketRegistry.PLAY);
        reconnectPlayers();

        sendPacket(new CLookupGlobalOnline());
        core.getProxy().getEventManager().fireAndForget(new ServerActiveEvent(this));
        core.getProxy().getScheduler()
                .buildTask(core, () -> core.setJoinable(true))
                .delay(3, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void process(SReloadLanguages packet, ChannelProcessorContext ctx) {
        Language.reloadLanguages();
    }

    @Override
    public void process(STablistHeaderFooter packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        Player player = gamer.getPlayer();

        player.sendPlayerListHeaderAndFooter(
                Component.text(packet.getHeader()),
                Component.text(packet.getFooter())
        );
    }

    @Override
    public void process(SServerCommand packet, ChannelProcessorContext ctx) {
        String command = packet.getCommand();

        core.getProxy().getCommandManager().executeAsync(core.getProxy().getConsoleCommandSource(), command);
    }

    @Override
    public void process(final STwoFactorAuthenticationResult packet, final ChannelProcessorContext ctx) {
        Player player = core.getProxy().getPlayer(packet.getName()).orElse(null);

        if (player == null) {
            log.info("Кор прислал резулт авторизации, а игрок под ником {}, съебался, тварь!", packet.getName());
            return;
        }

        String address = player.getRemoteAddress().getAddress().getHostAddress();

        if (!packet.isAllowLogin()) {
            player.disconnect(Component.text("§cВаш вход был отклонен из VK. IP адрес заблокирован на 10 минут"));
            AuthBanManager.addBannedAddress(address);
            return;
        }

        Gamer gamer = Gamer.getGamer(player);
        gamer.login();

        gamer.sendLocalizedMessage("auth_login_two_factor_allowed");

        core.getAuthManager().afterAuthenticationStage(gamer);
    }


    @Override
    public void process(SSendTitle packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getReceiver());

        if (gamer != null) {
            gamer.sendTitle(packet.getTitle(), packet.getSubtitle(), packet.getFadeIn() / 20, packet.getStay() / 20, packet.getFadeOut() / 20);
        }
    }

    @Override
    public void process(SFailedReconnectPlayers packet, ChannelProcessorContext ctx) {
        for (FailedReconnectGamer gamer : packet.getGamers()) {
            val player = core.getProxy().getPlayer(gamer.getName()).orElse(null);

            if (player == null) {
                continue;
            }

            player.disconnect(Component.text(gamer.getReason()));
        }
    }

    private void reconnectPlayers() {
        val gamers = Gamer.getGamers();
        gamers.removeIf(gamer -> !gamer.isOnline());

        if (gamers.isEmpty()) {
            return;
        }

        log.info("[RECONNECT] Trying to reconnect {} players", gamers.size());

        val reconnectingGamers = new HashSet<ReconnectingGamer>();

        for (val gamer : gamers) {
            val player = gamer.getPlayer();

            val name = gamer.getName();
            val address = player.getRemoteAddress().getAddress();

            val server = player.getCurrentServer().orElse(null);

            if (server == null) {
                Logging.IMP.error("Unable to find server for players {}, kicking...", name);
                player.disconnect(Component.text("Error! Couldn't find a server to connect :("));
                continue;
            }

            val serverName = server.getServerInfo().getName();

            reconnectingGamers.add(new ReconnectingGamer(name, address, gamer.getDomain(), gamer.getProtocol(), serverName));
        }

        sendPacket(new CReconnectPlayers(reconnectingGamers));
    }

    @Override
    public void process(BProxyKick packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null && gamer.isOnline()) {
            gamer.getPlayer().disconnect(Component.text(packet.getReason()));
        }
    }

    @Override
    public void process(BLicenseStatus packet, ChannelProcessorContext ctx) {
        core.getAuthManager()._setCheckingLicenseStatus(packet.getName(), packet.isStatus());
    }

    @Override
    public void process(SMuteChangeMuteState packet, ChannelProcessorContext ctx) {
        //добавляем мут на банжу, чтобы у игрока с этим ID отменялись чат-эвенты
        MuteMeta muteMeta = new MuteMeta(
                packet.getMuteId(), packet.getPlayerId(), packet.getAdmin(),
                packet.getIssueDate(), packet.getExpiration(), packet.getReason()
        );

        core.getPunishmentsManager().addActiveMute(muteMeta);
    }

    @Override
    public void process(BBroadcastChat packet, ChannelProcessorContext ctx) {
        core.getProxy().getScheduler().buildTask(core, () -> {
            String msg = packet.getMessage();
            String escapedMessage = msg.trim();

            Component comp = (escapedMessage.startsWith("{") && escapedMessage.endsWith("}")
                    || (escapedMessage.startsWith("[") && escapedMessage.endsWith("]")))
                    ? GsonComponentSerializer.gson().deserialize(msg)
                    : Component.text(msg);

            core.getProxy().getAllPlayers().forEach(player -> player.sendMessage(comp));
        }).schedule();
    }

    @Override
    public void process(BBroadcastChatLocalized packet, ChannelProcessorContext ctx) {
        core.getProxy().getScheduler().buildTask(core, () -> {
            // Создаем мапу для кеширования сообщений на разных языках
            Map<Language, Component> languageCacheMap = new HashMap<>();

            // Итерируемся по всем игрокам
            for (Gamer gamer : Gamer.getGamers()) {
                Language language = gamer.getLanguage();

                // Если передан ключевой язык, и язык игрока не соответствует ключевому языку, пропускаем игрока
                if (packet.getFocusLanguageId() > 0 && language.getId() != packet.getFocusLanguageId()) {
                    continue;
                }

                // Если сообщение на языке этого игрока уже есть в мапе - отправляем его оттуда
                if (languageCacheMap.containsKey(language)) {
                    gamer.sendMessage(languageCacheMap.get(language));
                    continue;
                }

                // Если нет, то парсим сообщение
                String msg = gamer.getLanguage().getMessage(packet.getKey(), true, packet.getObjects());
                String escapedMessage = msg.trim();

                Component comp = (escapedMessage.startsWith("{") && escapedMessage.endsWith("}"))
                        || (escapedMessage.startsWith("[") && escapedMessage.endsWith("]"))
                        ? GsonComponentSerializer.gson().deserialize(msg)
                        : Component.text(msg);

                gamer.sendMessage(comp);
                languageCacheMap.put(language, comp);
            }
        }).schedule();
    }

//    @Override
//    public void process(BPlayerSuccessBotCheck packet, ChannelProcessorContext ctx) {
//        OfflineProxyPlayer offlineProxyPlayer = new OfflineProxyPlayer(packet.getUsername(), packet.getIp());
//        core.getFilter().cacheFilterUser(offlineProxyPlayer);
//        Logging.IMP.info("[Bot] add user {} to check cache", offlineProxyPlayer.getUsername());
//    }

    @Override
    public void process(BChat packet, ChannelProcessorContext ctx) {
        val gamer = Gamer.getGamer(packet.getReceiver());

        if (gamer == null) return;

        val message = packet.getMessage();
        val escapedMessage = message.trim();

        if (escapedMessage.startsWith("lang")) {
            val parsed = I18nParserController.getInstance().parse(gamer.getLanguage(), message);
            gamer.sendMessage(parsed);
            return;
        }

        val component = (escapedMessage.startsWith("{") && escapedMessage.endsWith("}"))
                || (escapedMessage.startsWith("[") && escapedMessage.endsWith("]"))
                ? GsonComponentSerializer.gson().deserialize(message)
                : Component.text(message);

        gamer.sendMessage(component);
    }

    @Override
    public void process(SSendPlayer packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getOrCreate(packet.getPlayerId(), offline -> {
            offline.loadAuthData();
            offline.loadLanguage();

            Player player = core.getProxy().getPlayer(offline.getName()).orElse(null);

            if (player == null) {
                throw new RuntimeException("Core has sent a task to move the player, but the player is not found on the proxy!");
            }

            offline.setPlayer(player);
        });

        if (gamer == null) {
            return;
        }

        List<RegisteredServer> targetServersList = Arrays.stream(packet.getServers())
                .map(server -> core.getProxy().getServer(server).orElse(null))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (targetServersList.size() <= 1 && packet.getServers().length == 1) {
            String targetName = packet.getServers()[0];
            ServerMode mode = ModeUtil.getServerModeByName(targetName);
            if (mode.isUncommon()) {
                if (!targetServersList.isEmpty()) {
                    targetServersList.addAll(ServerUtil.getServerList(mode, Set.of(targetServersList.get(0))));
                } else {
                    targetServersList.addAll(ServerUtil.getServerList(mode));
                }
            }
        }

        if (targetServersList.isEmpty()) {
            Logging.IMP.info("Игрок {} попытался подключиться к серверу, но список пуст. Вот досада...", gamer.getName());
            gamer.sendFailedConnect(gamer.getLanguage().getMessage("gamer_server_connect_failed_reason_target_null"));
            return;
        }

        // Подключение по списку серверов, работает крайне плохо и
        // сильно грузит, поэтому кидаем на рандомный из списка
        new PlayerTryServerConnect(gamer, targetServersList).connect().thenAccept(success -> {
            if (!success) {
                gamer.sendFailedConnect(gamer.getLanguage().getMessage("gamer_server_connect_failed_reason_unknown"));
            }
        });

//        val sendUtil = new PlayerTryServerConnect(gamer, targetServersList);
//        sendUtil.connect().thenAcceptAsync(status -> {
//            if (!status) {
//                Logging.IMP.warn("[{}] Не удалось подключится к серверу!", gamer.getName());
//                Logging.IMP.info("[Connect] Не получилось подсоединить игрока к серверу, список: {}", targetServersList);
//                gamer.sendFailedConnect();
//            }
//            // Всё удачно, ничего отправлять не надо, тк при подключении игрока к серверу, кор сам снимет ограничение
//        });

    }

    @Override
    public void process(SUnregisterServer packet, ChannelProcessorContext ctx) {
        val name = packet.getServerName();

        RegisteredServer findServer = core.getProxy().getServer(name).orElse(null);

        if (findServer == null) {
            log.warn("Кор прислал запрос на удаление сервера " + name + ", но он не найден!");
            return;
        }
        core.getProxy().unregisterServer(findServer.getServerInfo());

        log.info("Сервер " + name + " был удалён из списка серверов");
    }

    @Override
    public void process(SRegisterServers packet, ChannelProcessorContext ctx) {
        for (ServerConnectionInfo info : packet.getServers()) {
            String name = info.getServerName();
            String ip = info.getServerAddress();

            int port = info.getServerPort();
            RegisteredServer alreadyRegistered = core.getProxy().getServer(name).orElse(null);

            if (alreadyRegistered != null) {
                log.info("Сервер " + name + " уже существует, удаляем его");
                core.getProxy().unregisterServer(alreadyRegistered.getServerInfo());
            }

            core.getProxy().registerServer(new ServerInfo(name, new InetSocketAddress(ip, port)));
            log.info("Сервер " + name + " был добавлен в список серверов");
        }
    }

    @Override
    public void process(BSetProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.updateProperty(packet.getProperty(), packet.getValue());
    }

    @Override
    public void process(BRemoveProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.dropProperty(packet.getProperty());
    }

    @Override
    public void process(BUserRankRefreshPacket packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.loadRanks();
    }

    @Override
    public void process(BUpdatePlayerLanguage packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        Language language = Language.getById(packet.getLanguageId());
        gamer.changeLanguage(language, false);
    }

    @Override
    public void process(SUnmute packet, ChannelProcessorContext ctx) {
        int playerId = packet.getPlayerId();

        core.getPunishmentsManager().removeActiveMute(playerId);
    }

    @Override
    public void process(SRegisterCommands packet, ChannelProcessorContext ctx) {
        core.setCommands(packet.getCommands());
    }

    @Override
    public void process(SSyncRequest packet, ChannelProcessorContext ctx) {
        Set<Integer> gamersId = new HashSet<>();

        for (Player player : core.getProxy().getAllPlayers()) {
            Gamer gamer = Gamer.getGamer(player);

            if (gamer == null) {
                Logging.IMP.warn("[SYNC] Unknown proxy gamer: {}",
                        player.getUsername());
                continue;
            }

            if (gamer.isLoggedIn() && gamer.isOnline() && gamer.getPlayerId() > 0) {
                gamersId.add(gamer.getPlayerId());
            }
        }

        ctx.setResponse(new CSyncResponse(gamersId.stream().mapToInt(Integer::intValue).toArray()));
        Logging.IMP.info("[SYNC] Data about {} players has been transmitted (Request: {})",
                gamersId.size(), packet.getRequestId());
    }

    @Override
    public void process(SPingRequest packet, ChannelProcessorContext ctx) {
        val gamerId = packet.getPlayerId();
        val gamer = Gamer.getGamer(gamerId);

        if (gamer == null) {
            ctx.setResponse(new CPingResponse(-1));
            return;
        }

        ctx.setResponse(new CPingResponse(gamer.getPlayer().getPing()));
    }

    @Override
    public void process(final @NotNull SPlayerPingRequest packet, final @NotNull ChannelProcessorContext ctx) {
        val pings = core.getProxy()
                .getAllPlayers()
                .stream()
                .map(Player::getPing)
                .sorted()
                .toList();

        if (pings.isEmpty()) {
            ctx.setResponse(new CPlayerPingResponse(0, 0, 0));
            return;
        }

        int n = pings.size();

        double median = (n % 2 == 0)
                ? (pings.get(n / 2 - 1) + pings.get(n / 2)) / 2.0
                : pings.get(n / 2);

        int index95 = (int) Math.ceil(0.95 * n) - 1;
        double p95 = pings.get(index95);

        int cut = (int) Math.floor(0.10 * n);
        List<Long> trimmed = pings.subList(0, n - cut);
        double trimmedMean = trimmed.stream().mapToInt(Long::intValue).average().orElse(0.0);

        ctx.setResponse(new CPlayerPingResponse(median, p95, trimmedMean));
    }

    @Override
    public void process(SRefreshRanksPacket packet, @NotNull ChannelProcessorContext ctx) {
        CompletableFuture.runAsync(() -> {
            val startTime = System.currentTimeMillis();

            core.getRanksService().load();

            core.getUserRanksService().refreshUsersRanks(Gamer.getGamers()).join();

            log.info("Refresh ranks completed successfully in {}ms", System.currentTimeMillis() - startTime);
        });
    }
}
