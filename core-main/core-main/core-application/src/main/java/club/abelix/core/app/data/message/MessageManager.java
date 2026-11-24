package club.abelix.core.app.data.message;

import club.abelix.commons.chat.ChatPublisherHolder;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Language;
import club.abelix.commons.translate.api.TranslateLanguage;
import club.abelix.commons.translate.api.TranslateRequest;
import club.abelix.commons.translate.google.GoogleTranslateLanguage;
import club.abelix.commons.translate.google.GoogleTranslateRequestProcessor;
import club.abelix.commons.util.language.LanguageUtil;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MessageManager implements Listener {

    public static final MessageManager INSTANCE = new MessageManager();
    private static final String MESSAGE = "{" +
            "\"text\":\"%s\"," +
            "\"clickEvent\":{" +
            "\"action\":\"suggest_command\"," +
            "\"value\":\"/msg %s \"" +
            "}," +
            "\"hoverEvent\":{" +
            "\"action\":\"show_text\"," +
            "\"value\":\"%s\"" +
            "}" +
            "}";
    private final Map<Gamer, Gamer> lastFrom = new ConcurrentHashMap<>(); //sender, receiver
    private final Map<Gamer, Gamer> lastTo = new ConcurrentHashMap<>();   //receiver, sender

    private void setLastRecipient(@NonNull Gamer from, @NonNull Gamer to) {
        lastFrom.put(from, to);
        lastTo.put(to, from);
    }

    public Gamer getLastFrom(@NonNull Gamer gamer) {
        return lastFrom.get(gamer);
    }

    public Gamer getLastTo(@NonNull Gamer gamer) {
        return lastTo.get(gamer);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onUnload(@NonNull PlayerQuitEvent event) {
        Gamer gamer = event.getGamer();

        lastFrom.remove(gamer);
        lastFrom.values().remove(gamer);

        lastTo.remove(gamer);
        lastTo.values().remove(gamer);
    }

    private String getMessage(Language language, String name, String from, String to, boolean sending, String message) {
        return String.format(
                MESSAGE,

                getMessage(language, from, to, message), name,
                language.getMessage(
                        "message_" + (sending ? "send" : "receive") + "_hover",
                        "%name%", name
                )
        );
    }

    private String getMessage(Language language, String from, String to, String message) {
        return language.getMessage("message_format", "%from%", from, "%to%", to, "%message%",
                message.replace("\\", "\\\\"));
    }

    public void processMessage(Gamer from, Gamer to, String msg) {
        if (msg == null || msg.indexOf('卐') >= 0 || msg.indexOf('卍') >= 0) {
            try {
                PunishmentManager.INSTANCE.processBan(ConsoleCommandSender.INSTANCE, from.getName(), "Forbidden characters in message", -1L);
            } catch (PunishmentException e) {
                throw new RuntimeException(e);
            }
            log.info("Detected forbidden characters in message from {} to {}: {}", from.getName(), to.getName(), msg);
            return;
        }

        if (!from.isFriend(to.getPlayerId()) && !from.getRanks().isStaff()) {
            if (to.isSettingSet(Setting.FORBID_PRIVATE_MESSAGES)) {
                from.sendLocalizedMessage("message_send_disabled", "%name%", to.getDisplayName());

                return;
            }
        }

        if (to.isIgnored(from.getPlayerId())) {
            from.sendLocalizedMessage("ignored_player_action", "%name%", to.getDisplayName());
            return;
        }

        processReply(from, to, msg);
    }

    public void processReply(Gamer from, Gamer to, String msg) {
        if (msg == null || msg.indexOf('卐') >= 0 || msg.indexOf('卍') >= 0) {
            try {
                PunishmentManager.INSTANCE.processBan(ConsoleCommandSender.INSTANCE, from.getName(), "Forbidden characters in message", -1L);
            } catch (PunishmentException e) {
                throw new RuntimeException(e);
            }
            log.info("Detected forbidden characters in message from {} to {}: {}", from.getName(), to.getName(), msg);
            return;
        }

        setLastRecipient(from, to);

        if (msg.endsWith("[shuffled]")) {
            return;
        }

        var publisher = ChatPublisherHolder.get();
        if (publisher != null) {
            String serverName = (from.getServer() != null) ? from.getServer().getName() : "unknown";
            publisher.publish("private", from.getName(), serverName, msg);
        }

        // social spy off for dev+
        if (from.hasPermission("socialspy.access")) {
            return;
        }

        for (Gamer gamer : Gamer.getGamers()) {
            if (gamer.getProperty("SocialSpy") == 1) {
                gamer.sendMessage(getMessage(gamer.getLanguage(), from.getDisplayName(), to.getDisplayName(), msg));
            }
        }

        from.sendMessage(getMessage(from.getLanguage(),
                to.getName(), "Me", to.getDisplayName(), true, msg));

        val translateLang = to.getProperty("Chat/Translate");

        if (translateLang != 0) {
            val language = Language.getById(translateLang);
            val detectedLanguage = LanguageUtil.detectLanguage(msg);

            if (detectedLanguage != null && detectedLanguage.equals(language)) {
                to.sendMessage(getMessage(to.getLanguage(),
                        from.getName(), from.getDisplayName(), "Me", false, msg));
                return;
            }

            val requestProcessor = GoogleTranslateRequestProcessor.create();

            val request = new TranslateRequest(
                    GoogleTranslateLanguage.AUTO,
                    TranslateLanguage.of(language.getShortName()),
                    msg
            );

            requestProcessor.processRequest(request)
                    .thenAccept(response -> {
                        String translatedMessage =
                                msg + " §7-> " + response.result();

                        to.sendMessage(getMessage(to.getLanguage(),
                                from.getName(), from.getDisplayName(), "Me", false, translatedMessage));
                    });

            return;
        }

        to.sendMessage(getMessage(to.getLanguage(),
                from.getName(), from.getDisplayName(), "Me", false, msg));
    }

}
