package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.mayor.MayorManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MayorCommand implements CommandExecutor, TabCompleter {
    private final MayorManager mayor;
    public MayorCommand(MayorManager mayor) { this.mayor = mayor; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!mayor.isMayorEnabled()) {
            boolean adminDiagnostic = args.length >= 2 && args[0].equalsIgnoreCase("admin")
                    && (args[1].equalsIgnoreCase("debug") || args[1].equalsIgnoreCase("status") || args[1].equalsIgnoreCase("validate"));
            if (!adminDiagnostic) {
                sender.sendMessage(CC.color(MayorManager.DISABLED_MESSAGE));
                return true;
            }
            if (!sender.hasPermission("fakevival.mayor.admin")) {
                sender.sendMessage(CC.color("&cNo permission."));
                return true;
            }
            sender.sendMessage(CC.color("&7Mayor Elections: &cDisabled"));
            if (args[1].equalsIgnoreCase("validate")) {
                sender.sendMessage(CC.color("&7Commands blocked: &atrue"));
                sender.sendMessage(CC.color("&7NPC actions blocked: &atrue"));
                sender.sendMessage(CC.color("&7Mayor modifiers inactive: &atrue"));
                sender.sendMessage(CC.color("&7Tablist placeholders safe: &atrue"));
            } else {
                sender.sendMessage(CC.color("&7No election timers, phase advancement, or mayor perks are active."));
            }
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) { sender.sendMessage(mayor.debugLine()); return true; }
            mayor.openInfo(player); return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("run")) { if (player(sender) instanceof Player p) mayor.openPerkMenu(p); return true; }
        if (sub.equals("campaign")) {
            Player p = player(sender); if (p == null) return true;
            if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /mayor campaign <message>")); return true; }
            mayor.setCampaign(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length))); return true;
        }
        if (sub.equals("withdraw")) { if (player(sender) instanceof Player p) mayor.withdraw(p); return true; }
        if (sub.equals("vote")) { if (player(sender) instanceof Player p) mayor.openVoting(p, false); return true; }
        if (!sub.equals("admin")) { sender.sendMessage(CC.color("&e/mayor &7- election information\n&e/mayor run &7- become a candidate\n&e/mayor campaign <message>\n&e/mayor withdraw")); return true; }
        if (!sender.hasPermission("fakevival.mayor.admin")) { sender.sendMessage(CC.color("&cNo permission.")); return true; }
        if (!(sender instanceof Player actor)) { sender.sendMessage("This command requires an in-game admin."); return true; }
        if (args.length < 2) { help(sender); return true; }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "setvotelocation" -> mayor.setVoteLocation(actor);
            case "spawnnpc" -> { if (args.length < 3) sender.sendMessage(CC.color("&cUsage: /mayor admin spawnnpc <register|vote|results>")); else mayor.spawnNpc(actor, args[2]); }
            case "startregistration" -> mayor.startRegistration(actor);
            case "startvoting" -> mayor.startVoting(actor);
            case "endvoting" -> mayor.endVoting(actor);
            case "forcewinner" -> {
                if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /mayor admin forcewinner <player>")); break; }
                @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]); mayor.forceWinner(actor, target);
            }
            case "ban", "unban" -> {
                if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /mayor admin " + action + " <player> [reason]")); break; }
                @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Administrative election restriction";
                mayor.setElectionBan(actor, target, action.equals("ban"), reason);
            }
            case "cancel" -> mayor.cancel(actor);
            case "reload" -> { mayor.refreshNow(); sender.sendMessage(CC.color("&aMayor cache refresh queued.")); }
            case "debug" -> {
                sender.sendMessage(CC.color("&7[SMP Mayor] &e" + mayor.debugLine()));
                var location = mayor.voteLocation(); sender.sendMessage(CC.color("&7Voting location: &e" + (location == null ? "not set" : location.serverId() + ":" + location.world() + " " + (int)location.x() + "," + (int)location.y() + "," + (int)location.z())));
            }
            case "validate" -> {
                var v = mayor.validate();
                sender.sendMessage(CC.color("&7[SMP Mayor] DB tables: " + yes(v.database()) + "&7, election loaded: " + yes(v.electionLoaded()) + "&7, voting location: " + yes(v.locationSet())));
                sender.sendMessage(CC.color("&7NPC actions valid/invalid: &e" + v.validNpcActions() + "&7/&e" + v.invalidNpcActions() + "&7, perks: &e" + v.perks() + "&7, invalid perks: &e" + v.invalidPerks()));
                sender.sendMessage(CC.color("&7Invalid candidates: &e" + v.invalidCandidates() + "&7, timestamps: " + yes(v.timestampsValid()) + "&7, active modifier: " + yes(v.modifierActive())));
            }
            default -> help(sender);
        }
        return true;
    }

    private Player player(CommandSender sender) { if (sender instanceof Player p) return p; sender.sendMessage("Players only."); return null; }
    private String yes(boolean value) { return value ? "&atrue" : "&cfalse"; }
    private void help(CommandSender sender) { sender.sendMessage(CC.color("&e/mayor admin <setvotelocation|spawnnpc|startregistration|startvoting|endvoting|forcewinner|ban|unban|cancel|debug|validate>")); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> values = args.length == 1 ? List.of("run", "campaign", "withdraw", "vote", "admin")
                : args.length == 2 && args[0].equalsIgnoreCase("admin") ? List.of("setvotelocation", "spawnnpc", "startregistration", "startvoting", "endvoting", "forcewinner", "ban", "unban", "cancel", "reload", "debug", "validate")
                : args.length == 3 && args[1].equalsIgnoreCase("spawnnpc") ? List.of("register", "vote", "results") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(prefix)).toList();
    }
}
