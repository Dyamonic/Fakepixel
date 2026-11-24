package club.abelix.core.app.command;

import club.abelix.commons.chat.ChatPublisherHolder;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.translate.api.TranslateLanguage;
import club.abelix.commons.translate.api.TranslateRequest;
import club.abelix.commons.translate.google.GoogleTranslateLanguage;
import club.abelix.commons.translate.google.GoogleTranslateRequestProcessor;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.language.LanguageUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import com.google.common.collect.ImmutableList;
import lombok.val;

import java.util.List;

@WithAlias("chatofdonaters")
@WithAlias("donatchat")
@WithAlias("dc")
@WithAlias("вс")
@NamedCommand("donatechat")
public final class DonateChatCommand {

    @Dependency
    private static ServerManager manager;

    private static final List<String> offChat = ImmutableList.of("off", "toggle", "disable", "enable", "on");

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("donatechat.access")
    @WithCondition("notBanned")
    @WithCondition("notMuted")
    public static void execute(final CoreExecutionSession session) {
        String message = session.getArgumentString();
        val sender = session.getSender();

        if (offChat.contains(message) && sender.isOnline()) {
            toggleDonateChat(session);
            return;
        }

        broadcastDonateMessage(session, message);
    }

    private static void toggleDonateChat(CoreExecutionSession session) {
        val sender = session.getSender();
        val gamer = session.getGamer();

        if (!gamer.hasPermission("donatechat.toggle")) {
            gamer.sendLocalizedMessage("no_permission");
            return;
        }

        boolean state = gamer.isSettingSet(Setting.DISABLE_DONATE_CHAT);

        sender.sendLocalizedMessage("donatechat_toggle", "%state%", Lang.of(state ? "disabled" : "enabled"));
        gamer.setSettingState(Setting.DISABLE_DONATE_CHAT, !state);
    }

    private static void broadcastDonateMessage(CoreExecutionSession session, String message) {
        val sender = session.getSender();

        if (sender.hasPermission("donatechat.colored")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        if (sender.isOnline()) {
            val gamer = session.getGamer();

            if (gamer.isSettingSet(Setting.DISABLE_DONATE_CHAT)) {
                sender.sendLocalizedMessage("donatechat_is_toggled");
                return;
            }
        }

        String displayName = sender.getDisplayName();

        var publisher = ChatPublisherHolder.get();
        if (publisher != null) {
            val gamer = session.getGamer();
            String serverName = (gamer != null && gamer.getServer() != null) ? gamer.getServer().getName() : "unknown";
            publisher.publish("donate", sender.getName(), serverName, message);
        }

        for (val gamer : Gamer.getGamers()) {
            if (!gamer.hasPermission("donatechat.access")) {
                continue;
            }

            if (gamer.isSettingSet(Setting.DISABLE_DONATE_CHAT) || !gamer.isActivePersonalChat()) {
                continue;
            }

            val translateLang = gamer.getProperty("Chat/Translate");

            if (translateLang != 0) {
                val language = Language.getById(translateLang);
                val detectedLanguage = LanguageUtil.detectLanguage(message);

                if (detectedLanguage != null && detectedLanguage.equals(language)) {
                    sendMessage(gamer, displayName, message);
                    continue;
                }

                val requestProcessor = GoogleTranslateRequestProcessor.create();

                val request = new TranslateRequest(
                        GoogleTranslateLanguage.AUTO,
                        TranslateLanguage.of(language.getShortName()),
                        message
                );

                requestProcessor.processRequest(request)
                        .thenAccept(response -> {
                            String translatedMessage =
                                    session.getArgumentString() + " §7-> " + response.result();

                            sendMessage(gamer, displayName, translatedMessage);
                        });

                continue;
            }

            sendMessage(gamer, displayName, message);
        }
    }

    private static void sendMessage(Gamer gamer, String displayName, String message) {
        gamer.sendLocalizedMessage(
                "core_donatechat_message_format",
                "%user%", displayName,
                "%message%", message
        );
    }
}
