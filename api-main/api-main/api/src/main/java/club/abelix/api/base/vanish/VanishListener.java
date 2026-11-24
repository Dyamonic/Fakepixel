package club.abelix.api.base.vanish;

import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerJoinMessageEvent;
import club.abelix.api.base.gamer.event.GamerPropertyChangeEvent;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class VanishListener extends YListener {

    /**
     * Название проперти ваниша
     */
    public static final String VANISH_PROPERTY_NAME = "Vanished";

    @EventHandler
    public void onPlayerVanishStatusChange(GamerPropertyChangeEvent event) {
        Gamer gamer = event.getGamer();

        if (!event.getProperty().equalsIgnoreCase(VANISH_PROPERTY_NAME)) {
            return;
        }

        HeartBeat.callEvent(new GamerVanishEvent(gamer, event.getNewValue() == 1));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerVanish(GamerVanishEvent event) {
        Gamer gamer = event.getGamer();
        boolean vanished = event.isVanished();

        refreshVisibility(gamer);
        checkAndSetStaffFlying(gamer, vanished);
        HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(gamer));
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (!(damager instanceof Player)) {
            return;
        }

        Gamer gamer = Gamer.getGamer((Player) damager);

        if (gamer != null && gamer.isVanished()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageBySource(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (!(damager instanceof Projectile)) {
            return;
        }

        if (!(((Projectile) damager).getShooter() instanceof Entity shooter)) {
            return;
        }

        Gamer gamer = Gamer.getGamer(shooter.getName());

        if (gamer != null && gamer.isVanished()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null) {
            return;
        }

        if (gamer.isVanished()) {
            checkAndSetStaffFlying(gamer, true);
            gamer.sendLocalizedMessage("vanished");
        }

        refreshVisibility(gamer);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatFormatEvent event) {
        Gamer sender = event.getSender();
        Gamer receiver = event.getReceiver();

        if (isStaff(sender) && sender.isVanished()) {
            if (!isStaff(receiver)) {
                event.setCancelled(true);
                return;
            }

            event.unsafeAppendFormat(receiver.getLanguage().getMessage("vanished_chat_format"));
            return;
        }

        if (!sender.isVanished()) {
            return;
        }

        if (isStaff(receiver) || receiver.getRanks().hasPermission("vanish.chat.read")) {
            event.unsafeAppendFormat(receiver.getLanguage().getMessage("vanished_chat_format"));
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onGamerJoinMessage(GamerJoinMessageEvent event) {
        Gamer gamer = event.getGamer();

        if (gamer.isVanished()) {
            event.setCancelled(true);
        }
    }

    private void refreshVisibility(@NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        if (player == null) {
            return;
        }

        for (Gamer targetGamer : Gamer.getGamers()) {
            Player target = targetGamer.getPlayer();

            if (target == null || target.equals(player)) {
                continue;
            }

            updateVisibility(gamer, targetGamer);
            updateVisibility(targetGamer, gamer);
        }
    }

    private void updateVisibility(@NonNull Gamer viewerGamer, @NonNull Gamer targetGamer) {
        Player viewer = viewerGamer.getPlayer();
        Player target = targetGamer.getPlayer();

        if (viewer == null || target == null) {
            return;
        }

        if (canSee(viewerGamer, targetGamer)) {
            viewer.showPlayer(target);
        } else {
            viewer.hidePlayer(target);
        }
    }

    private boolean canSee(@NonNull Gamer viewer, @NonNull Gamer target) {
        if (isStaff(viewer)) {
            return true;
        }

        if (isStaff(target) && target.isVanished()) {
            return false;
        }

        return !target.isVanished() || viewer.getRanks().hasPermission("vanish.see");
    }

    private boolean isStaff(@NonNull Gamer gamer) {
        return gamer.getRanks().isStaff();
    }

    private boolean isDonateVanished(@NonNull Gamer gamer) {
        return gamer.isVanished() && !gamer.getRanks().isStaff();
    }

    private void checkAndSetStaffFlying(final Gamer gamer, boolean vanished) {
        if (isStaff(gamer) && gamer.getPlayer() != null) {
            Player p = gamer.getPlayer();
            p.setAllowFlight(vanished);
            p.setFlying(vanished);
        }
    }
}