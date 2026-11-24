package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@NamedCommand("eventmanager")
public final class EventManagerCommand implements Listener {
    private static BuildFFAEventManager eventManager;

    public static void setEventManager(BuildFFAEventManager manager) {
        eventManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.EVENT_MANAGER_PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (eventManager == null) return;
        String[] arguments = session.getArguments();
        if (arguments.length == 0) {
            eventManager.open(session.getGamer().getPlayer());
            return;
        }
        if (arguments[0].equalsIgnoreCase("list")) {
            if (arguments.length == 1) eventManager.sendEventList(session.getGamer());
            else eventManager.sendUsage(session.getGamer());
            return;
        }
        if (arguments[0].equalsIgnoreCase("stop")) {
            if (arguments.length >= 2) eventManager.stopEvent(session.getGamer(), joinEventName(arguments));
            else eventManager.sendStopUsage(session.getGamer());
            return;
        }
        if (arguments[0].equalsIgnoreCase("enable") || arguments[0].equalsIgnoreCase("disable")) {
            boolean enabled = arguments[0].equalsIgnoreCase("enable");
            if (arguments.length >= 2) eventManager.setEventEnabled(session.getGamer(), joinEventName(arguments), enabled);
            else eventManager.sendAvailabilityUsage(session.getGamer(), enabled);
            return;
        }
        eventManager.sendUsage(session.getGamer());
    }

    private static String joinEventName(String[] arguments) {
        return String.join(" ", Arrays.copyOfRange(arguments, 1, arguments.length));
    }

    @EventHandler
    public void onTabComplete(PlayerChatTabCompleteEvent event) {
        if (eventManager == null || !eventManager.hasEventManagerPermission(event.getPlayer())) return;
        String buffer = event.getChatMessage();
        if (!buffer.toLowerCase(Locale.ROOT).startsWith("/eventmanager")) return;
        String[] arguments = buffer.substring(1).split(" ", 3);
        List<String> suggestions = new ArrayList<>();
        if (arguments.length == 2) {
            String prefix = arguments[1].toLowerCase(Locale.ROOT);
            for (String subcommand : Arrays.asList("list", "enable", "disable", "stop")) {
                if (subcommand.startsWith(prefix)) suggestions.add(subcommand);
            }
        } else if (arguments.length == 3) {
            String prefix = normalize(arguments[2]);
            List<String> eventIds;
            if (arguments[1].equalsIgnoreCase("enable")) eventIds = eventManager.getDisabledEventIds();
            else if (arguments[1].equalsIgnoreCase("disable")) eventIds = eventManager.getEnabledEventIds();
            else if (arguments[1].equalsIgnoreCase("stop")) eventIds = eventManager.getActiveEventIds();
            else return;
            for (String id : eventIds) if (normalize(id).startsWith(prefix)) suggestions.add(id);
        } else return;
        event.getTabCompletions().clear();
        event.getTabCompletions().addAll(suggestions);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
