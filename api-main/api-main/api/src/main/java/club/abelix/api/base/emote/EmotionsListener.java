package club.abelix.api.base.emote;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.permission.Group;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EmotionsListener extends YListener {

    public EmotionsListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public static void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null) return;

        if (!gamer.getRanks().hasPermission("lobby.emotions")) return;

        FakePlayerNickname nickname = gamer.getFakeName();

        // если игрок никнутый не как элита и не как персонал, то у него нет прав на эмодзи
        if (nickname != null && nickname.getGroup().hasPermission("lobby.emotions") && !nickname.getGroup().hasPermission("core.staff")) {
            return;
        }

        // пишем инфу о том что игроку это доступно, если еще никогда не писали
        if (gamer.getProperty("Emotions/Promotion") == 0) {
            gamer.sendLocalizedMessage("emotion_in_chat_available");
            gamer.setProperty("Emotions/Promotion", 1);
        }

        val group = gamer.getShownGroup();

        String message = event.getMessage();

        String[] words;

        if (message.startsWith("!")) {
            words = message.substring(1).split(" ");
        } else {
            words = message.split(" ");
        }

        for (String word : words) {
            EmotionType type = EmotionType.findEmotion(word);

            if (type != null) {
                message = message.replace(word, type.getReplaceTo() + group.getSuffix());
            }
        }

        event.setMessage(message);
    }

}
