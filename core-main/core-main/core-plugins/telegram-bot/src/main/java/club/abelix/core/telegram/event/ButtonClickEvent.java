package club.abelix.core.telegram.event;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import com.pengrad.telegrambot.model.CallbackQuery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class ButtonClickEvent extends Event implements Cancellable {

    @Setter
    private boolean cancelled;

    private final long chatId;
    private final long userId;
    private final CallbackQuery callbackQuery;

}
