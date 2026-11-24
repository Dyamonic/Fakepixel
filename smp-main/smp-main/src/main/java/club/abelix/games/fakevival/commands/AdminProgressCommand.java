package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class AdminProgressCommand implements CommandExecutor {
    public enum Type { SKILL, JOB }

    private final Game plugin;
    private final Type type;

    public AdminProgressCommand(Game plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = type == Type.SKILL ? "fakevival.admin.skill" : "fakevival.admin.job";
        if (!CommandUtil.requireExactAdmin(plugin, sender, permission)) return true;
        if (args.length != 3) {
            sender.sendMessage(CC.color(type == Type.SKILL
                    ? "&cUsage: /addskill <player> <skillname> <level>"
                    : "&cUsage: /addjob <player> <jobname> <level>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }
        String id = args[1].trim().toLowerCase(Locale.ROOT);
        int requested;
        try {
            requested = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(CC.color("&cLevel must be a whole number."));
            return true;
        }

        int applied;
        if (type == Type.SKILL) {
            if (!plugin.getSkillManager().skillExists(id)) {
                sender.sendMessage(CC.color("&cUnknown skill: " + id));
                return true;
            }
            applied = Math.clamp(requested, 1, plugin.getSkillManager().getMaxLevel(id));
            plugin.getSkillManager().setLevel(target, id, applied);
            if (plugin.getMmoStatService() != null) plugin.getMmoStatService().refreshPlayerMMOStats(target);
            sender.sendMessage(CC.color("&aSet " + target.getName() + "'s " + id + " level to " + applied + "."));
            target.sendMessage(CC.color("&aYour " + id + " skill level was set to " + applied + " by an admin."));
        } else {
            // JobManager#jobExists is the established 'missing job' predicate used by /jobs.
            if (plugin.getJobManager().jobExists(id)) {
                sender.sendMessage(CC.color("&cUnknown job: " + id));
                return true;
            }
            applied = Math.clamp(requested, 1, plugin.getJobManager().getMaxLevel(id));
            plugin.getJobManager().setLevel(target, id, applied);
            sender.sendMessage(CC.color("&aSet " + target.getName() + "'s " + id + " job level to " + applied + "."));
            target.sendMessage(CC.color("&aYour " + id + " job level was set to " + applied + " by an admin."));
        }
        return true;
    }
}
