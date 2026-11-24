package club.abelix.api.base.command;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.ICommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandProxy extends Command {

    private final ICommand origin;

    protected CommandProxy(ICommand origin) {
        super(origin.getName(), "", "/" + origin.getName(), Arrays.asList(origin.getAliases()));

        this.origin = origin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        origin.execute(sender instanceof Player ? Gamer.getGamer((Player) sender) : new ProxiedConsoleCommandSender(sender), args);

        return true;
    }
}
