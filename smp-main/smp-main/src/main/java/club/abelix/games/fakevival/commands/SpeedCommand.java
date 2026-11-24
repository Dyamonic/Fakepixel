package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class SpeedCommand implements CommandExecutor {
    private static final String PERMISSION = "fakevival.command.speed";
    private static final String OTHERS_PERMISSION = "fakevival.command.speed.others";
    private final JavaPlugin plugin;

    public SpeedCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return CommandUtil.noPermission(plugin, sender);
        if (args.length < 2 || args.length > 3) {
            usage(sender);
            return true;
        }

        SpeedType type = SpeedType.from(args[0]);
        if (type == null) {
            sender.sendMessage(CC.color("&cSpeed type must be walk or fly."));
            return true;
        }

        Player target;
        if (args.length == 3) {
            if (!sender.hasPermission(OTHERS_PERMISSION)) {
                sender.sendMessage(CC.color("&cYou do not have permission to change other players' speed."));
                return true;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            target = player;
        }

        String value = args[1].trim();
        if (value.equalsIgnoreCase("reset")) {
            setSpeed(target, type, type.defaultSpeed);
            target.sendMessage(CC.color(type == SpeedType.WALK ? "&aReset your walk speed." : "&aReset your fly speed."));
            if (sender != target) sender.sendMessage(CC.color("&aUpdated speed for " + target.getName() + "."));
            return true;
        }

        int friendlySpeed;
        try {
            friendlySpeed = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(CC.color("&cSpeed must be between 1 and 10."));
            return true;
        }
        if (friendlySpeed < 1 || friendlySpeed > 10) {
            sender.sendMessage(CC.color("&cSpeed must be between 1 and 10."));
            return true;
        }

        setSpeed(target, type, friendlySpeed / 10.0F);
        target.sendMessage(CC.color((type == SpeedType.WALK ? "&aSet your walk speed to &e" : "&aSet your fly speed to &e") + friendlySpeed + "&a."));
        if (sender != target) sender.sendMessage(CC.color("&aUpdated speed for " + target.getName() + "."));
        return true;
    }

    private void setSpeed(Player player, SpeedType type, float speed) {
        float clamped = Math.clamp(speed, 0.0F, 1.0F);
        if (type == SpeedType.WALK) {
            player.setWalkSpeed(clamped);
        } else {
            player.setFlySpeed(clamped);
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(CC.color("&cUsage: /speed <walk|fly> <1-10|reset>"));
    }

    private enum SpeedType {
        WALK(0.2F),
        FLY(0.1F);

        private final float defaultSpeed;

        SpeedType(float defaultSpeed) {
            this.defaultSpeed = defaultSpeed;
        }

        private static SpeedType from(String raw) {
            return switch (raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT)) {
                case "walk", "w" -> WALK;
                case "fly", "f" -> FLY;
                default -> null;
            };
        }
    }
}
