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

@NamedCommand("reachcheck")
public final class ReachCheckCommand implements Listener {
    private static CpsCheckManager manager;

    public static void setManager(CpsCheckManager staffMonitor) {
        manager = staffMonitor;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.REACH_CHECK_PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (manager == null) return;
        manager.executeReach(session.getGamer().getPlayer(), session.getArguments());
    }

    @EventHandler
    public void onTabComplete(PlayerChatTabCompleteEvent event) {
        if (!Bukkit.isPrimaryThread()) return;
        if (manager == null || !manager.hasReachPermission(event.getPlayer())) return;
        String[] arguments = event.getChatMessage().split(" ", -1);
        if (arguments.length != 2 || !arguments[0].equalsIgnoreCase("/reachcheck")) return;
        String prefix = arguments[1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        if ("stop".startsWith(prefix)) suggestions.add("stop");
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) suggestions.add(player.getName());
        event.getTabCompletions().clear();
        event.getTabCompletions().addAll(suggestions);
    }
}
