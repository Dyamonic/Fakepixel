package club.abelix.api.base.hotbar.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.Hotbar;
import club.abelix.api.base.hotbar.Icon;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
public class HotbarManager {

    public static final HotbarManager INSTANCE = new HotbarManager();

    private final Map<String, Hotbar> registeredHotbarMap = new HashMap<>();

    private final Map<Integer, Hotbar> gamerHotbarMap = new ConcurrentHashMap<>();

    /**
     * Хотбар, который будет автоматически ставиться при входе игрока
     * В хабе/лобби -- "hub", до старта игры -- "lobby", после старта -- null.
     */
    @Getter
    private String defaultHotbar;

    public void setDefaultHotbar(String defaultHotbar) {
        this.defaultHotbar = defaultHotbar;
        log.info("[Hotbars] Default hotbar set: {}", defaultHotbar);
    }

    public void clearHotbars() {
        gamerHotbarMap.clear();
        registeredHotbarMap.clear();
        defaultHotbar = null;
        log.info("[Hotbars] Cleared all hotbars. size={}", gamerHotbarMap.size());
    }

    public Hotbar findHotbar(@NonNull String hotbarName) {
        return registeredHotbarMap.get(hotbarName);
    }

    public void registerIcon(@NonNull String hotbar, int slot,
                             @NonNull Function<Gamer, ItemStack> icon,
                             @NonNull Consumer<Gamer> callback) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("Invalid hotbar slot " + slot);
        }

        Hotbar bar = registeredHotbarMap.computeIfAbsent(hotbar, Hotbar::new);

        if (bar.getIcons()[slot] != null) {
            throw new IllegalStateException("Slot " + slot + " in " + hotbar + " already has icon");
        }

        bar.getIcons()[slot] = new Icon(icon, callback);

        gamerHotbarMap.forEach((id, playerBar) -> {
            if (playerBar == bar) {
                Gamer g = Gamer.getGamer(id);
                if (g != null) {
                    playerBar.apply(g);
                }
            }
        });

        log.info("[Hotbars] Register icon {}/{}", hotbar, slot);
    }

    public void unregisterIcon(@NonNull String hotbar, int slot) {
        // Не создаём новый хотбар, если его нет
        Hotbar bar = registeredHotbarMap.get(hotbar);
        if (bar != null) {
            bar.getIcons()[slot] = null;
        }
        log.info("[Hotbars] Reset icon {}/{}", hotbar, slot);
    }

    // Зарегистрировать фиксированную иконку
    public void registerIcon(@NonNull String hotbar, int slot,
                             @NonNull ItemStack icon,
                             @NonNull Consumer<Gamer> callback) {
        registerIcon(hotbar, slot, viewer -> icon, callback);
    }

    // Перерисовывает хотбар
    public void updateIcons(@NonNull Gamer gamer) {
        Hotbar hotbar = gamerHotbarMap.get(gamer.getPlayerId());
        if (hotbar != null) {
            hotbar.apply(gamer);
        }
    }

    // Перерисовывает иконку в хотбаре
    public void updateIcon(@NonNull Gamer gamer, int slot) {
        Player player = gamer.getPlayer();
        Hotbar hotbar = gamerHotbarMap.get(gamer.getPlayerId());

        if (hotbar != null) {
            Icon icon = hotbar.getIcons()[slot];

            if (icon == null) {
                player.getInventory().setItem(slot, null);
                return;
            }

            player.getInventory().setItem(slot, icon.getIcon().apply(gamer));
        }
    }

    public Hotbar getHotbar(@NonNull Gamer gamer) {
        return gamerHotbarMap.get(gamer.getPlayerId());
    }

    public void setHotbar(@NonNull Gamer gamer, @NonNull String hotbar) {
//        Hotbar bar = registeredHotbarMap.get(hotbar);
//        if (bar == null) {
//            throw new IllegalArgumentException("Hotbar " + hotbar + " not found");
//        }
//
//        gamerHotbarMap.put(gamer.getPlayerId(), bar);
//        bar.apply(gamer);
//        log.info("[Hotbars] Set hotbar: {}, {} | size={}", gamer.getName(), hotbar, gamerHotbarMap.size());
    }

    public void clearPlayerHotbar(@NonNull Gamer gamer) {
        Hotbar hotbar = gamerHotbarMap.remove(gamer.getPlayerId());

        if (hotbar != null) {
            log.info("[Hotbars] Cleared gamer hotbar, gamer={} | size={}", gamer.getName(), gamerHotbarMap.size());

            Icon[] icons = hotbar.getIcons();
            for (int i = 0; i < 9; i++) {
                if (icons[i] == null) continue;
                gamer.getPlayer().getInventory().setItem(i, null);
            }
        } else {
            log.info("[Hotbars] Nothing to clear for gamer={} | size={}", gamer.getName(), gamerHotbarMap.size());
        }
    }

    public void clearPlayerHotbars(Collection<Gamer> gamers) {
        log.info("[Hotbars] Clear player hotbars ({} gamers, before size={})",
                gamers.size(), gamerHotbarMap.size());
        gamers.forEach(this::clearPlayerHotbar);
        log.info("[Hotbars] Finished clearing player hotbars. size={}", gamerHotbarMap.size());
    }
}
