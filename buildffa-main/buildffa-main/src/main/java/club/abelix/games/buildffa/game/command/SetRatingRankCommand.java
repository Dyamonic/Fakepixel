package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.network.common.logging.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@NamedCommand("setratingrank")
public final class SetRatingRankCommand extends YListener implements Listener {
    private static final String PERMISSION = "buildffa.setratingrank";
    private static final List<String> DIVISION_IDENTIFIERS = Arrays.asList(
            "newbie", "bronze_i", "bronze_ii", "bronze_iii",
            "metal_i", "metal_ii", "metal_iii",
            "gold_i", "gold_ii", "gold_iii",
            "emerald_i", "emerald_ii", "emerald_iii", "ruby"
    );

    public SetRatingRankCommand(BuildFFAGame game) {
        super(game.getPlugin());
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(PERMISSION)
    public static void execute(BukkitExecutionSession session) throws CommandException {
        Gamer administrator = session.getGamer();
        String[] arguments = session.getArguments();
        if (arguments.length < 2) {
            administrator.sendMessage(color("&cUsage: /setratingrank <player> <ratingRank>"));
            administrator.sendMessage(color("&7Valid values: &f" + String.join(", ", DIVISION_IDENTIFIERS)));
            return;
        }

        AbstractDivision division = resolveDivision(String.join(" ", Arrays.copyOfRange(arguments, 1, arguments.length)));
        if (division == null) {
            administrator.sendMessage(color("&cUnknown rating rank."));
            administrator.sendMessage(color("&7Valid values: &f" + String.join(", ", DIVISION_IDENTIFIERS)));
            return;
        }

        Gamer target = session.getGamer(0);
        if (target == null || !target.isOnline() || target.getPlayer() == null) {
            administrator.sendMessage(color("&cThat player's loaded profile is not available. No rating was changed."));
            return;
        }

        int previousRating = target.getLifetimeStat("rating");
        int newRating = division.getRequiredRating();
        int currentDivision = target.getLifetimeStat("division");

        target.addStat("rating", newRating - previousRating);
        target.addStat("division", division.ordinal() - currentDivision);

        String divisionName = division.getName(administrator.getLanguage());
        String formattedRating = NumberFormat.getIntegerInstance(Locale.US).format(newRating);
        administrator.sendMessage(color("&aSet &f" + target.getPlayerName() + "&a's rating rank to &f"
                + divisionName + " &awith &f" + formattedRating + " rating&a."));
        target.sendMessage(color("&eAn administrator changed your rating rank to &f" + divisionName
                + " &ewith &f" + formattedRating + " rating&e."));

        Logging.IMP.info("BuildFFA setratingrank administrator={} target={} previousRating={} newRating={} division={}",
                administrator.getPlayerName(), target.getPlayer().getUniqueId(), previousRating, newRating, division.name());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(PlayerChatTabCompleteEvent event) {
        if (!event.getPlayer().hasPermission(PERMISSION)) return;

        String buffer = event.getChatMessage();
        if (!buffer.toLowerCase(Locale.ROOT).startsWith("/setratingrank")) return;

        String[] arguments = buffer.substring(1).split(" ", -1);
        if (arguments.length == 2) {
            String prefix = arguments[1].toLowerCase(Locale.ROOT);
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
            event.getTabCompletions().clear();
            event.getTabCompletions().addAll(players);
            return;
        }

        if (arguments.length >= 3) {
            String prefix = arguments[arguments.length - 1].toLowerCase(Locale.ROOT);
            List<String> divisions = new ArrayList<>();
            for (String identifier : DIVISION_IDENTIFIERS) {
                if (identifier.startsWith(prefix)) divisions.add(identifier);
            }
            event.getTabCompletions().clear();
            event.getTabCompletions().addAll(divisions);
        }
    }

    private static AbstractDivision resolveDivision(String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("newbie") || normalized.equals("novice")) return AbstractDivision.NOVICE;

        try {
            return AbstractDivision.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
