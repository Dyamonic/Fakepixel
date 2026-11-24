package club.abelix.core.app.data.friend;

import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.data.friend.FriendSqlHandler;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.core.app.Async;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.data.friend.event.FriendAdd;
import club.abelix.core.app.data.friend.event.FriendRemove;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.play.server.SFriendAction;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class FriendManager {

    public static final FriendManager INSTANCE = new FriendManager();

    //Отправитель, Получатель
    private final Multimap<Gamer, Gamer> INVITES_FROM = CollectionUtil.createConcurrentSetMultimap();

    //Получатель, Отправитель
    private final Multimap<Gamer, Gamer> INVITES_TO = CollectionUtil.createConcurrentSetMultimap();

    public Collection<Gamer> getInvitesFrom(Gamer sender) {
        return INVITES_FROM.get(sender);
    }

    public Collection<Gamer> getInvitesTo(Gamer gamer) {
        return INVITES_TO.get(gamer);
    }

    public void addInvite(Gamer sender, Gamer receiver) {
        INVITES_FROM.put(sender, receiver);
        INVITES_TO.put(receiver, sender);
    }

    /**
     * @param sender   отправитель
     * @param receiver получатель
     */
    public boolean removeInvite(Gamer sender, Gamer receiver) {
        boolean removedFirst = INVITES_FROM.remove(sender, receiver);
        boolean removedSecond = INVITES_TO.remove(receiver, sender);

        return removedFirst || removedSecond;
    }

    public void removePlayer(Gamer gamer) {
        Collection<Gamer> senders = INVITES_TO.removeAll(gamer);   //Отправившие нам инвайты
        Collection<Gamer> receivers = INVITES_FROM.removeAll(gamer); //Получившие наши инвайты

        if (senders != null) {
            for (Gamer sender : senders) {
                sender.sendLocalizedMessage("friend_invitation_to_receiver_reject_by_quit", "%name%", gamer.getDisplayName());

                INVITES_FROM.remove(sender, gamer);
            }
        }

        if (receivers != null) {
            for (Gamer receiver : receivers) {
                receiver.sendLocalizedMessage("friend_invitation_from_sender_reject_by_quit", "%name%", gamer.getDisplayName());

                INVITES_TO.remove(receiver, gamer);
            }
        }
    }

    /**
     * Крч, sender -> receiver запрос в други. Чекаем, есть ли
     * запрос receiver -> sender. Если есть, то отменяем нахуй
     * делающийся запрос и удаляем диаметральный, добавляем
     * в друзья. Иначе оставляем запрос на добавление в ДР.
     *
     * @param sender   игрок, который кидает invite
     * @param receiver игрок, который принимает invite от sender
     */
    public void createInvite(Gamer sender, Gamer receiver) throws CommandTranslationException {
        Collection<Gamer> receivers = getInvitesFrom(receiver); //Получатели от Gamer

        if (receivers.contains(sender)) {
            removeInvite(receiver, sender);

            FriendSqlHandler.INSTANCE.addFriend(receiver.getPlayerId(), sender.getPlayerId());

            receiver.addFriend(sender.getPlayerId());
            sender.addFriend(receiver.getPlayerId());

            CoreApplication.getInstance().getPluginManager().callEvent(new FriendAdd(sender, receiver));

            sender.sendToServer(new SFriendAction(sender.getPlayerId(), receiver.getPlayerId(), SFriendAction.Action.ADD_FRIEND));
            receiver.sendToServer(new SFriendAction(receiver.getPlayerId(), sender.getPlayerId(), SFriendAction.Action.ADD_FRIEND));

            receiver.sendLocalizedMessage("friend_accept", "%name%", sender.getDisplayName());
            sender.sendLocalizedMessage("friend_accept", "%name%", receiver.getDisplayName());

            return;
        }

        Collection<Gamer> invites = getInvitesFrom(sender); //Получатели от sender

        if (invites.contains(receiver)) {
            throw new CommandTranslationException("friend_invitation_already_sent", "%name%", receiver.getDisplayName());
        }

        if (receiver.isSettingSet(Setting.FORBID_FRIEND_REQUESTS)) {
            throw new CommandTranslationException("friend_disabled_invitation_sent", "%name%", receiver.getDisplayName());
        }

        if (receiver.isIgnored(sender.getPlayerId())) {
            sender.sendLocalizedMessage("ignored_player_action", "%name%", receiver.getDisplayName());
            return;
        }

        addInvite(sender, receiver);

        Language receiverLang = receiver.getLanguage();

        String msg =
                String.format(
                        "{\"text\":\"\\n%s\\n\\n\",\"extra\":[{\"text\":\"           \"},{\"text\":\"%s\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/friend %s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\"}}},{\"text\":\"           \"},{\"text\":\"%s\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/friend reject %s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\"}}},{\"text\":\"\\n\"}]}",
                        receiverLang.getMessage("friend_invitation", "%name%", sender.getDisplayName()),
                        receiverLang.getMessage("accept_request_button"), sender.getName(), receiverLang.getMessage("friend_accept_description"),
                        receiverLang.getMessage("reject_request_button"), sender.getName(), receiverLang.getMessage("friend_reject_description")
                ).replace("\n", "\\n");

        receiver.sendMessage(msg);
        sender.sendLocalizedMessage("friend_invitation_sent");

        Async.runLater(() -> {
            if (!removeInvite(sender, receiver)) {
                return; //Уже удалено
            }

            if (!sender.isOnline()) {
                return;
            }

            if (!receiver.isOnline()) {
                return;
            }

            receiver.sendLocalizedMessage("friend_invitation_expired_receiver", "%name%", sender.getDisplayName());
            sender.sendLocalizedMessage("friend_invitation_expired_sender", "%name%", receiver.getDisplayName());
        }, 1L, TimeUnit.MINUTES);
    }

    public void removeFriend(Gamer gamer, Gamer friend) {
        FriendSqlHandler.INSTANCE.removeFriend(gamer.getPlayerId(), friend.getPlayerId());

        gamer.removeFriend(friend.getPlayerId());
        friend.removeFriend(gamer.getPlayerId());

        if (friend.isOnline()) {
            friend.sendLocalizedMessage("friend_remove_receiver", "%name%", gamer.getDisplayName());
            friend.sendToServer(new SFriendAction(friend.getPlayerId(), gamer.getPlayerId(), SFriendAction.Action.REMOVE_FRIEND));
        }

        if (gamer.isOnline()) {
            gamer.sendLocalizedMessage("friend_remove_sender", "%name%", friend.getDisplayName());
            gamer.sendToServer(new SFriendAction(gamer.getPlayerId(), friend.getPlayerId(), SFriendAction.Action.REMOVE_FRIEND));
        }

        CoreApplication.getInstance().getPluginManager().callEvent(new FriendRemove(gamer, friend));
    }

    /**
     * @param receiver игрок, который отменяет invite от sender
     * @param sender   игрок, который кинул invite
     */
    public void rejectInvite(Gamer receiver, Gamer sender) throws CommandMessageException {
        Collection<Gamer> senders = getInvitesTo(receiver);

        if (!senders.contains(sender)) {
            throw new CommandTranslationException("friend_invitation_not_found", "%name%", sender.getDisplayName());
        }

        removeInvite(sender, receiver);

        receiver.sendLocalizedMessage("friend_invitation_rejected_receiver", "%name%", sender.getDisplayName());
        sender.sendLocalizedMessage("friend_invitation_rejected_sender", "%name%", receiver.getDisplayName());
    }
}
