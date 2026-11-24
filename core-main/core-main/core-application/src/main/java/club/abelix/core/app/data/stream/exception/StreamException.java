package club.abelix.core.app.data.stream.exception;

import club.abelix.commons.command.exception.CommandTranslationException;
import lombok.NonNull;

public class StreamException extends CommandTranslationException {

    public StreamException(@NonNull String message) {
        super(message);
    }

}
