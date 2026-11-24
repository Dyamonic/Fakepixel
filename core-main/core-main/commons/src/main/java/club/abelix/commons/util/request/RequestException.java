package club.abelix.commons.util.request;

import club.abelix.commons.command.exception.CommandTranslationException;
import lombok.Getter;

@Getter
public class RequestException extends CommandTranslationException {

    public RequestException(Object... messages) {
        super(messages);
    }

    public void sendMessage(AbstractRequest request) {
        printMessages(request.getReceiver());
    }

}
