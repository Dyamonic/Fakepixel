package club.abelix.api.server.lobby.linkage;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.linkage.npc.LinkageRewardNPC;
import club.abelix.commons.util.time.PlayerCooldownUtil;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.both.BCheckChannelSubscription;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LinkageRewardManager {

    TLongList channelsToSubscribe = new TLongArrayList();
    TIntSet waitingPlayers = new TIntHashSet();

    public void registerManager(final @NonNull ConfigurationSection section) {
        this.channelsToSubscribe.addAll(section.getLongList("ChannelsIds"));

        val npcSection = section.getConfigurationSection("NPC");

        if (npcSection != null) {
            new LinkageRewardNPC(this, npcSection);
        }
    }

    public CompletableFuture<BCheckChannelSubscription.SubscriptionStatus> checkChannelSubscription(final @NonNull Gamer gamer, final long chatId) {
        return Connection.awaitPacket(new BCheckChannelSubscription.Request(gamer.getPlayerId(), chatId), 5000L)
                .thenApply(response -> {
                    if (response == null) {
                        return null;
                    }

                    return response.getStatus();
                });
    }

    public PlayerCooldownUtil.CooldownRecord checkAndSetCooldown(final @NonNull Gamer gamer) {
        val cooldownKey = "linkage_conditions_check";
        val playerId = gamer.getPlayerId();

        if (PlayerCooldownUtil.hasCooldown(playerId, cooldownKey)) {
            return PlayerCooldownUtil.getCooldown(playerId, cooldownKey);
        }

        PlayerCooldownUtil.addCooldown(playerId, cooldownKey, 30, TimeUnit.SECONDS);
        return null;
    }
}
