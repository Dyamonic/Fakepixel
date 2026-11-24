package club.abelix.core.telegram.event;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class BotCommandEvent extends Event implements Cancellable {

    @Setter
    private boolean cancelled;

    private final BotUser user;
    private final BotCommand command;

}
