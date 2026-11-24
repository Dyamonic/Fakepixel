package club.abelix.api.base.chat.mode;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CPlayerCommand;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatModeListener extends YListener {

    public ChatModeListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        // команды игнорируем
        if (message.startsWith("/")) {
            return;
        }

        Gamer gamer = Gamer.getGamer(event.getPlayer());

        if (gamer == null) {
            return;
        }

        ChatMode mode = gamer.getChatMode();

        String command = mode.getCommand();

        if (!command.isEmpty()) {
            String commandLine = "/" + command;

            // если нет аргументов, то она скорее всего не заканчивается на пробел
            // мы добавляем его сами
            if (command.split(" ").length == 1) {
                commandLine += " ";
            }

            Connection.sendPacket(new CPlayerCommand(gamer.getPlayerId(), commandLine + message));
            event.setCancelled(true);
        }
    }

}
