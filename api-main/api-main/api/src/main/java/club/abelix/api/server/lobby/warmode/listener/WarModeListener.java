package club.abelix.api.server.lobby.warmode.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.warmode.WarModeManager;
import club.abelix.api.server.lobby.warmode.event.WarModeStopEvent;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * @author pro0xy
 * <p>Created on 03.09.2023 at 15:48</p>
 */

public class WarModeListener extends YListener {

    private final BaseLobbyServer baseLobbyServer;

    public WarModeListener(@NonNull BaseLobbyServer server) {
        super(server.getPlugin());
        baseLobbyServer = server;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamerJoin(GamerJoinEvent event){
        if (WarModeManager.INSTANCE.getWarMode()){
            Gamer gamer = event.getGamer();
            WarModeManager.INSTANCE.giveKit(gamer.getPlayer());
            gamer.sendLocalizedMessage("war_mode_join_message");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        if (WarModeManager.INSTANCE.getWarMode()){
            WarModeManager.INSTANCE.giveKit(event.getPlayer());
            event.getPlayer().teleport(baseLobbyServer.getSpawnLocation());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (WarModeManager.INSTANCE.getWarMode()){
            if (event.getEntity() instanceof Player player) {
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);

                    player.spigot().respawn();

                    Gamer gamer = Gamer.getGamer(player);
                    gamer.sendLocalizedActionBar("war_mode_death_actionbar_message");

                    player.teleport(baseLobbyServer.getSpawnLocation());
                    WarModeManager.INSTANCE.giveKit(player);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWarModeStop(WarModeStopEvent event){
        for (Gamer target : Gamer.getGamers()){
            target.sendLocalizedMessage("war_mode_end_message");
        }
        new AbstractTask(plugin) {
            @Override
            protected void run() {
                Bukkit.shutdown();
            }
        }.later(100);
    }
}
