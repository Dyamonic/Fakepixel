package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.JobManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class JobsCommand implements CommandExecutor {
    private final JobManager jobManager;
    private final MenuManager menuManager;
    private final Game game;

    public JobsCommand(Game game,JobManager jobManager, MenuManager menuManager) {
        this.game = game;
        this.jobManager = jobManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if(!game.getNetworkService().isOverworld()){player.sendMessage(CC.color(game.getNetworkService().isDungeon()?"&cQuests and jobs cannot be used inside dungeons.":"&cQuests and jobs can only be used on overworld servers."));return true;}
        ConfigurationSection section = jobManager.getJobsSection();

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            menuManager.openJobs(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&bJobs:"));
            if (section != null) {
                for (String job : section.getKeys(false)) {
                    if (!section.isConfigurationSection(job)) continue;
                    sender.sendMessage(CC.color("&7- &f" + job + " &8(/jobs join " + job + ")"));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("select")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /jobs join <job>"));
                return true;
            }
            String job = args[1].toLowerCase();
            if (jobManager.jobExists(job)) {
                sender.sendMessage(CC.color("&cThat job does not exist."));
                return true;
            }
            if (jobManager.hasJob(player, job)) {
                sender.sendMessage(CC.color("&eYou already joined the &f" + job + " &ejob."));
                return true;
            }
            jobManager.joinJob(player, job);
            sender.sendMessage(CC.color("&aYou joined the &f" + job + " &ajob."));
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /jobs leave <job>"));
                return true;
            }
            String job = args[1].toLowerCase();
            if (!jobManager.hasJob(player, job)) {
                sender.sendMessage(CC.color("&cYou are not in that job."));
                return true;
            }
            jobManager.leaveJob(player, job);
            sender.sendMessage(CC.color("&aYou left the &f" + job + " &ajob."));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(CC.color("&bJoined Jobs: &f" + jobManager.getJobsDisplay(player)));
            for (String job : jobManager.getJobs(player)) {
                sender.sendMessage(CC.color("&7- &f" + job + " &7Level: &a" + jobManager.getLevel(player, job) + " &7XP: &e" + jobManager.getXp(player, job)));
            }
            return true;
        }

        sender.sendMessage(CC.color("&cUsage: /jobs, /jobs list, /jobs join <job>, /jobs leave <job>, /jobs info"));
        return true;
    }
}
