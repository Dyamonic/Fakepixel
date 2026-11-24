package club.abelix.commons.data.skin.exception;

import lombok.NonNull;

public abstract class SkinException extends Exception {

    protected SkinException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    protected SkinException(@NonNull String message) {
        super(message);
    }

    protected SkinException() {
    }

}
