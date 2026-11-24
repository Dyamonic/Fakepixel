package club.abelix.api.server.lobby.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.division.status.listener.DivisionStatusListener;
import club.abelix.api.base.event.friend.FriendAddEvent;
import club.abelix.api.base.event.friend.FriendRemoveEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerSettingToggleEvent;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.party.event.PartyInviteEvent;
import club.abelix.api.base.party.event.PartyKickEvent;
import club.abelix.api.server.lobby.event.PlayerToggleHideEvent;
import club.abelix.commons.data.setting.Setting;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class LobbyHiderListener extends YListener {

    public LobbyHiderListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    private static boolean isHideEnabled(@NonNull Gamer gamer) {
        return gamer.isSettingSet(Setting.HIDE_PLAYERS);
    }

    private static boolean isSameParty(@NonNull Gamer gamer, @NonNull Gamer other) {
        return gamer.isSameParty(other.getParty());
    }

    private static boolean isFriends(@NonNull Gamer gamer, @NonNull Gamer other) {
        return gamer.isFriend(other);
    }

    private static void hidePlayers(@NonNull Gamer gamer, @NonNull Collection<Gamer> gamers) {
        Player player = gamer.getPlayer();

        val ranks = gamer.getRanks();

        boolean isPlayerHiding = isHideEnabled(gamer);

        boolean isPlayerImportant = ranks.hasPermission("lobby.hide.bypass")
                && gamer.getFakeName() == null;

        for (Gamer other : gamers) {
            if (other == gamer || other.isVanished()) {
                continue;
            }

            boolean isTargetHiding = isHideEnabled(other);

            boolean isFriends = isFriends(gamer, other);
            boolean isInParty = isSameParty(gamer, other);

            val otherRanks = other.getRanks();

            boolean isImportant = otherRanks.hasPermission("lobby.hide.bypass")
                    && other.getFakeName() == null;

            Player target = other.getPlayer();

            boolean hideTargetForPlayer = isPlayerHiding && (!isFriends && !isInParty && !isImportant);

            PlayerToggleHideEvent playerHide = new PlayerToggleHideEvent(gamer, other, hideTargetForPlayer);
            HeartBeat.callEvent(playerHide);

            if (!playerHide.isCancelled()) {
                if (hideTargetForPlayer) {
                    player.hidePlayer(target);
                    hideDivisions(gamer);
                } else {
                    player.showPlayer(target);
                    showDivisions(gamer);
                }
            }

            boolean hideYouForTarget = isTargetHiding && (!isFriends && !isInParty && !isPlayerImportant);

            PlayerToggleHideEvent targetHide = new PlayerToggleHideEvent(other, gamer, hideYouForTarget);
            HeartBeat.callEvent(targetHide);

            if (targetHide.isCancelled()) {
                continue;
            }

            if (hideYouForTarget) {
                target.hidePlayer(player);
            } else if (!gamer.isVanished()) {
                target.showPlayer(player);
            }
        }
    }

    private static void hideDivisions(Gamer gamer) {
        DivisionStatusListener.PLAYER_HOLOGRAMS.forEach((g, h) -> {
            val currentPlayer = gamer.getPlayer();
            val secondaryPlayer = g.getPlayer();
            if (currentPlayer != secondaryPlayer) {
                h.removeReceiver(currentPlayer);
            }
        });
    }

    private static void showDivisions(Gamer gamer) {
        DivisionStatusListener.PLAYER_HOLOGRAMS.forEach((g, h) -> {
            val currentPlayer = gamer.getPlayer();
            val secondaryPlayer = g.getPlayer();
            if (currentPlayer != secondaryPlayer) {
                h.addReceiver(currentPlayer);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        if (gamer != null) {
            HeartBeat.runSync(() -> hidePlayers(gamer, Gamer.getGamers()));
        }
    }

    @EventHandler
    public void onGamerSettingsChange(GamerSettingToggleEvent event) {
        Setting setting = event.getSetting();

        if (setting != Setting.HIDE_PLAYERS) {
            return;
        }

        Gamer gamer = event.getGamer();

        if (!gamer.isVanished()) {
            HeartBeat.runSync(() -> hidePlayers(gamer, Gamer.getGamers()));
        }
    }

    @EventHandler
    public void onPartyInvite(PartyInviteEvent event) {
        HeartBeat.runSync(() -> hidePlayers(
                event.getGamer(),
                event.getParty().getMembers()
        ));
    }

    @EventHandler
    public void onPartyKick(PartyKickEvent event) {
        HeartBeat.runSync(() -> hidePlayers(
                event.getGamer(),
                event.getParty().getMembers()
        ));
    }

    // region Static methods

    @EventHandler
    public void onFriendAdd(FriendAddEvent event) {
        Gamer friendGamer = Gamer.getGamer(event.getFriendId());

        if (friendGamer != null) {
            HeartBeat.runSync(() -> hidePlayers(event.getGamer(), Collections.singleton(friendGamer)));
        }
    }

    @EventHandler
    public void onFriendRemove(FriendRemoveEvent event) {
        Gamer friendGamer = Gamer.getGamer(event.getFriendId());

        if (friendGamer != null) {
            HeartBeat.runSync(() -> hidePlayers(event.getGamer(), Collections.singleton(friendGamer)));
        }
    }

    @EventHandler
    public void onVanish(GamerVanishEvent event) {
        if (!event.isVanished()) {
            HeartBeat.runSync(() -> hidePlayers(event.getGamer(), Gamer.getNonVanishedGamers()));
        }
    }

    @EventHandler
    public void onNickChange(GamerDisplayNameUpdateEvent event) {
        Gamer gamer = event.getGamer();

        HeartBeat.runSync(() -> hidePlayers(gamer, Gamer.getGamers()));
    }

}
