package club.abelix.core.velocity.command;

import club.abelix.commons.gamer.ICommandSender;
import club.abelix.core.velocity.StarshipVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public abstract class ProxiedCommandSender implements ICommandSender {

    @Override
    public String getName() {
        return getCommandSender() instanceof Player ? ((Player) getCommandSender()).getUsername() : "Console";
    }

    public abstract CommandSource getCommandSender();

    @Override
    public void sendMessage(final String s) {
        getCommandSender().sendMessage(Component.text(s));
    }

    @Override
    public void dispatchCommand(final String s) {
        StarshipVelocity.getInstance().getProxy().getCommandManager().executeAsync(getCommandSender(), s);
    }
}
