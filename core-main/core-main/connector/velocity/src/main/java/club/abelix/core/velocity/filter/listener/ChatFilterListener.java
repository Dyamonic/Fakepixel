package club.abelix.core.velocity.filter.listener;

import club.abelix.commons.util.string.StringUtil;
import club.abelix.core.velocity.chat.NatsChatPublisher;
import club.abelix.core.velocity.filter.ChatFilterManager;
import club.abelix.core.velocity.filter.data.Message;
import club.abelix.core.velocity.filter.data.MessageType;
import club.abelix.core.velocity.filter.violation.ViolationType;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.unsigned.event.UnsignedChatEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ChatFilterListener {

    /** Минимальный интервал между пакетами чата (мс). */
    private static final long PACKET_COOLDOWN_MS = 400;

    /** Минимальный средний интервал между сообщениями (мс). */
    private static final long CHAT_COOLDOWN_MS = 2_000;

    /** Порог схожести слов для детекции флуда (0..1). */
    private static final double FLOOD_SIMILARITY_THRESHOLD = 0.7;

    /** Сообщения старше этого TTL не участвуют в проверке флуда (мс). */
    private static final long FLOOD_WINDOW_MS = 60_000;

    /** Количество похожих сообщений в окне для срабатывания анти-флуда. */
    private static final int FLOOD_COUNT_THRESHOLD = 3;

    /** Префиксы серверов, где чат не логируется. */
    private static final String[] NON_LOGGED_SERVER_PREFIXES = {"limbo", "login"};

    private final ChatFilterManager chatFilterManager;
    private final NatsChatPublisher natsChatPublisher;

    @Subscribe
    public void onPlayerQuit(DisconnectEvent e) {
        chatFilterManager.removePlayerData(e.getPlayer());
    }

    @Subscribe
    public void onPlayerChat(UnsignedChatEvent e) {
        if (!e.getResult().isAllowed()) return;

        val player = e.getPlayer();
        val data   = chatFilterManager.getPlayerData(player);

        // --- rate limiting ---

        if (System.currentTimeMillis() - data.getLastTime() < PACKET_COOLDOWN_MS) {
            e.setResult(UnsignedChatEvent.ChatResult.denied());
            data.addVL(player, ViolationType.FAST_PACKETS);
            return;
        }

        if (data.messageCount() >= 2 && data.getAvgDelay() < CHAT_COOLDOWN_MS) {
            e.setResult(UnsignedChatEvent.ChatResult.denied());
            data.addVL(player, ViolationType.FAST_CHAT);
            return;
        }

        // --- flood detection ---

        String message  = e.getMessage();
        String[] words  = StringUtil.getWords(message);
        Gamer gamer     = Gamer.getGamer(player);

        if (gamer != null && gamer.hasPermission("chatfilter.punishment.bypass")) {
            pass(e, data, message, words);
            return;
        }

        int similarCount = 0;
        long now = System.currentTimeMillis();

        for (Message past : data.getMessages()) {
            if (now - past.time() > FLOOD_WINDOW_MS) continue;

            int shared = StringUtil.getSimilarCount(past.words(), words);
            if (shared > words.length * FLOOD_SIMILARITY_THRESHOLD) {
                if (++similarCount >= FLOOD_COUNT_THRESHOLD) {
                    e.setResult(UnsignedChatEvent.ChatResult.denied());
                    data.addVL(player, ViolationType.FLOOD);
                    return;
                }
            }
        }

        pass(e, data, message, words);
    }

    private void pass(UnsignedChatEvent e, club.abelix.core.velocity.filter.data.PlayerData data,
                      String message, String[] words) {
        e.setResult(UnsignedChatEvent.ChatResult.message(message));
        data.addMessage(chatFilterManager, new Message(System.currentTimeMillis(), words, MessageType.WORDS));

        val player = e.getPlayer();

        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        for (String prefix : NON_LOGGED_SERVER_PREFIXES) {
            if (server.startsWith(prefix)) {
                return;
            }
        }

        natsChatPublisher.publish("regular", player.getUsername(), server, message);
    }
}
