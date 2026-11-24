package club.abelix.core.app.event.gamer;

import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class CommandEvent extends Event implements Cancellable {

    private final Gamer sender;
    private final String command;
    private final String[] args;

    @Setter
    private boolean cancelled;

}
