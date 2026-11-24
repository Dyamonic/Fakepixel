package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
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
import lombok.val;

@WithAlias("sc")
@WithAlias("ыс")
@WithAlias("adminchat")
@WithAlias("ac")
@WithAlias("officerchat")
@WithAlias("oc")
@NamedCommand("staffchat")
public final class StaffChatCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.staffchat")
    @WithCondition("notBanned")
    public static void execute(final CoreExecutionSession session) {
        val ranks = session.getRanks();

        String message = session.getArgumentString();

        if (session.getSender().hasPermission("command.staffchat.colored")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        //final Predicate<Gamer> disablePredicate = gamer -> !gamer.isHelper();

        val sender = session.getSender();
        String displayName = sender.getDisplayName();

        for (val gamer : Gamer.getGamers()) {
            if (!gamer.hasPermission("command.staffchat") || !gamer.isActivePersonalChat()) {
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
                "core_staffchat_message_format",
                "%user%", displayName,
                "%message%", message
        );
    }
}
