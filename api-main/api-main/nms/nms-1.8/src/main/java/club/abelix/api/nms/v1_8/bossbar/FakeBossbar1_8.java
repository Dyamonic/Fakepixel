package club.abelix.api.nms.v1_8.bossbar;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.NmsConstant;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.api.nms.packet.AbstractVisibleObject;
import club.abelix.api.nms.provider.LanguageProvider;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.parser.I18nParserController;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class FakeBossbar1_8 extends AbstractVisibleObject implements FakeBossbar {

    private final I18nParserController parser = I18nParserController.getInstance();

    protected final NmsApi api;

    @Getter
    private String title;

    @Getter
    private float value;

    private final BukkitTask task;

    public FakeBossbar1_8(NmsApi api, String title, float value) {
        this.api = api;
        this.title = title;
        this.value = value;

        Server server = api.getPlugin().getServer();

        this.task = server.getScheduler().runTaskTimerAsynchronously(api.getPlugin(), () -> {
            for (Player player : receivers) {
                updateBossbar(player);
            }
        }, 20, 20);
    }

    private Location getBossLocation(Player player) {
        Location location = player.getLocation();

        Vector vector = new Vector();
        double rotX = location.getYaw();
        double rotY = location.getPitch() + 25F;
        vector.setY(-Math.sin(Math.toRadians(rotY)));
        double xz = Math.cos(Math.toRadians(rotY));
        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));
        vector.multiply(48F);

        return location.add(vector);
    }

    private WrappedDataWatcher getDataWatcher(@NonNull Player player) {
        LanguageProvider provider = api.getPlugin().getServer()
                .getServicesManager().load(LanguageProvider.class);

        Language language = provider.getLanguage(player);

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(0, (byte) 0x20);
        watcher.setObject(3, (byte) 1);
        watcher.setObject(4, (byte) 1);
        watcher.setObject(6, value * 300F);

        if (title != null) {
            String localizedTitle = title;

            if (localizedTitle.startsWith("lang:")) {
                localizedTitle = parser.parse(language, localizedTitle);
            }

            watcher.setObject(2, localizedTitle);
        }

        watcher.setObject(11, (byte) 1);
        watcher.setObject(20, 890);

        return watcher;
    }

    private void spawnEntity(Player player) {
        WrappedDataWatcher dataWatcher = getDataWatcher(player);
        Location location = getBossLocation(player);

        PacketContainer packet = api.getPacketLibraryApi().getSpawnLivingEntityPacket(
                player.getEntityId() + 50000, EntityType.WITHER, location, dataWatcher
        );

        api.getPacketLibraryApi().sendPacket(player, packet);
    }

    private void updateMetadata(Player player) {
        WrappedDataWatcher dataWatcher = getDataWatcher(player);

        PacketContainer packet = api.getPacketLibraryApi().getDataWatcherPacket(
                player.getEntityId() + 50000, dataWatcher
        );

        api.getPacketLibraryApi().sendPacket(player, packet);
    }

    private void destroyEntity(Player player) {
        PacketContainer packet = api.getPacketLibraryApi().getEntityDestroyPacket(
                player.getEntityId() + 50000
        );

        api.getPacketLibraryApi().sendPacket(player, packet);
    }

    private void updateMetadata() {
        receivers.forEach(this::updateMetadata);
    }

    @Override
    public void setPublicStatus(boolean b) {
        super.setPublicStatus(b);

        if (b && !NmsConstant.c_customBossbarSending) {
            addReceivers(Bukkit.getOnlinePlayers());
        }
    }

    @Override
    public boolean addReceivers(@NotNull Collection<? extends Player> players) {
        boolean add = false;

        for (Player player : players) {
            if (receivers.add(player)) {
                api.getBossbarApi().addBossbar(player, this);
                spawnEntity(player);

                add = true;
            }
        }

        return add;
    }

    @Override
    public void setBarColor(String color) {
        //unsupported
    }

    @Override
    public String getBarColor() {
        return null;
    }

    @Override
    public void setBarStyle(String style) {
        //unsupported
    }

    @Override
    public String getBarStyle() {
        return null;
    }

    @Override
    public boolean removeReceivers(@NotNull Collection<? extends Player> players) {
        boolean remove = false;

        for (Player player : players) {
            if (receivers.remove(player)) {
                api.getBossbarApi().removeBossbar(player, this);

                destroyEntity(player);

                remove = true;
            }
        }

        return remove;
    }

    @Override
    public void remove() {
        removeReceivers(new HashSet<>(receivers));

        task.cancel();
    }

    @Override
    public void setTitle(String title) {
        if (!title.equals(this.title)) {
            this.title = title;

            updateMetadata();
        }
    }

    @Override
    public void updateBossbar(Player player) {
        Location location = getBossLocation(player);

        PacketContainer packet = api.getPacketLibraryApi().getEntityTeleportPacket(
                player.getEntityId() + 50000, location
        );

        api.getPacketLibraryApi().sendPacket(player, packet);
    }

    @Override
    public void setValue(float v) {
        if (v > 1F || v < 0F) {
            throw new IllegalArgumentException("Value must be between 0 and 1");
        }

        if (v != value) {
            this.value = v;

            this.updateMetadata();
        }
    }

    @Override
    public void setValue(float v, float max) {
        if (v > max || v < 0F) {
            throw new IllegalStateException("Value must be between 0 and " + max);
        }

        setValue(v / max);
    }

    @Override
    public boolean addViewer(@NonNull @NotNull Player player) {
        return addReceiver(player);
    }

    @Override
    public boolean removeViewer(@NonNull @NotNull Player player) {
        return removeReceiver(player);
    }

    @Override
    public boolean hasViewer(@NonNull Player player) {
        return hasReceiver(player);
    }
}
