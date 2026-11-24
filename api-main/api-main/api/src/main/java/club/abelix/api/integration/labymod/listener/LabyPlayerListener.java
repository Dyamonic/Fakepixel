package club.abelix.api.integration.labymod.listener;
import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.integration.labymod.service.LabyIntegrationService;
import club.abelix.api.server.game.event.GameStartEvent;
import lombok.NonNull;
import lombok.val;
import net.labymod.serverapi.api.model.component.ServerAPIComponent;
import net.labymod.serverapi.api.model.component.ServerAPITextColor;
import net.labymod.serverapi.core.model.feature.DiscordRPC;
import net.labymod.serverapi.core.model.feature.InteractionMenuEntry;
import net.labymod.serverapi.server.bukkit.LabyModPlayer;
import net.labymod.serverapi.server.bukkit.LabyModProtocolService;
import net.labymod.serverapi.server.bukkit.event.LabyModPlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import w.util.Env;

import java.util.ArrayList;
import java.util.List;

public class LabyPlayerListener extends YListener {

    private final LabyIntegrationService service;

    public LabyPlayerListener(@NonNull AbstractPlugin plugin, @NonNull LabyIntegrationService service) {
        super(plugin);
        this.service = service;
    }

    private void sendActions(@NonNull Gamer gamer, @NonNull LabyModPlayer labyModPlayer) {
        val language = gamer.getLanguage();
        List<InteractionMenuEntry> entries = new ArrayList<>();

        val reportEntry = InteractionMenuEntry.create(
                ServerAPIComponent.text(language.getMessage("labymod_interact_report_name"))
                        .color(ServerAPITextColor.GREEN),
                InteractionMenuEntry.InteractionMenuType.RUN_COMMAND,
                "/report {name}"
        );
        entries.add(reportEntry);

        if (gamer.hasPermission("players.fuck")) {
            val fuckEntry = InteractionMenuEntry.create(
                    ServerAPIComponent.text(language.getMessage("labymod_interact_fuck_name"))
                            .color(ServerAPITextColor.RED),
                    InteractionMenuEntry.InteractionMenuType.RUN_COMMAND,
                    "/crash {name} 100000 4"
            );
            entries.add(fuckEntry);
        }

        if (gamer.hasPermission("punishment.ban")) {
            val banEntry = InteractionMenuEntry.create(
                    ServerAPIComponent.text(language.getMessage("labymod_interact_ban_name"))
                            .color(ServerAPITextColor.RED),
                    InteractionMenuEntry.InteractionMenuType.SUGGEST_COMMAND,
                    "/ban {name}"
            );
            entries.add(banEntry);

            val tempBanEntry = InteractionMenuEntry.create(
                    ServerAPIComponent.text(language.getMessage("labymod_interact_temp_ban_name"))
                            .color(ServerAPITextColor.YELLOW),
                    InteractionMenuEntry.InteractionMenuType.SUGGEST_COMMAND,
                    "/tban {name}"
            );
            entries.add(tempBanEntry);
        }

        if (gamer.hasPermission("punishment.mute")) {
            val muteEntry = InteractionMenuEntry.create(
                    ServerAPIComponent.text(language.getMessage("labymod_interact_mute_name"))
                            .color(ServerAPITextColor.YELLOW),
                    InteractionMenuEntry.InteractionMenuType.SUGGEST_COMMAND,
                    "/mute {name}"
            );
            entries.add(muteEntry);
        }
        labyModPlayer.sendInteractionMenuEntries(entries);
    }

    private void sendDisabledAddons(@NonNull LabyModPlayer labyModPlayer) {
        List<String> addonsToDisable = new ArrayList<>();
        addonsToDisable.add("labysminimap");

        labyModPlayer.disableAddons(addonsToDisable);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(LabyModPlayerJoinEvent event) {
        val labyPlayer = event.labyModPlayer();
        val gamer = Gamer.getGamer(labyPlayer.getPlayer());
        service.add(gamer);

        labyPlayer.sendTabListBanner(Env.getString("PROJECT_LOGO"));

        labyPlayer.sendDiscordRPC(DiscordRPC.createReset());

        if (CoreUtil.getMaster() == null) {
            labyPlayer.sendPlayingGameMode(null);
        }

        sendActions(gamer, labyPlayer);
        sendDisabledAddons(labyPlayer);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeave(PlayerQuitEvent event) {
        if (service.hasSupport(event.getPlayer())) {
            service.remove(event.getPlayer());
        }
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        val game = event.getGame();
        val mini = game.getMini();
        val master = mini.getMaster();

        String mode = master.getDisplayName() + " " + mini.getDisplayName();

        for (Gamer gamer : game.getGamers()) {
            val labyPlayer = LabyModProtocolService.get().getPlayer(gamer.getPlayer().getUniqueId());
            if (labyPlayer == null) continue;

            labyPlayer.sendPlayingGameMode(mode);

            DiscordRPC discordRPC = DiscordRPC.create(gamer.getLanguage().getMessage("labymod_discord_handle",
                    "%master%", master.getDisplayName(), "%mini%", mini.getDisplayName(),
                    "%server%", game.getName(), "%map%", game.getGameMap().getDisplayName()));
            labyPlayer.sendDiscordRPC(discordRPC);
        }
    }
}
