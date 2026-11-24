package club.abelix.core.app.event.gamer;

import club.abelix.commons.language.LanguageContext;
import club.abelix.core.app.api.event.Cancellable;
import club.abelix.core.app.api.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * В этом эвенте все сообщения которые отправляются через ServerManager#broadcastStaff
 * Все сообщения можно перопределить, а саму отправку отменить
 */

@Getter
@Setter
@RequiredArgsConstructor
public class StaffMessageEvent extends Event implements Cancellable {

    private final LanguageContext context;

    private boolean cancelled;

}
