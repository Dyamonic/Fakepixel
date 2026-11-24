package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerAsyncLoadEvent;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerInterruptLoadEvent;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.party.manager.PartyManager;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.lobby.snow.BiomeChangePacketListener;
import club.abelix.commons.data.prefix.Prefix;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.common.NetworkPlayerLoad;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BRemoveProperty;
import club.abelix.network.packet.play.both.BSetProperty;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import club.abelix.network.packet.play.client.CNetworkPlayerLoadRequest;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SAddPlayerSuffix;
import club.abelix.network.packet.play.server.SNetworkPlayerLoadResponse;
import club.abelix.network.packet.play.server.SRemovePrefix;
import club.abelix.network.packet.play.server.SUpdatePrefix;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class GamerListener extends YListener implements PlayServerProcessor {

    private final Map<String, Gamer> loadedGamers = new ConcurrentHashMap<>();

    public GamerListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamerStartLoading(AsyncPlayerPreLoginEvent event) {
        plugin.getLogger().info(String.format("Gamer %s starting to loading...", event.getName()));
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        Instant loadStart = Instant.now();

        String name = event.getName();

        Gamer gamer = new Gamer(name);
        // Загрузка данные из бд
        try {
            System.out.println(Thread.currentThread().getName() + "is loading gamer " + name);
            gamer.load();
        } catch (Exception any) {
            Logging.IMP.error("Gamer {}. Failed load from database. Details:", name);
            any.printStackTrace();

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cGame failed database data");
            return;
        }

        try {
            // Загрузка данных с кора
            SNetworkPlayerLoadResponse response = Connection.awaitPacket(
                    new CNetworkPlayerLoadRequest(
                            gamer.getPlayerId(), NetworkPlayerLoad.ALL_INFO
                    )).get(1, TimeUnit.SECONDS);
            NetworkPlayerLoad.PlayerHolder player = response.getPlayers().get(0);

            // игрок не найден на коре - но зашел сюда
            // что-то тут не так, ему нельзя дать войти
            if (!player.isOnline()) {
                throw new IllegalStateException("Core player is offline");
            }

            gamer.setServer(player.getServer());

            NetworkPlayerLoad.PartyHolder party = player.getParty();

            if (party != null) {
                PartyManager.handlePartyLoad(gamer, player.getParty());
            }
        } catch (InterruptedException | ExecutionException | IllegalStateException | TimeoutException e) {
            plugin.getLogger().warning(String.format("Failed gamer %s preload event! Details: ", name));
            e.printStackTrace();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage("§cThe data is not loaded. You may not be connected to the coordinator!");
            return;
        }

        GamerAsyncLoadEvent loadEvent = new GamerAsyncLoadEvent(gamer, event.getUniqueId());
        plugin.getServer().getPluginManager().callEvent(loadEvent);

        if (loadEvent.isCancelled()) {

            plugin.getLogger().info(String.format("Gamer %s [Id: %d] failed login event. Kicked him",
                    gamer.getName(), gamer.getPlayerId()));
            event.setKickMessage(loadEvent.getCancelReason());
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);

            plugin.getLogger().info(String.format("Gamer %s [Id: %d] in %d ms. Kicked in reason %s",
                    gamer.getName(), gamer.getPlayerId(),
                    Duration.between(loadStart, Instant.now()).toMillis(), loadEvent.getCancelReason()));
            return;
        }

        plugin.getLogger().info(String.format("%s (Real name: %s) [Id: %d] success loaded in %d ms!",
                gamer.getPlayerName(), event.getName(), gamer.getPlayerId(), Duration.between(loadStart, Instant.now()).toMillis()));
        loadedGamers.put(gamer.getName(), gamer);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGamerJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        Gamer gamer = loadedGamers.remove(player.getName());

        boolean success = false;

        try {
            if (gamer == null) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cGame data not loaded");
                throw new RuntimeException(String.format("Failed gamer load data %s", player.getName()));
            }

            boolean lobby = CoreUtil.isLobby();

            int lobbyLimit = lobby ? 100 : 150;

            var tooManyPlayers = plugin.getServer().getOnlinePlayers().size() >= lobbyLimit;

            if ((lobby || CoreUtil.isUncommon()) && event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
                if (!tooManyPlayers && gamer.getRanks().hasPermission("online.joinfullbypass")) {
                    event.setResult(PlayerLoginEvent.Result.ALLOWED);
                }
            }

            if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
                throw new RuntimeException(String.format("Failed gamer %s join (%s)", player.getName(),
                        event.getResult()));
            }

            gamer.setPlayer(player);

            boolean enableDeveloperMode = gamer.hasPermission("dev.mode") && (gamer.getProperty("Dev/Mode") == 1);

            PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission("bukkit.command.version", enableDeveloperMode);
            attachment.setPermission("minecraft.command.help", enableDeveloperMode);
            attachment.setPermission("bukkit.command.help", enableDeveloperMode);
            attachment.setPermission("minecraft.command.me", enableDeveloperMode);
            attachment.setPermission("bukkit.command.me", enableDeveloperMode);
            attachment.setPermission("minecraft.command.tell", enableDeveloperMode);
            attachment.setPermission("bukkit.command.tell", enableDeveloperMode);
            attachment.setPermission("minecraft.command.trigger", enableDeveloperMode);
            attachment.setPermission("bukkit.command.plugins", enableDeveloperMode);

            player.setOp(enableDeveloperMode);

            if (enableDeveloperMode) gamer.sendLocalizedMessage("dev_mode_enable");
            // ====== Режим разработчика КОНЕЦ =======

            // забираем права у всех
            attachment.unsetPermission("worldedit.brush.targetmask");
            attachment.unsetPermission("worldedit.brush.targetoffset");

            if (gamer.getProperty("Lobby/Snow") == 1) {
                BiomeChangePacketListener.toggleSnowForPlayer(player);
            }

            //добавляем в мапу всех геймеров
            Gamer.addGamer(gamer);

            // Отправляем ивент о входе игрока
            GamerJoinEvent gamerJoinEvent = new GamerJoinEvent(gamer);
            HeartBeat.callEvent(gamerJoinEvent);

            if (gamerJoinEvent.isCancelled()) {
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(gamerJoinEvent.getCancelReason());
            } else {
                success = true;
            }

        } finally {
            if (!success) {
                HeartBeat.callEvent(new GamerInterruptLoadEvent(gamer));
            }
        }
    }

    /**
     * Нужно, чтобы игроков 1.13+ не кикало за табкомплит
     * на серверах < 1.13. Костыль, но иначе никак :(
     */
    @EventHandler
    public void onSpamKick(PlayerKickEvent event) {
        if (event.getReason().contains("Kicked for spamming")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerShuffle(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!message.endsWith("[shuffled]")) {
            return;
        }

        message = message.replace("\\[shuffled]", "");

        event.getRecipients().clear();
        event.getRecipients().add(player);
        event.setMessage(message);

        for (Player target : plugin.getServer().getOnlinePlayers()) {
            Gamer gamer = Gamer.getGamer(target);
            if (!gamer.getRanks().hasPermission("chat.advertisement")) {
                continue;
            }

            gamer.sendMessage(" §cAdvertisement §7» §7" + ChatColor.stripColor(player.getDisplayName()) + ": " + message);
        }
    }

    @EventHandler
    public void onDisplayNameUpdate(GamerDisplayNameUpdateEvent event) {
        Gamer gamer = event.getGamer();

        gamer.getPlayer().setDisplayName(gamer.getDisplayName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();

        Gamer gamer = Gamer.getGamer(player);
        gamer.updateVersion();

        player.setDisplayName(gamer.getDisplayName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        Gamer gamer = Gamer.remove(event.getPlayer());

        if (gamer != null) {
            HeartBeat.callEvent(new GamerUnloadEvent(gamer));
        }
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(BUserRankRefreshPacket packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        gamer.loadRanks();
        gamer.refreshDisplayName();

        log.info("Refreshed ranks for {}", gamer.getName());
    }

    @Override
    public void process(BSetProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            gamer.updateProperty(packet.getProperty(), packet.getValue());
        }
    }

    @Override
    public void process(SAddPlayerSuffix packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            gamer.addPurchasedSuffix(packet.getSuffixId());
        }
    }

    @Override
    public void process(BRemoveProperty packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            gamer.dropProperty(packet.getProperty());
        }
    }

    @Override
    public void process(SRemovePrefix packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            gamer.setPrefix(null);
            gamer.updateDisplayName();

            HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(gamer));
        }
    }

    @Override
    public void process(SUpdatePrefix packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer != null) {
            Prefix prefix = new Prefix(packet.getExpire(), packet.getText());

            if (!prefix.isExpired()) {
                gamer.setPrefix(prefix);
            }

            gamer.updateDisplayName();

            HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(gamer));
        }
    }
}
