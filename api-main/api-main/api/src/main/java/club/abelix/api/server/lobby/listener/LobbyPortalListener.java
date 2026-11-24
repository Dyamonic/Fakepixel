package club.abelix.api.server.lobby.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.mode.selector.ModeSelectorGuisFactory;
import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.type.mode.Lobby;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.concurrent.TimeUnit;

public class LobbyPortalListener extends YListener {

    private static final String COOLDOWN_FORMAT = "%s_hubmenu";

    public LobbyPortalListener(@NonNull BaseLobbyServer server) {
        super(server.getPlugin());
    }

    @EventHandler
    public void onPortalMove(@NonNull PlayerMoveEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        if (player.getLocation().getBlock().getType() != Material.PORTAL) return;

        if (CoreUtil.isHub()) {
            val formattedCooldown = COOLDOWN_FORMAT.formatted(gamer.getLowerName());
            if (!CooldownUtil.hasCooldown(formattedCooldown)) {
                CooldownUtil.addCooldown(formattedCooldown, 3, TimeUnit.SECONDS);
                DiBukkitPlatform.getFactory()
                        .getBeanService()
                        .withBean(ModeSelectorGuisFactory.class, f -> f.openSeverSelectorGui(gamer, null));
            }
        } else if (CoreUtil.isLobby()) CoreUtil.connectMode(Gamer.getGamer(event.getPlayer()), Lobby.HUB);
    }
}
