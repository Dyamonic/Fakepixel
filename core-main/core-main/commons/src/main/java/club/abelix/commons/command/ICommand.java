package club.abelix.commons.command;

import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import lombok.val;

import java.util.List;

public interface ICommand {

    String getName();

    String[] getAliases();

    default void execute(ICommandSender sender, String[] args) {
        val gamer = sender instanceof IGamer
                ? (IGamer) sender
                : null;

        execute(gamer, sender, args);
    }

    void execute(IGamer gamer, ICommandSender sender, String[] args);

    List<String> tabcomplete(ICommandSender sender, String[] args);

}
