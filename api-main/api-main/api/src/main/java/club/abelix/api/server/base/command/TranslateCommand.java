package club.abelix.api.server.base.command;

import club.abelix.api.base.chat.translation.ChatTranslationManager;
import club.abelix.api.base.chat.translation.TranslatableChatMessage;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;
import ru.whileinside.util.UrlEncoder;

import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
@NamedCommand("translate")
public class TranslateCommand {

    private static final UrlEncoder ENCODER = new UrlEncoder();

    @Executor
    @WithCondition("minArgs: 1")
    @WithCooldown(value = 15_000, currentServerOnly = false)
    public static void invoke(BukkitExecutionSession session) throws Exception {
        int id = session.getIntArgument(0, "ID");
        TranslatableChatMessage message = ChatTranslationManager.INSTANCE.findMessage(id);

        if (message == null) {
            throw new CommandTranslationException("translate_message_not_found");
        }

        Language language = message.getLanguage();
        Language senderLanguage = session.getLanguage();

        CompletableFuture<String> translated = message.getTranslated();

        if (language == senderLanguage) {
            translated.complete(message.getText());
        } else {
            translate(translated, message.getText(), language, senderLanguage);
        }

        translated.whenComplete((result, cause) -> {
            if (cause == null) {
                session.sendLocalizedMessage("translate_result_success", "%message%", result);
            } else {
                cause.printStackTrace();

                session.sendLocalizedMessage("translate_result_fail");
            }
        });
    }

    private static void translate(CompletableFuture<String> future, String message, Language from, Language to) {
        future.complete("Not implemented yet");

        // TODO: 05.09.2021 fix this
        /* YHttpRequest request = new YHttpRequest(HttpRequest.Type.GET,
                "https://fasttranslator.herokuapp.com/api/v1/text/to/text"
                        + "?source=" + ENCODER.encode(message)
                        + "&lang=" + from.getShortName() + "-" + to.getShortName());

        request.setConnectTimeout(1500);
        request.setReadTimeout(1500);

        request.sendRequest().whenComplete((response, cause) -> {
            if (cause != null) {
                future.completeExceptionally(cause);
            }

            try {
                future.complete(Json.defaultInstance().fromJson(response.getData()).asObject().getUnescapedString("data"));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }); */
    }
}
