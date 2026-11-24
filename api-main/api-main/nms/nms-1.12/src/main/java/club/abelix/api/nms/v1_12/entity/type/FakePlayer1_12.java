package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.entity.type.player.SkinPart;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.tablist.FakeEntityTablistEntry;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import club.abelix.api.nms.team.rule.CollisionRule;
import club.abelix.api.nms.team.rule.TagVisibility;
import club.abelix.api.nms.v1_12.team.FakeTeam1_12;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FakePlayer1_12 extends FakeLivingEntity1_12 implements FakeEntityPlayer {

    protected final ThreadLocalRandom random = ThreadLocalRandom.current();

    // ------------------------------------------------------------------------------

    private static final WrappedDataWatcher.WrappedDataWatcherObject SKIN_PARTS_WATCHER_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(13, BYTE_SERIALIZER);

    // ------------------------------------------------------------------------------

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

    public FakePlayer1_12(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull String skin, @NonNull Location location)
            throws SkinException {

        this(nmsApi, name, SkinsApi.getCachedSkin(skin), location);
    }

    public FakePlayer1_12(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull Skin skin, @NonNull Location location) {
        super(nmsApi, EntityType.PLAYER, location);

        this.name = name.length() > 16 ? name.substring(0, 16) : name;
        this.uuid = UUID.randomUUID();

        this.fakeTeam = new FakeTeam1_12(nmsApi, "ZNPC/" + getEntityId(), "§8", "");
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

        // не отправляем пакет удаления из таба если надо
        if (disableRemovingFromTab) {
            return;
        }

        addTablistRemoveTask(players, 100);
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
    public synchronized void setGlowingColor(@NonNull ChatColor glowingColor) {
        if (getGlowingColor() == glowingColor) {
            return;
        }

        this.fakeTeam.setPrefix(glowingColor.toString());
        this.fakeTeam.setColor(glowingColor);

        broadcastPacket(nmsApi.getPacketLibraryApi().getTeamPacket(TeamAction.UPDATE, fakeTeam, Collections.emptyList()));
    }

    public synchronized ChatColor getGlowingColor() {
        return fakeTeam.getColor();
    }

    @Override
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
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setTeam(@NonNull FakeTeam team) {
        this.fakeTeam = team;
    }

    private synchronized void updateSkinPartsStatus() {
        getDataWatcher().setObject(SKIN_PARTS_WATCHER_OBJECT, enabledSkinParts);
        broadcastDataWatcherPacket();
    }

    @Override
    public void destroyEntity(@NotNull Collection<? extends Player> players) {
        super.destroyEntity(players);

        // у игроков в табе итак нет этого нпс (но это только в теории)
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
