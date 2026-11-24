package club.abelix.commons.util.request;

import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class RequestType {

    /**
     * Имя запроса, используется для перевода
     */
    private final String name;

    /**
     * Команда, которая выполнится, если игрок нажмёт ЛКМ по кнопке принять.
     * %s заменяется на ник отправителя
     */
    private final String acceptCommand;

    /**
     * Команда, которая выполнится, если игрок нажмёт ЛКМ по кнопке отклонить.
     * %s заменяется на ник отправителя
     */
    private final String denyCommand;

    /**
     * Если получатель отправит тот же самый запрос отправителю,
     * то получатель автоматически примет предыдущий
     */
    private final boolean acceptOnReceiverSend;

    public RequestType(String name, String acceptCommand, String denyCommand, boolean acceptOnReceiverSend) {
        this.name = name;
        this.acceptCommand = acceptCommand;
        this.denyCommand = denyCommand;
        this.acceptOnReceiverSend = acceptOnReceiverSend;
    }

    public String getName() {
        return name;
    }

    public String getAcceptCommand(String sender) {
        return String.format(acceptCommand, sender);
    }

    public String getDenyCommand(String sender) {
        return String.format(denyCommand, sender);
    }

    public boolean isAcceptOnReceiverSend() {
        return acceptOnReceiverSend;
    }

    /**
     * Отправить сообщение об ошибке, когда запрос не найден
     */
    public void sendNotSentMessage(@NonNull IGamer gamer) {
        gamer.sendLocalizedMessage("request_" + name + "_not_sent_message");
    }

    /**
     * Отправить сообщение о принятии запроса
     */
    public void sendAcceptMessage(@NonNull IGamer sender, @NonNull IGamer receiver, ObjectLinkedBuf translations) {
        ObjectLinkedBuf snd = new ObjectLinkedBuf(translations.toArray());
        snd.writer().writeNext("%name%", receiver.getDisplayName());
        ObjectLinkedBuf rcv = new ObjectLinkedBuf(translations.toArray());
        rcv.writer().writeNext("%name%", sender.getDisplayName());

        sender.sendLocalizedMessage("request_" + name + "_accept_sender", snd.toArray());
        receiver.sendLocalizedMessage("request_" + name + "_accept_receiver", rcv.toArray());
    }

    /**
     * Отправить сообщение об истечении срока действия запроса
     */
    public void sendMissMessage(@NonNull IGamer sender, @NonNull IGamer receiver, @NotNull ObjectLinkedBuf translations) {
        ObjectLinkedBuf snd = new ObjectLinkedBuf(translations.toArray());
        snd.writer().writeNext("%name%", receiver.getDisplayName());
        ObjectLinkedBuf rcv = new ObjectLinkedBuf(translations.toArray());
        rcv.writer().writeNext("%name%", sender.getDisplayName());

        sender.sendLocalizedMessage("request_" + name + "_miss_sender", snd.toArray());
        receiver.sendLocalizedMessage("request_" + name + "_miss_receiver", rcv.toArray());
    }

    /**
     * Отправить сообщение о том, что запрос отклонен
     */
    public void sendRejectMessage(@NonNull IGamer sender, @NonNull IGamer receiver, @NotNull ObjectLinkedBuf translations) {
        ObjectLinkedBuf snd = new ObjectLinkedBuf(translations.toArray());
        snd.writer().writeNext("%name%", receiver.getDisplayName());
        ObjectLinkedBuf rcv = new ObjectLinkedBuf(translations.toArray());
        rcv.writer().writeNext("%name%", sender.getDisplayName());

        sender.sendLocalizedMessage("request_" + name + "_reject_sender", snd.toArray());
        receiver.sendLocalizedMessage("request_" + name + "_reject_receiver", rcv.toArray());
    }

    /**
     * Отправить сообщение, когда запрос отправлен (для отправителя)
     */
    public String getReceiverSentMessage(@NonNull Language language, @NonNull IGamer sender,
                                         @NonNull ObjectLinkedBuf translations) {
        ObjectLinkedBuf buf = new ObjectLinkedBuf(translations.toArray());
        buf.writer().writeNext("%name%", sender.getDisplayName());

        return language.getMessage("request_" + name + "_sent_receiver", buf.toArray());
    }

    /**
     * Отправить сообщение, когда запрос отправлен (для получателя)
     */
    public String getSenderSentMessage(@NonNull Language language, @NonNull IGamer receiver,
                                       @NotNull ObjectLinkedBuf translations) {
        ObjectLinkedBuf buf = new ObjectLinkedBuf(translations.toArray());
        buf.writer().writeNext("%name%", receiver.getDisplayName());

        return language.getMessage("request_" + name + "_sent_sender", buf.toArray());
    }

    /**
     * Отправить сообщение об ошибке, когда запрос уже отправлен
     */
    public void sendAlreadySentMessage(@NonNull IGamer gamer) {
        gamer.sendLocalizedMessage("request_" + name + "_already_sent");
    }
}
