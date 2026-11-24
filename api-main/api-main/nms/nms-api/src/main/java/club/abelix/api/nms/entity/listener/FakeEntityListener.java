package club.abelix.api.nms.entity.listener;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.interact.InteractAction;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.commons.util.time.CooldownUtil;
import club.abelix.core.bukkit.HeartBeat;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class FakeEntityListener implements Listener {

    private final NmsApi nmsApi;


    public FakeEntityListener(@NonNull NmsApi nmsApi) {
        Plugin plugin = nmsApi.getPlugin();

        PacketLibraryApi.PROTOCOL_MANAGER.addPacketListener(new PacketAdapter(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleEntityInteract(event);
            }
        });

        nmsApi.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(nmsApi.getPlugin(), () -> {
            for (val player : nmsApi.getPlugin().getServer().getOnlinePlayers()) {
                checkVisibility(player);
            }
        }, 0L, 20L);

        this.nmsApi = nmsApi;
    }

    private void checkVisibility(final @NonNull Player player) {
        for (val entity : getTrackedEntities()) {
            val location = entity.getLocation();
            val world = location.getWorld();

            if (world != player.getWorld()) {
                entity.removeViewer(player);
                continue;
            }

            if (player.getLocation().distanceSquared(location) <= entity.getVisibilityDistance()) {
                // игрок уже видит сущность
                if (entity.hasViewer(player)) {
                    continue;
                }

                if (entity.isPublicStatus() || entity.hasReceiver(player)) {
                    HeartBeat.runSync(() -> entity.addViewer(player));
                }
            } else {
                if (entity.isPublicStatus() || entity.hasReceiver(player)) {
                    HeartBeat.runSync(() -> entity.removeViewer(player));
                }
            }
        }
    }

    private void handleChunkUnloads(Player player) {
        for (val entity : getTrackedEntities()) {
            if (!entity.isPublicStatus() && !entity.hasReceiver(player)) {
                continue;
            }

            entity.removeViewer(player);
        }
    }

    private void handleQuit(Player player) {
        for (val entity : getTrackedEntities()) {
            if (!entity.removeReceiver(player)) {
                entity.removeViewer(player);
            }
        }
    }

    public void handleEntityInteract(PacketEvent event) {
        val player = event.getPlayer();
        val entityUseAction = event.getPacket().getEntityUseActions().read(0);

        if (entityUseAction == EnumWrappers.EntityUseAction.INTERACT) {
            return;
        }

        val fakeEntity = nmsApi.getFakeEntityApi().getFakeEntity(event.getPacket().getIntegers().read(0));

        if (!(fakeEntity instanceof FakeLivingEntity)) {
            return;
        }

        val cooldownKey = player.getName().toLowerCase().concat("-entity-interact");

        if (CooldownUtil.hasCooldown(cooldownKey)) {
            return;
        }

        val action = entityUseAction == EnumWrappers.EntityUseAction.ATTACK
                ? InteractAction.LEFT_CLICK : InteractAction.RIGHT_CLICK;

        try {
            val handModifier = event.getPacket().getHands();

            if (!handModifier.getValues().isEmpty()) {
                val hand = handModifier.read(0);

                if (hand != EnumWrappers.Hand.MAIN_HAND) {
                    return;
                }
            }
        } catch (Exception ignored) {

        }

        val clickAction = ((FakeLivingEntity) fakeEntity).getClickAction();

        if (clickAction != null) {
            nmsApi.getPlugin().getServer().getScheduler().runTask(nmsApi.getPlugin(), () -> clickAction.accept(player, action));

            CooldownUtil.addCooldown(cooldownKey, 500);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final @NonNull PlayerJoinEvent event) {
        val player = event.getPlayer();

        // игрока возможно могло кикнуть
        if (player.isOnline()) {
            checkVisibility(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handleQuit(event.getPlayer());
    }

    private Collection<FakeEntity> getTrackedEntities() {
        return nmsApi.getFakeEntityApi().getTrackedEntities();
    }

}