package club.abelix.core.app.data.punishment.exception;

import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.util.array.ObjectLinkedBuf;

import java.util.List;

public class PunishmentException extends CommandTranslationException {

    public PunishmentException(Object... objects) {
        super(objects);
    }

    public PunishmentException(ObjectLinkedBuf... messages) {
        super(messages);
    }

    public PunishmentException(List<ObjectLinkedBuf> messages) {
        super(messages);
    }
}
