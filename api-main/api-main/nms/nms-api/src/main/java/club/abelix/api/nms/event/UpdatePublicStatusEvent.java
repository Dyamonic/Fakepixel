package club.abelix.api.nms.event;

import club.abelix.api.nms.packet.IVisibleObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class UpdatePublicStatusEvent extends Event {

    private final IVisibleObject object;
    private final boolean publicStatus;

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }


}
