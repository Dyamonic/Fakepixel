package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.staff.CpsCheckManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@NamedCommand("cpscheck")
public final class CpsCheckCommand implements Listener {
    public static final String PERMISSION = BuildFFAGame.CPS_CHECK_PERMISSION;
    private static CpsCheckManager manager;

    public static void setManager(CpsCheckManager cpsCheckManager) {
        manager = cpsCheckManager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (manager == null) return;
        Player staff = session.getGamer().getPlayer();
        String[] arguments = session.getArguments();
        if (arguments.length != 1) {
            manager.sendUsage(staff);
            return;
        }
        if (arguments[0].equalsIgnoreCase("stop")) {
            manager.stop(staff, true);
            return;
        }
        manager.toggle(staff, arguments[0]);
    }

    @EventHandler
    public void onTabComplete(PlayerChatTabCompleteEvent event) {
        if (manager == null || !manager.hasPermission(event.getPlayer())) return;
        String buffer = event.getChatMessage();
        if (!buffer.toLowerCase(Locale.ROOT).startsWith("/cpscheck")) return;
        String[] arguments = buffer.substring(1).split(" ", -1);
        if (arguments.length != 2) return;
        String prefix = arguments[1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        if (manager.isMonitoring(event.getPlayer()) && "stop".startsWith(prefix)) suggestions.add("stop");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) suggestions.add(player.getName());
        }
        event.getTabCompletions().clear();
        event.getTabCompletions().addAll(suggestions);
    }
}
