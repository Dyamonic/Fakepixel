package club.abelix.games.buildffa.game.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Special;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AntiAfkManager {

    static final long PLAYER_MAX_AFK_MILLIS = 3L * 60L * 1000L;

    BuildFFAGame game;
    Map<UUID, MovementData> playerMovementData = new HashMap<>();

    public void enable() {
        new AntiAfkListener(this);
        new AntiAfkTask(this).timer(20L, 20L);
    }

    private void registerPlayer(Player player) {
        playerMovementData.put(player.getUniqueId(), new MovementData(System.currentTimeMillis()));
    }

    private void unregisterPlayer(Player player) {
        playerMovementData.remove(player.getUniqueId());
    }

    private void updateMovement(Player player) {
        MovementData data = playerMovementData.get(player.getUniqueId());

        if (data == null) {
            return;
        }

        data.setLastMovementTime(System.currentTimeMillis());
    }

    private boolean isExempt(Player player) {
        return game.isSpectating(player) || game.hasPermission(player, "buildffa.antiafk.exempt");
    }

    private void sendPlayerToLimbo(Gamer gamer) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> sendPlayerToLimbo(gamer));
            return;
        }
        game.getEventManager().beforeServerTransfer(gamer.getPlayer());
        gamer.sendLocalizedMessage("kicked_for_afk_message");

        gamer.connectMode(Special.LIMBO, true).whenComplete((result, throwable) -> {
            if (result != ConnectStatus.SUCCESS) {
                gamer.disconnect("kicked_for_afk_reason");
            }
        });
    }

    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static final class MovementData {

        long lastMovementTime;

        public boolean isAfk(final long now) {
            return now - lastMovementTime >= PLAYER_MAX_AFK_MILLIS;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class AntiAfkListener extends YListener implements Listener {

        AntiAfkManager antiAfkManager;

        private AntiAfkListener(AntiAfkManager antiAfkManager) {
            super(antiAfkManager.getGame().getPlugin());
            this.antiAfkManager = antiAfkManager;
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            if (event.getTo() == null) {
                return;
            }

            if (event.getFrom().getWorld() != event.getTo().getWorld()) {
                antiAfkManager.updateMovement(event.getPlayer());
                return;
            }

            if (event.getFrom().getX() == event.getTo().getX()
                    && event.getFrom().getY() == event.getTo().getY()
                    && event.getFrom().getZ() == event.getTo().getZ()) {
                return;
            }

            antiAfkManager.updateMovement(event.getPlayer());
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            antiAfkManager.registerPlayer(event.getPlayer());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            antiAfkManager.unregisterPlayer(event.getPlayer());
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class AntiAfkTask extends AbstractTask {

        AntiAfkManager antiAfkManager;

        private AntiAfkTask(AntiAfkManager antiAfkManager) {
            super(antiAfkManager.getGame().getPlugin());
            this.antiAfkManager = antiAfkManager;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, MovementData>> iterator = antiAfkManager.getPlayerMovementData().entrySet().iterator();
            List<Player> newlyExemptPlayers = new ArrayList<>();

            while (iterator.hasNext()) {
                Map.Entry<UUID, MovementData> entry = iterator.next();
                Player player = Bukkit.getPlayer(entry.getKey());

                if (player == null || !player.isOnline()) {
                    iterator.remove();
                    continue;
                }

                if (antiAfkManager.isExempt(player)) {
                    entry.getValue().setLastMovementTime(now);
                    continue;
                }

                if (!entry.getValue().isAfk(now)) {
                    continue;
                }

                iterator.remove();

                if (antiAfkManager.isExempt(player)) {
                    newlyExemptPlayers.add(player);
                    continue;
                }

                antiAfkManager.sendPlayerToLimbo(Gamer.getGamer(player));
            }

            for (Player player : newlyExemptPlayers) antiAfkManager.registerPlayer(player);
        }
    }
}
