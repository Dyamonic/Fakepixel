package club.abelix.api.base.command;

import club.abelix.commons.gamer.ICommandSender;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class ProxiedCommandSender implements ICommandSender {

    @Override
    public String getName() {
        return getCommandSender().getName();
    }

    public abstract CommandSender getCommandSender();

    @Override
    public void sendRawMessage(String s) {
        if (getCommandSender() instanceof Player) {
            Player player = (Player) getCommandSender();
            player.spigot().sendMessage(ComponentSerializer.parse(s));
        } else sendMessage(s);
    }

    @Override
    public void sendMessage(String s) {
        getCommandSender().sendMessage(s);
    }

    @Override
    public void dispatchCommand(String s) {
        Bukkit.dispatchCommand(getCommandSender(), s);
    }
}
