package club.abelix.api.base.event.chat;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.event.Cancellable;

@Getter
public class AsyncChatFormatEvent extends YEvent implements Cancellable {

    private final Gamer sender;
    private final Gamer receiver;

    private final String originalFormat;

    @Setter
    private String format;

    @Setter
    private String message;

    @Getter
    private boolean formatModified;

    @Setter
    private boolean cancelled;

    public AsyncChatFormatEvent(Gamer sender, Gamer receiver, String originalFormat, String format, String message) {
        super(true);

        this.sender = sender;
        this.receiver = receiver;
        this.originalFormat = originalFormat;
        this.format = format;
        this.message = message;
    }

    public void appendFormat(@NonNull String format) {
        setFormat(format + this.format);
    }

    public void unsafeAppendFormat(@NonNull String format) {
        setFormat(format + this.format);
        this.formatModified = true;
    }
}
