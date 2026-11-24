package club.abelix.commons.util.request;

import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.util.ThrowableConsumer;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public abstract class RequestManager {

    @Dependency
    public static RequestManager IMP;
    private final Map<RequestData, AbstractRequest> requests = new HashMap<>();

    protected static void setImplementation(final @NonNull RequestManager implementation) {
        Preconditions.checkState(IMP == null, "Implementation already set");

        IMP = implementation;
    }

    private void findSent(@NonNull IGamer player, @NonNull RequestData data,
                          @NonNull ThrowableConsumer<AbstractRequest, RequestException> requestConsumer) {
        AbstractRequest found = requests.remove(data);

        if (found != null) {
            try {
                requestConsumer.accept(found);
            } catch (RequestException e) {
                e.printMessages(player);
            }

            found.cancelWatcher();
            return;
        }

        data.getType().sendNotSentMessage(player);
    }

    public void acceptRequest(@NonNull IGamer player, @NonNull RequestData data) {
        findSent(player, data, AbstractRequest::acceptRequest);
    }

    public void rejectRequest(@NonNull IGamer player, @NonNull RequestData data) {
        findSent(player, data, AbstractRequest::denyRequest);
    }

    public void sendRequest(@NonNull AbstractRequest request) {
        IGamer sender = request.getSender();
        IGamer receiver = request.getReceiver();

        if (sender == receiver) {
            sender.sendLocalizedMessage("request_sent_to_same_player");
            return;
        }

        RequestData data = request.getData();
        RequestType type = data.getType();

        if (type.isAcceptOnReceiverSend()) {
            // меняем местами сендера и рецивера
            RequestData reversed = data.reverse();

            if (requests.containsKey(reversed)) {
                findSent(request.getReceiver(), reversed, AbstractRequest::acceptRequest);

                return;
            }
        }

        if (requests.containsKey(data)) {
            type.sendAlreadySentMessage(sender);
            return;
        }

        request.startWatcher();
        request.sendRequest();

        requests.put(data, request);
    }

    protected void handleQuit(int playerId) {
        requests.entrySet().removeIf(entry -> {
            RequestData data = entry.getKey();

            return data.getSenderId() == playerId || data.getReceiverId() == playerId;
        });
    }

    public boolean removeRequest(AbstractRequest request) {
        return requests.remove(request.getData()) != null;
    }
}