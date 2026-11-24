package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SetRtpLocationCommand implements CommandExecutor {
    private final Game game;

    public SetRtpLocationCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("fakevival.rtp.admin")) {
            player.sendMessage(CC.color("&cYou do not have permission."));
            return true;
        }
        if (game.getNetworkService().isDungeon()) {
            player.sendMessage(CC.color("&cRTP locations cannot be set inside dungeon servers."));
            return true;
        }
        if (!game.getNetworkService().isOverworld()) {
            player.sendMessage(CC.color("&cRTP locations can only be set on overworld servers."));
            return true;
        }
        if (!isAllowedWorld(player.getWorld())) {
            player.sendMessage(CC.color("&cRTP locations cannot use hub, lobby, dungeon, Nether, or End worlds."));
            return true;
        }
        int min = club.abelix.games.fakevival.config.RuntimeSettings.config(game).getInt("rtp.min-radius", 500);
        int max = Math.max(min + 1, club.abelix.games.fakevival.config.RuntimeSettings.config(game).getInt("rtp.max-radius", 5000));
        game.getNetworkService().saveRtpLocation(player, min, max, success -> player.sendMessage(CC.color(success
                ? "&aRTP location set for this overworld server."
                : "&cCould not save the RTP location to MariaDB.")));
        return true;
    }

    private boolean isAllowedWorld(World world) {
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) return false;
        String name = world.getName().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return !name.equals("hub") && !name.equals("lobby") && !name.startsWith("dungeon_")
                && !name.startsWith("dun_") && !name.startsWith("smpdun_instance_");
    }
}
