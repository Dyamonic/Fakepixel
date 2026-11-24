package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;

public class LobbyTagListener extends TagListener {

    public LobbyTagListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        sendCreateTeams(player);
        addTeam(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        sendRemoveTeams(player);
        removeTeam(player);
    }

    @EventHandler
    public void onGroupUpdate(GamerDisplayNameUpdateEvent event) {
        addTeam(event.getGamer());
    }

    @Override
    protected Collection<Player> getPlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }
}