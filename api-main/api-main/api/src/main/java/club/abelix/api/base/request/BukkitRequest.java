package club.abelix.api.base.request;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.util.request.AbstractRequest;
import club.abelix.commons.util.request.RequestManager;
import club.abelix.commons.util.request.RequestType;
import club.abelix.core.bukkit.HeartBeat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * @author WhileInside
 */
public abstract class BukkitRequest extends AbstractRequest {

    protected final Gamer sender;
    protected final Gamer receiver;
    protected final Player senderPlayer;
    protected final Player receiverPlayer;
    private BukkitTask task;

    protected BukkitRequest(RequestType requestType,
                            Gamer sender, Gamer receiver) {
        super(requestType, sender, receiver);

        this.senderPlayer = sender.getPlayer();
        this.receiverPlayer = receiver.getPlayer();

        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public void startWatcher() {
        task = HeartBeat.runLaterSync(() -> {
            boolean b = RequestManager.IMP.removeRequest(this);

            if (b) {
                missRequest();
            }
        }, 60 * 20);
    }

    @Override
    public void cancelWatcher() {
        if (task != null) {
            task.cancel();
        }
    }

}
