package club.abelix.commons.command.exception;

import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.array.ObjectLinkedBuf;

import java.util.Collections;
import java.util.List;

public class CommandTranslationException extends CommandMessageException {

    private final ObjectLinkedBuf[] messages;

    public CommandTranslationException(Object... objects) {
        this(new ObjectLinkedBuf(objects));
    }

    public CommandTranslationException(ObjectLinkedBuf... messages) {
        super(Collections.emptyList());

        for (ObjectLinkedBuf message : messages) {
            if (message.size() % 2 != 1) {
                throw new IllegalStateException();
            }
        }

        this.messages = messages;
    }

    public CommandTranslationException(List<ObjectLinkedBuf> messages) {
        this(messages.toArray(new ObjectLinkedBuf[0]));
    }

    @Override
    public void printMessages(ExecutionSession session) {
        printMessages(session.getSender());
    }

    public void printMessages(ICommandSender sender) {
        sender.sendMessage(getMessage(sender.getLanguage()));
    }

    @Override
    public String getMessage() {
        return getMessage(Language.defaultLanguage());
    }

    public String getMessage(Language language) {
        StringBuilder builder = new StringBuilder();
        boolean appendNewLine = false;

        for (ObjectLinkedBuf array : messages) {
            if (appendNewLine) {
                builder.append("\n");
            } else {
                appendNewLine = true;
            }

            ObjectLinkedBuf.Reader reader = array.reader();

            String key = reader.readNext();

            Object[] params = new Object[array.size() - 1];
            reader.readNextObjects(params);

            builder.append(language.getMessage(key, params));
        }

        return builder.toString();
    }
}
