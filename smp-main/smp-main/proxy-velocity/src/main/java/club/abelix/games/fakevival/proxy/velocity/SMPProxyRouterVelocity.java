package club.abelix.games.fakevival.proxy.velocity;

import club.abelix.games.fakevival.proxy.RouterCore;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Plugin(id = "smpproxyrouter", name = "SMPProxyRouter", version = "1.0.0", authors = {"1zg"})
public final class SMPProxyRouterVelocity {
    private final ProxyServer proxy;
    private final Logger logger;
    private RouterCore router;

    @Inject
    public SMPProxyRouterVelocity(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        router = new RouterCore(new VelocityProxyPlatformAdapter(proxy));
        var commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder("smpjoin").plugin(this).build(), new JoinCommand());
        commands.register(commands.metaBuilder("smprouter").aliases("smprouterstatus").plugin(this).build(), new StatusCommand());
        logger.info("SMPProxyRouter enabled on Velocity. MariaDB routing is active.");
    }

    private final class JoinCommand implements SimpleCommand {
        @Override public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player player)) { source.sendMessage(Component.text("Players only.")); return; }
            String[] args = invocation.arguments();
            if (args.length < 1) { player.sendMessage(Component.text("§cUsage: /smpjoin <hub|overworld|dungeon> [dungeonId]")); return; }
            String type = router.groups().normalize(args[0]);
            if (type == null) { player.sendMessage(Component.text("§cUnknown server group.")); return; }
            String dungeonId = args.length >= 2 ? args[1] : "unknown";
            proxy.getScheduler().buildTask(SMPProxyRouterVelocity.this, () -> {
                try {
                    if (!router.route(player.getUniqueId(), type, dungeonId))
                        player.sendMessage(Component.text("§cNo available " + type.toLowerCase() + " server was found."));
                } catch (Exception exception) {
                    logger.warn("Route failed for {}: {}", player.getUsername(), exception.getMessage());
                    player.sendMessage(Component.text("§cCould not route you right now."));
                }
            }).schedule();
        }
    }

    private final class StatusCommand implements SimpleCommand {
        @Override public boolean hasPermission(Invocation invocation) { return invocation.source().hasPermission("fakevival.proxy.admin"); }
        @Override public void execute(Invocation invocation) {
            proxy.getScheduler().buildTask(SMPProxyRouterVelocity.this, () -> {
                try {
                    for (String type : new String[]{"HUB", "OVERWORLD", "DUNGEON"}) {
                        invocation.source().sendMessage(Component.text("§6" + type + " Servers:"));
                        for (var server : router.database().available(type)) {
                            invocation.source().sendMessage(Component.text("§f" + server.id() + " §a" + server.status() + " §e" + server.online() + "/" + server.max() + " §bTPS " + String.format("%.1f", server.tps()) + (type.equals("DUNGEON") ? " §dactiveRuns=" + server.activeRuns() : "")));
                        }
                    }
                } catch (Exception exception) {
                    invocation.source().sendMessage(Component.text("§cCould not load router status."));
                }
            }).schedule();
        }
    }
}
