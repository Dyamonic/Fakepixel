package club.abelix.api.server.game.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.util.chat.MessageSender;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.game.GameGroupSelectorListener;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.api.server.game.team.selector.GameGroup;
import club.abelix.api.server.game.team.selector.SelectorGui;
import club.abelix.api.server.game.team.selector.TeamInviteRequest;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GameGroupSelectorManager {

    private final Game game;

    private final Set<TeamInviteRequest> invites = new HashSet<>();

    private final Map<Gamer, GameGroup> groups = new HashMap<>();
    private final Map<Party, GameGroup> partyGroups = new HashMap<>();

    public GameGroupSelectorManager(Game game) {
        this.game = game;
    }

    public void reset() {
        groups.clear();
        invites.clear();
        partyGroups.clear();
    }

    public void init() {
        game.registerListener(new GameGroupSelectorListener(game));

        HotbarManager.INSTANCE.registerIcon("GameLobby/" + game.getServerGameId() + "/private", 4, gamer -> ItemUtil.getBuilder(gamer.getSkull())
                .setName(Lang.of("team_selector_item_name"))
                .setLore(Lang.of("team_selector_item_lore"))
                .build(), gamer -> gamer.getPlayer().chat("/selector"));
    }

    public void onGameStart() {
        HotbarManager.INSTANCE.unregisterIcon("GameLobby/" + game.getServerGameId() + "/private", 4);

        game.removeListener(GameGroupSelectorListener.class);

        invites.forEach(TeamInviteRequest::cancel);
        invites.clear();
    }

    private void sendRequestMessage(Player player, String message,
                                    String acceptButton, String acceptDescription, String acceptCommand,
                                    String rejectButton, String rejectDescription, String rejectCommand) {
        // спизжено со скай блока
        MessageSender.sendJsonMessage(player,
                String.format(
                        "{\"text\":\"\\n%s\\n\\n\",\"extra\":[{\"text\":\"           \"},{\"text\":\"%s\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\"}}},{\"text\":\"           \"},{\"text\":\"%s\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"%s\"}}},{\"text\":\"\\n\"}]}",
                        message,
                        acceptButton, acceptCommand, acceptDescription,
                        rejectButton, rejectCommand, rejectDescription
                )
        );
    }

    public void sendInvite(Gamer sender, Gamer receiver) {
        if (invites.contains(new TeamInviteRequest(sender, receiver))) {
            sender.sendLocalizedMessage("team_selector_already_sent");
            return;
        }
        // если этот игрок уже отправил ему приглашение
        // то принимаем его
        boolean success = findRequest(true, receiver, sender, this::acceptInvite);

        // мы приняли приглашение, нет смысла отправлять приглашение
        if (success) {
            return;
        }

        Language language = receiver.getLanguage();

        TeamInviteRequest invite = new TeamInviteRequest(sender, receiver);
        invite.startWatcher(game.getPlugin(), this);

        sender.sendLocalizedMessage("team_selector_sent", "%name%", receiver.getDisplayName());

        sendRequestMessage(
                receiver.getPlayer(), language.getMessage("team_selector_invite_message", "%name%", sender.getDisplayName()),
                language.getMessage("accept_request_button"), language.getMessage("accept_request_description"),
                "/selector accept " + sender.getName(),
                language.getMessage("reject_request_button"), language.getMessage("reject_request_description"),
                "/selector reject " + sender.getName()
        );

        invites.add(invite);
    }

    public void updateGuiExclude(Collection<Gamer> gamers) {
        Collection<Gamer> allGamers = Gamer.getGamers();

        if (!gamers.isEmpty()) {
            allGamers.removeAll(gamers);
        }

        for (Gamer gamer : allGamers) {
            Player player = gamer.getPlayer();

            // достаём открытый инвентарь
            InventoryView view = player.getOpenInventory();

            if (view != null && view.getTopInventory().getHolder() instanceof SelectorGui) {
                SelectorGui gui = (SelectorGui) view.getTopInventory().getHolder();

                // обновляем инвентарь
                gui.draw();
            }
        }

    }

    public void onLeave(@NonNull Gamer gamer) {
        invites.removeIf(invite -> invite.getSender() == gamer || invite.getReceiver() == gamer);

        GameGroup group = groups.remove(gamer);

        if (group != null) {
            group.removePlayer(gamer);
        }
    }

    /**
     * @param silent - тихий режим (т.е не пишем сообщение)
     * @return true if failed
     */
    private boolean findRequest(boolean silent, Gamer sender, Gamer receiver, Consumer<TeamInviteRequest> callback) {
        boolean success = invites.removeIf(invite -> {
            if (invite.getSender() == sender && invite.getReceiver() == receiver) {
                callback.accept(invite);
                return true;
            }

            return false;
        });

        if (!success && !silent) {
            receiver.sendLocalizedMessage("team_selector_request_not_found");
        }

        return success;
    }


    private void acceptInvite(@NonNull TeamInviteRequest request) {
        Gamer sender = request.getSender();
        Gamer receiver = request.getReceiver();

        GameGroup senderGroup = getGroup(sender);
        GameGroup receiverGroup = getGroup(receiver);

        if (receiver.hasParty()) {
            receiver.sendLocalizedMessage("team_selector_cant_accept_party");
            return;
        }

        if (receiverGroup.getGamers().size() != 1) {
            receiver.sendLocalizedMessage("team_selector_cant_accept_team");
            return;
        }

        if (senderGroup.getGamers().size() >= game.getSetting(GameSetting.PLAYERS_PER_TEAM, 1)) {
            receiver.sendLocalizedMessage("team_selector_cant_accept_full");
            return;
        }

        // добавляем тиммейта
        senderGroup.addPlayer(receiver);
        groups.put(receiver, senderGroup);

        // пишем сообщения
        sender.sendLocalizedMessage("team_selector_accept_sender", "%name%", receiver.getDisplayName());
        receiver.sendLocalizedMessage("team_selector_accept_receiver", "%name%", sender.getDisplayName());

        request.cancel();

        Gui.forEachOpened(SelectorGui.class, SelectorGui::draw);
    }

    public void acceptInvite(Gamer sender, Gamer receiver) {
        findRequest(false, sender, receiver, this::acceptInvite);
    }

    public void denyInvite(Gamer sender, Gamer receiver) {
        findRequest(false, sender, receiver, request -> {
            sender.sendLocalizedMessage("team_selector_reject_sender", receiver.getDisplayName());
            receiver.sendLocalizedMessage("team_selector_reject_receiver", sender.getDisplayName());

            request.cancel();
        });
    }

    public GameGroup getGroup(@NonNull Gamer gamer) {
        return groups.get(gamer);
    }

    private GameGroup getNewGroup(@NonNull Gamer gamer, boolean forceSingleton) {
        Party party = gamer.getParty();

        if (party == null || forceSingleton) {
            return new GameGroup(game, gamer);
        }

        GameGroup group = partyGroups.compute(party, (x, gameGroup) -> {
            if (gameGroup != null) {
                gameGroup.addPlayer(gamer);

                return gameGroup;
            }

            return new GameGroup(game, gamer);
        });

        if (party.isLeader(gamer)) {
            group.setLeader(gamer);
        }

        if (group.getGamerCount() == game.getPlayersPerTeam()) {
            partyGroups.remove(party);
        }

        return group;
    }

    public GameGroup addGroup(@NonNull Gamer gamer, boolean forceSingleton) {
        return groups.put(gamer, getNewGroup(gamer, forceSingleton));
    }

    public boolean hasGroup(@NonNull Gamer gamer) {
        return groups.containsKey(gamer);
    }

    public void resetGroup(@NonNull Gamer gamer) {
        GameGroup ex = addGroup(gamer, true);

        if (ex != null) {
            ex.removePlayer(gamer);

            if (ex.getLeader() == gamer) {
                new ArrayList<>(ex.getGamers()).forEach(this::resetGroup);
            }

            Player player = gamer.getPlayer();
            InventoryView view = player.getOpenInventory();

            if (view != null && view.getTopInventory().getHolder() instanceof SelectorGui) {
                SelectorGui gui = (SelectorGui) view.getTopInventory().getHolder();

                // обновляем группу и обновляем инвентарь
                gui.updateGroup();
                gui.draw();
            }
        }
    }

    public boolean removeInvite(TeamInviteRequest request) {
        return invites.remove(request);
    }

    /**
     * Возвращает массив групп, в которых в начале будет самое большее количество игроков
     */
    public GameGroup[] getSortedGroups() {
        return groups.values().stream().distinct().sorted(Comparator.reverseOrder())
                .toArray(GameGroup[]::new);
    }
}
