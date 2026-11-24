package club.abelix.api.base.util.chat;

import club.abelix.api.FalconAPI;
import lombok.NonNull;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Written by Vlundik for YAPI in 02 2016
 */
public class MessageSender {

    public static void sendMessage(@NonNull Player player, @NonNull String message) {
        player.sendMessage(message);
    }

    public static void sendTitle(@NonNull Player player, @NonNull String title, @NonNull String subtitle) {
        FalconAPI.getNmsApi().getPlayerApi().sendTitle(player, title, subtitle);
    }

    public static void resetTitle(@NonNull Player player) {
        FalconAPI.getNmsApi().getPlayerApi().resetTitle(player);
    }

    public static void sendTitle(@NonNull Player player, @NonNull String title, @NonNull String subtitle, int in, int stay, int out) {
        FalconAPI.getNmsApi().getPlayerApi().sendTitle(player, title, subtitle, in, stay, out);
    }

    public static void sendActionBar(@NonNull Player player, @NonNull String message) {
        FalconAPI.getNmsApi().getPlayerApi().sendActionBar(player, message);
    }

    public static void sendJsonMessage(@NonNull Player player, @NonNull String json) {
        player.spigot().sendMessage(ComponentSerializer.parse(json));
    }
}
