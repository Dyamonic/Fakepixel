package club.abelix.api.base.division.status.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.division.status.Status;
import club.abelix.api.base.division.status.event.UpdateGamerStatusEvent;
import club.abelix.api.base.division.status.task.StatusUpdateTask;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.data.setting.Setting;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentHashMap;


public class DivisionStatusListener extends YListener {

    public static final ConcurrentHashMap<Gamer, Hologram> PLAYER_HOLOGRAMS = new ConcurrentHashMap<>();

    public DivisionStatusListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamerJoin(GamerJoinEvent event) {
        val gamer = event.getGamer();
        val player = gamer.getPlayer();
        val selectedText = DivisionManager.IMP.getDisplayText(gamer);

        if (selectedText == null) {
            return;
        }

        handleStatusChange(gamer, new Status(selectedText));

        PLAYER_HOLOGRAMS.forEach((g, h) -> h.addReceiver(player));
    }

    @EventHandler
    public void onGamerUnload(GamerUnloadEvent event) {
        Gamer gamer = event.getGamer();
        removeStatusHologram(gamer);
    }

    @EventHandler
    public void onGamerStatusUpdate(UpdateGamerStatusEvent event) {
        Gamer gamer = event.getGamer();
        removeStatusHologram(gamer);
        handleStatusChange(gamer, event.getStatus());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        val player = event.getPlayer();
        PLAYER_HOLOGRAMS.forEach((g, h) -> h.removeReceiver(player));
    }

    private void handleStatusChange(Gamer gamer, Status status) {
        if (status == null || PLAYER_HOLOGRAMS.containsKey(gamer) || gamer.isVanished()) {
            return;
        }

        Hologram hologram = createHologram(gamer, status);
        PLAYER_HOLOGRAMS.put(gamer, hologram);

        StatusUpdateTask.INSTANCE.addTask(gamer, () -> {
            if (gamer.isOnline()) {
                hologram.teleport(gamer.getPlayer().getLocation().add(0.0, 2.1, 0.0));
            } else {
                removeStatusHologram(gamer);
            }
        });
    }

    private void removeStatusHologram(Gamer gamer) {
        if (PLAYER_HOLOGRAMS.containsKey(gamer)) {
            Hologram hologram = PLAYER_HOLOGRAMS.remove(gamer);
            if (hologram != null) hologram.remove();

            StatusUpdateTask.INSTANCE.stopPlayerTask(gamer);
        }
    }

    public Hologram createHologram(Gamer gamer, Status status) {
        Hologram hologram = new Hologram(gamer.getPlayer().getLocation().add(0.0, 2.1, 0.0));
        hologram.setClickAction(null);
        hologram.addTextLine(status.getDisplayName(), true, false);
        hologram.setPublicStatus(false);
        Bukkit.getOnlinePlayers().forEach(hologram::addReceiver);
        return hologram;
    }
}
