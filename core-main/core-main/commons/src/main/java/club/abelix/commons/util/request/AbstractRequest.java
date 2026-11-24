package club.abelix.commons.util.request;

import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class AbstractRequest {

    private final IGamer sender;
    private final IGamer receiver;
    protected RequestData data;

    protected AbstractRequest(RequestType requestType, IGamer sender, IGamer receiver) {
        this.data = new RequestData(requestType, sender.getPlayerId(), receiver.getPlayerId());
        this.sender = sender;
        this.receiver = receiver;
    }

    public abstract void startWatcher();

    public abstract void cancelWatcher();

    private void sendRequestMessage(@NonNull IGamer player, @NonNull String message,
                                    @NonNull String acceptCommand, @NonNull String denyCommand) {
        player.sendRawMessage(String.format("{\"text\":\"\\n%s\\n\\n\",\"extra\":[{\"text\":\"           \"},{\"text\":\"§a[ACCEPT]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"§7Click, to accept\"}}},{\"text\":\"           \"},{\"text\":\"§c[DENY]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"§7Click, for deny\"}}},{\"text\":\"\\n\"}]}",
                message, acceptCommand, denyCommand));
    }

    protected ObjectLinkedBuf getSendTranslations() {
        return ObjectLinkedBuf.EMPTY;
    }

    public void sendRequest() {
        ObjectLinkedBuf translations = getSendTranslations();

        RequestType type = data.getType();

        // пишем сообщения
        sendRequestMessage(receiver,
                type.getReceiverSentMessage(receiver.getLanguage(), sender, translations),
                type.getAcceptCommand(sender.getName()),
                type.getDenyCommand(sender.getName())
        );

        sender.sendMessage(type.getSenderSentMessage(sender.getLanguage(), receiver, translations));
    }

    public void acceptRequest() throws RequestException {
        try {
            accept();
            data.getType().sendAcceptMessage(sender, receiver, getSendTranslations());
        } finally {
            cancelWatcher();
        }
    }

    public void denyRequest() {
        data.getType().sendRejectMessage(sender, receiver, getSendTranslations());
        cancelWatcher();
    }

    public void missRequest() {
        data.getType().sendMissMessage(sender, receiver, getSendTranslations());
    }

    protected abstract void accept() throws RequestException;

}
