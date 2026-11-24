package club.abelix.api.nms.team;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.packet.AbstractVisibleObject;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.team.action.TeamAction;
import club.abelix.api.nms.team.rule.FakeTeamFlag;
import club.abelix.api.nms.team.rule.TagVisibility;
import club.abelix.api.nms.team.rule.TeamFlag;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.collect.Lists;
import lombok.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
@ToString(exclude = "nmsApi")
public abstract class FakeTeamBase extends AbstractVisibleObject implements FakeTeam {

    private final NmsApi nmsApi;
    private final String name;
    /**
     * Участники данной тимы (игроки в ней)
     */
    private final List<String> players = new ArrayList<>();
    private String prefix, suffix;
    @Setter
    private TagVisibility tagVisibility = TagVisibility.ALWAYS;
    @Setter
    private ChatColor color = ChatColor.WHITE;
    private byte minecraftTeamFlags;
    private byte fakeTeamFlags;
    private WrappedChatComponent prefixComponent, suffixComponent;

    public FakeTeamBase(@NonNull NmsApi nmsApi, @NonNull String name) {
        this.nmsApi = nmsApi;
        this.name = fixName(name);
    }

    public FakeTeamBase(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull String prefix, @NonNull String suffix) {
        this(nmsApi, name);

        this.prefix = fixName(prefix);
        this.suffix = fixName(suffix);
    }

    @Override
    public void addPlayer(String playerName) {
        addPlayers(Lists.newArrayList(playerName));
    }

    @Override
    public void addPlayers(List<String> playerNames) {
        players.addAll(playerNames);

        PacketLibraryApi packetLibraryApi = nmsApi.getPacketLibraryApi();

        PacketContainer container = packetLibraryApi.getTeamPacket(TeamAction.PLAYERS_ADD, this, playerNames);

        for (val player : receivers) {
            packetLibraryApi.sendPacket(player, container);
        }
    }

    @Override
    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    @Override
    public Set<Player> getReceivers() {
        return receivers;
    }

    @Override
    public boolean addReceivers(@NotNull Collection<? extends Player> players) {
        boolean add = super.addReceivers(players);

        if (add) {
            PacketLibraryApi packetLibraryApi = nmsApi.getPacketLibraryApi();

            PacketContainer createPacket = packetLibraryApi.getTeamPacket(TeamAction.CREATE, this,
                    this.players);

            players.forEach(player -> packetLibraryApi.sendPacket(player, createPacket));
        }

        return add;
    }

    @Override
    public boolean removeReceivers(@NotNull Collection<? extends Player> players) {
        boolean remove = super.removeReceivers(players);

        if (remove) {
            PacketLibraryApi packetLibraryApi = nmsApi.getPacketLibraryApi();

            PacketContainer container = packetLibraryApi.getTeamPacket(TeamAction.REMOVE, this,
                    Collections.emptyList());

            players.forEach(player -> packetLibraryApi.sendPacket(player, container));
        }

        return remove;
    }

    @Override
    public boolean hasReceiver(@NotNull Player player) {
        return receivers.contains(player);
    }

    @Override
    public boolean isPublicStatus() {
        return hasFlag(FakeTeamFlag.SEND_TEAM_ON_JOIN);
    }

    @Override
    public void setPublicStatus(boolean publicStatus) {
        setFlag(FakeTeamFlag.SEND_TEAM_ON_JOIN);
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = fixName(prefix);
        this.prefixComponent = null;
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = fixName(suffix);
        this.suffixComponent = null;
    }

    @Override
    public boolean hasFlag(FakeTeamFlag flag) {
        return (fakeTeamFlags & flag.getMask()) > 0;
    }

    @Override
    public boolean setFlag(FakeTeamFlag flag) {
        return fakeTeamFlags != (fakeTeamFlags |= flag.getMask());
    }

    @Override
    public boolean unsetFlag(FakeTeamFlag flag) {
        return fakeTeamFlags != (fakeTeamFlags &= ~flag.getMask());
    }

    @Override
    public boolean setFlag(@NonNull TeamFlag flag) {
        return minecraftTeamFlags != (minecraftTeamFlags |= flag.getMask());
    }

    @Override
    public boolean unsetFlag(@NonNull TeamFlag flag) {
        return minecraftTeamFlags != (minecraftTeamFlags &= ~flag.getMask());
    }

    @Override
    public boolean hasFlag(@NonNull TeamFlag flag) {
        return (minecraftTeamFlags & flag.getMask()) > 0;
    }

    @Override
    public boolean isSeeInvisible() {
        return hasFlag(TeamFlag.SEE_INVISIBLE);
    }

    @Override
    public void setSeeInvisible(boolean b) {
        boolean updated = b
                ? setFlag(TeamFlag.SEE_INVISIBLE)
                : unsetFlag(TeamFlag.SEE_INVISIBLE);

        if (updated) {
            update();
        }
    }

    @Override
    public boolean isFriendlyFire() {
        return hasFlag(TeamFlag.FRIENDLY_FIRE);
    }

    @Override
    public void setFriendlyFire(boolean b) {
        boolean updated = b
                ? setFlag(TeamFlag.FRIENDLY_FIRE)
                : unsetFlag(TeamFlag.FRIENDLY_FIRE);

        if (updated) {
            update();
        }
    }

    @Override
    public void update() {
        PacketLibraryApi packetLibraryApi = nmsApi.getPacketLibraryApi();

        PacketContainer container = packetLibraryApi.getTeamPacket(TeamAction.UPDATE, this,
                Collections.emptyList());

        for (val player : receivers) {
            packetLibraryApi.sendPacket(player, container);
        }
    }

    @Override
    public void remove() {
        new ArrayList<>(players).forEach(this::removePlayer);
        new ArrayList<>(receivers).forEach(this::removeReceiver);
    }

    @Override
    public WrappedChatComponent getPrefixComponent() {
        if (prefixComponent == null) {
            prefixComponent = WrappedChatComponent.fromText(prefix);
        }

        return prefixComponent;
    }

    @Override
    public WrappedChatComponent getSuffixComponent() {
        if (suffixComponent == null) {
            suffixComponent = WrappedChatComponent.fromText(suffix);
        }

        return suffixComponent;
    }

    private String fixName(@NonNull String value) {
        return nmsApi.isLegacy() && value.length() > 16 ? value.substring(0, 16) : value;
    }

}
