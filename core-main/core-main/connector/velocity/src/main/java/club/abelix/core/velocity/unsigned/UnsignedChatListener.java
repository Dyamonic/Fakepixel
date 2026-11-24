package club.abelix.core.velocity.unsigned;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.unsigned.event.UnsignedChatEvent;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import io.github._4drian3d.vpacketevents.api.event.PacketReceiveEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * @author milansky
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class UnsignedChatListener {

    static MethodHandles.Lookup SESSION_LOOKUP, KEYED_LOOKUP;
    static VarHandle SESSION_BOOL_HANDLE, SESSION_STRING_HANDLE;
    static VarHandle KEYED_BOOL_HANDLE, KEYED_STRING_HANDLE;

    static {
        try {
            SESSION_LOOKUP = MethodHandles.privateLookupIn(SessionPlayerChatPacket.class, MethodHandles.lookup());

            SESSION_BOOL_HANDLE = SESSION_LOOKUP.findVarHandle(SessionPlayerChatPacket.class, "signed", boolean.class);
            SESSION_STRING_HANDLE = SESSION_LOOKUP.findVarHandle(SessionPlayerChatPacket.class, "message", String.class);

            KEYED_LOOKUP = MethodHandles.privateLookupIn(KeyedPlayerChatPacket.class, MethodHandles.lookup());

            KEYED_BOOL_HANDLE = KEYED_LOOKUP.findVarHandle(KeyedPlayerChatPacket.class, "unsigned", boolean.class);
            KEYED_STRING_HANDLE = KEYED_LOOKUP.findVarHandle(KeyedPlayerChatPacket.class, "message", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    StarshipVelocity core;

    /**
     * Отключает подпись сообщений для игроков
     * с версии >= 1.19.3
     */
    @Subscribe
    @SneakyThrows
    public @Nullable EventTask onSessionPacketReceive(final @NotNull PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof final @NotNull SessionPlayerChatPacket chat)) return null;
        val player = event.getPlayer();
        val messageContent = chat.getMessage();

        SESSION_BOOL_HANDLE.set(chat, false);

        // Это команда или хуйня какая-то, игнорируем
        if (messageContent == null || messageContent.charAt(0) == '/') {
            return null;
        }

        val future = core.getProxy().getEventManager()
                .fire(new UnsignedChatEvent(player, chat.getMessage()))
                .thenAccept(unsignedEvent -> {
                    if (!unsignedEvent.getResult().isAllowed()) {
                        event.setResult(ResultedEvent.GenericResult.denied());
                        return;
                    }

                    unsignedEvent.getResult().getMessage()
                            .ifPresent(replaced -> SESSION_STRING_HANDLE.set(chat, replaced));
                });

        return EventTask.resumeWhenComplete(future);
    }

    /**
     * Отключает подпись сообщений для игроков
     * с версий 1.19, 1.19.1, 1.19.2
     */
    @Subscribe
    @SneakyThrows
    public @Nullable EventTask onKeyedPacketReceive(final @NotNull PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof final @NotNull KeyedPlayerChatPacket chat)) return null;
        val player = event.getPlayer();
        val messageContent = chat.getMessage();

        KEYED_BOOL_HANDLE.set(chat, true);

        // Это команда или хуйня какая-то, игнорируем
        if (messageContent == null || messageContent.charAt(0) == '/') {
            return null;
        }

        val future = core.getProxy().getEventManager()
                .fire(new UnsignedChatEvent(player, chat.getMessage()))
                .thenAccept(unsignedEvent -> {
                    if (!unsignedEvent.getResult().isAllowed()) {
                        event.setResult(ResultedEvent.GenericResult.denied());
                        return;
                    }

                    unsignedEvent.getResult().getMessage()
                            .ifPresent(replaced -> KEYED_STRING_HANDLE.set(chat, replaced));
                });

        return EventTask.resumeWhenComplete(future);
    }

    /**
     * Просто вызывает этот ивент
     * для игроков с версией клиента < 1.19
     */
    @Subscribe
    @SneakyThrows
    public @Nullable EventTask onLegacyPacketReceive(final @NotNull PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof final @NotNull LegacyChatPacket chat)) return null;
        val messageContent = chat.getMessage();

        // Это команда или хуйня какая-то, игнорируем
        if (messageContent == null || messageContent.charAt(0) == '/') {
            return null;
        }

        val player = event.getPlayer();
        val future = core.getProxy().getEventManager()
                .fire(new UnsignedChatEvent(player, messageContent))
                .thenAccept(unsignedEvent -> {
                    if (!unsignedEvent.getResult().isAllowed()) {
                        event.setResult(ResultedEvent.GenericResult.denied());
                        return;
                    }

                    unsignedEvent.getResult().getMessage().ifPresent(chat::setMessage);
                });

        return EventTask.resumeWhenComplete(future);
    }
}