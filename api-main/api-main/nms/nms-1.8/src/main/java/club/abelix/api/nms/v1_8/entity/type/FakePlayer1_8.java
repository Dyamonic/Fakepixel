package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.entity.type.player.SkinPart;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.tablist.FakeEntityTablistEntry;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import club.abelix.api.nms.team.rule.CollisionRule;
import club.abelix.api.nms.team.rule.TagVisibility;
import club.abelix.api.nms.v1_8.team.FakeTeam1_8;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class FakePlayer1_8 extends FakeLivingEntity1_8 implements FakeEntityPlayer {

    private final PacketContainer removePlayerPacket;

    private final UUID uuid;

    @Getter
    private final String name;

    @Getter
    private final Skin skin;

    @Getter
    private FakeTeam fakeTeam;

    @Getter
    private byte enabledSkinParts;

    private boolean disableRemovingFromTab;

    private final List<FakeEntityTablistEntry> tablistEntries = new ArrayList<>();

    public FakePlayer1_8(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull String skin, @NonNull Location location)
            throws SkinException {

        this(nmsApi, name, SkinsApi.getCachedSkin(skin), location);
    }

    public FakePlayer1_8(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull Skin skin, @NonNull Location location) {
        super(nmsApi, EntityType.PLAYER, location);

        this.name = name.length() > 16 ? name.substring(0, 16) : name;
        this.uuid = UUID.randomUUID();

        this.fakeTeam = new FakeTeam1_8(nmsApi, "ZNPC/" + getEntityId(), "§8", "");
        this.fakeTeam.setTagVisibility(TagVisibility.NEVER);
        this.fakeTeam.setCollisionRule(CollisionRule.NEVER);
        this.fakeTeam.addPlayer(name);

        this.skin = skin;

        this.removePlayerPacket = nmsApi.getPacketLibraryApi()
                .getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, this);

        this.enabledSkinParts = SkinPart.TOTAL;
        updateSkinPartsStatus();
    }

    @Override
    public synchronized void spawnEntity(@NotNull Collection<? extends Player> players) {
        super.spawnEntity(players);

        if (disableRemovingFromTab) {
            return;
        }

        nmsApi.getPlugin().getServer().getScheduler().runTaskLater(nmsApi.getPlugin(), () -> {
            PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

            players.forEach(player -> packetApi.sendPacket(player, removePlayerPacket));
        }, 100);
    }

    @Override
    protected synchronized void addSpawnEntityPackets(PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        packets.add(packetApi.getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, this));
        super.addSpawnEntityPackets(packetApi, packets);

        packets.add(packetApi.getTeamPacket(TeamAction.CREATE, fakeTeam, fakeTeam.getPlayers()));
        packets.add(packetApi.getEntityLookPacket(getEntityId(), getLocation()));
        packets.add(packetApi.getHeadRotationPacket(getEntityId(), getLocation().getYaw()));
    }

    @Override
    protected PacketContainer getSpawnEntityPacket(@NonNull PacketLibraryApi packetApi) {
        return packetApi.getSpawnNamedEntityPacket(getEntityId(), uuid, getLocation(), getDataWatcher());
    }

    @Override
    public synchronized void setGlowingColor(ChatColor glowingColor) {
        if (getGlowingColor() == glowingColor) {
            return;
        }

        this.fakeTeam.setPrefix(glowingColor.toString());
        this.fakeTeam.setColor(glowingColor);

        broadcastPacket(nmsApi.getPacketLibraryApi().getTeamPacket(TeamAction.UPDATE, fakeTeam, Collections.emptyList()));
    }

    @Override
    public synchronized ChatColor getGlowingColor() {
        return fakeTeam.getColor();
    }

    @Override
    public synchronized UUID getUUID() {
        return uuid;
    }

    @Override
    public void setTeam(@NonNull FakeTeam team) {
        this.fakeTeam = team;
    }

    private synchronized void updateSkinPartsStatus() {
        getDataWatcher().setObject(10, enabledSkinParts);
        broadcastDataWatcherPacket();
    }

    public synchronized void setSkinPartStatus(@NonNull SkinPart skinPart, boolean status) {
        int oldSkinParts = enabledSkinParts;

        if (status) {
            enabledSkinParts |= skinPart.getMask();
        } else {
            enabledSkinParts &= ~skinPart.getMask();
        }

        if (oldSkinParts == enabledSkinParts) {
            return;
        }

        updateSkinPartsStatus();
    }

    @Override
    public void destroyEntity(@NotNull Collection<? extends Player> players) {
        super.destroyEntity(players);

        if (!disableRemovingFromTab) {
            return;
        }

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        for (Player player : players) {
            packetApi.sendPacket(player, removePlayerPacket);
        }
    }

    @Override
    public void disableRemovingFromTab() {
        disableRemovingFromTab = true;
    }

    public void addTablistRemoveTask(final @NonNull Collection<? extends Player> players, long delay) {
        tablistEntries.add(new FakeEntityTablistEntry(players, this, delay, System.currentTimeMillis()));
    }

    public void removeTablistEntry(final @NonNull FakeEntityTablistEntry entry) {
        tablistEntries.remove(entry);
    }

    public Collection<FakeEntityTablistEntry> getTablistEntries() {
        return tablistEntries;
    }

    @Override
    public void hideFromTab(@NonNull Collection<? extends Player> players) {
        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        players.forEach(player -> packetApi.sendPacket(player, removePlayerPacket));
    }
}
