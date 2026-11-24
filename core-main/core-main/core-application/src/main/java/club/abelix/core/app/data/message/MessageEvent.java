package club.abelix.core.app.data.message;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class MessageEvent extends Event implements Cancellable {

    private final Gamer from;
    private final Gamer to;
    @Setter
    private String message;
    @Setter
    private boolean cancelled;

    public MessageEvent(@NonNull Gamer from, @NonNull Gamer to, @NonNull String msg) {
        this.from = from;
        this.to = to;
        this.message = msg;
    }

}
