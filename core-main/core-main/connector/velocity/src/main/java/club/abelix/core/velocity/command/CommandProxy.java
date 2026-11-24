package club.abelix.core.velocity.command;

import club.abelix.commons.command.ICommand;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.val;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record CommandProxy(ICommand origin) implements SimpleCommand {

    private static final long COOLDOWN_MS = 5_000L;

    @Override
    public void execute(Invocation invocation) {
        val gamer = Gamer.getGamer((Player) invocation.source());

        origin.execute(
                invocation.source() instanceof Player ?
                        gamer :
                        new ProxiedConsoleCommandSender(invocation.source()),
                invocation.arguments()
        );
    }

    private static long secondsCeil(long ms) {
        return (ms + 999L) / 1000L;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return SimpleCommand.super.suggestAsync(invocation);
    }

}

