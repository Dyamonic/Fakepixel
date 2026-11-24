package club.abelix.core.velocity.unsigned.event;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Optional;

/**
 * @author milansky
 */
@Setter
@Getter
@ToString
@AwaitingEvent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class UnsignedChatEvent implements ResultedEvent<UnsignedChatEvent.ChatResult> {

    final Player player;
    final String message;
    ChatResult result = ChatResult.ALLOWED;

    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static final class ChatResult implements Result {
        private static final ChatResult ALLOWED = new ChatResult(null, true);
        private static final ChatResult DENIED = new ChatResult(null, false);

        String message;
        final boolean status;

        public Optional<String> getMessage() {
            return Optional.ofNullable(this.message);
        }

        public boolean isAllowed() {
            return this.status;
        }

        public String toString() {
            return this.status ? "allowed" : "denied";
        }

        public static ChatResult allowed() {
            return ALLOWED;
        }

        public static ChatResult denied() {
            return DENIED;
        }

        public static ChatResult message(final @NonNull String message) {
            return new ChatResult(message, true);
        }
    }
}
