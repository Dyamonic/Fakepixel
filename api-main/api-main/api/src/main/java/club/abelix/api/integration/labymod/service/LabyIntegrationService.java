package club.abelix.api.integration.labymod.service;

import club.abelix.api.base.gamer.Gamer;
import lombok.extern.log4j.Log4j2;
import net.labymod.serverapi.server.bukkit.LabyModPlayer;
import net.labymod.serverapi.server.bukkit.LabyModProtocolService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log4j2
public class LabyIntegrationService {
    private final List<Player> LABY_GAMERS = new ArrayList<>();

    public void add(@NotNull Player player) {
        LABY_GAMERS.add(player);
        log.info("Added player {} to labymod", player.getName());
    }

    public void add(@NotNull Gamer gamer) {
        add(gamer.getPlayer());
    }

    public void remove(@NotNull Player player) {
        LABY_GAMERS.remove(player);
    }

    public void remove(@NotNull Gamer gamer) {
        remove(gamer.getPlayer());
    }

    public boolean hasSupport(@NotNull Player player) {
        return LABY_GAMERS.contains(player);
    }

    public boolean hasSupport(@NotNull Gamer gamer) {
        return hasSupport(gamer.getPlayer());
    }

    public LabyModPlayer get(@NotNull Player player) {
        return LabyModProtocolService.get().getPlayer(player.getUniqueId());
    }

    public LabyModPlayer get(@NotNull Gamer gamer) {
        if (gamer.getPlayer() == null) return null;

        return LabyModProtocolService.get().getPlayer(gamer.getPlayer().getUniqueId());
    }

    public List<UUID> getAllUUID() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : LABY_GAMERS) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    public List<Gamer> getAllGamers() {
        List<Gamer> gamers = new ArrayList<>();
        for (Player player : LABY_GAMERS) {
            gamers.add(Gamer.getGamer(player));
        }
        return gamers;
    }

    public boolean hasPlayers() {
        return !LABY_GAMERS.isEmpty();
    }
}
