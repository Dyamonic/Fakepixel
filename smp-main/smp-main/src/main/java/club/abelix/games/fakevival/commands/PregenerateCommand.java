package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.ChunkPregeneratorManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class PregenerateCommand implements CommandExecutor {
    private final ChunkPregeneratorManager manager;
    private final Game game;

    public PregenerateCommand(Game game,ChunkPregeneratorManager manager) {
        this.game = game;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!game.getNetworkService().isOverworld()){sender.sendMessage(CC.color(game.getNetworkService().isDungeon()?"&cPregen cannot be used on dungeon servers.":"&cPregen can only be used on overworld servers."));return true;}
        return manager.handle(sender, args, 0);
    }
}
