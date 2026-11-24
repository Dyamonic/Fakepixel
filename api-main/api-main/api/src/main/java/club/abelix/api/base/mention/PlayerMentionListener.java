package club.abelix.api.base.mention;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.data.permission.Group;
import lombok.NonNull;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author artem
 */
public class PlayerMentionListener extends YListener {

    public PlayerMentionListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChatFormat(AsyncChatFormatEvent event) {
        String message = event.getMessage();

        Gamer sender = event.getSender();
        Gamer receiver = event.getReceiver();

        if (sender == receiver || receiver.isVanished()) {
            return;
        }

        String pattern = receiver.getPlayerName();

        if (sender.getRanks().hasPermission("chat.mention.everyone")) {
            pattern += "|@everyone";
        }

        Pattern namePattern = Pattern.compile("(?i)" + pattern);
        Matcher nameMatcher = namePattern.matcher(message);

        if (nameMatcher.find()) {
            String name = nameMatcher.group();
            boolean isEveryone = name.equalsIgnoreCase("@everyone");

            FalconAPI.getNmsApi().getSoundApi().playSound(receiver.getPlayer(), YSound.ORB_PICKUP, 1F, 2F);

            val group = sender.getShownGroup();

            event.setMessage(nameMatcher.replaceFirst((isEveryone ? "§c" : "§e")
                    + "§n" + name + group.getSuffix()));
        }
    }


}
