package club.abelix.core.app.api.request;

import club.abelix.commons.util.request.AbstractRequest;
import club.abelix.commons.util.request.RequestManager;
import club.abelix.commons.util.request.RequestType;
import club.abelix.core.app.Async;
import club.abelix.core.app.data.gamer.Gamer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author artem
 */
public abstract class CoreRequest extends AbstractRequest {

    protected Gamer sender;
    protected Gamer receiver;
    private ScheduledFuture<?> future;

    protected CoreRequest(RequestType requestType,
                          Gamer sender, Gamer receiver) {
        super(requestType, sender, receiver);

        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public void startWatcher() {
        this.future = Async.runLater(() -> {
            boolean b = RequestManager.IMP.removeRequest(this);

            if (b) {
                missRequest();
            }
        }, 1, TimeUnit.MINUTES);
    }

    @Override
    public void cancelWatcher() {
        if (future != null) {
            future.cancel(false);
        }
    }

}
