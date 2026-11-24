package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.compose.HotbarComposer;
import club.abelix.di.api.annotation.Bean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Bean
@Slf4j
@Getter
public class HotbarService {

    private final Map<String, Hotbar> hotbars = new ConcurrentHashMap<>();
    private final Map<Integer, Hotbar> gamerHotbarMap = new ConcurrentHashMap<>();
    private final HotbarComposer composer = new HotbarComposer();
    private final HotbarIconProvidersRegistry registry = new StandardHotbarIconProvidersRegistry();

    private Hotbar defaultHotbar;

    public void setDefaultHotbar(Hotbar hotbar) {
        this.defaultHotbar = hotbar;

        registerHotbar(hotbar);

        log.info("Set default hotbar: {}", hotbar.getId());
    }

    public Optional<Hotbar> findHotbar(String id) {
        return Optional.ofNullable(hotbars.get(id));
    }

    public void registerHotbar(Hotbar hotbar) {
        hotbars.put(hotbar.getId(), hotbar);
        log.info("Registered hotbar: {}", hotbar.getId());
    }

    public void refreshIcon(Gamer g, int slot) {
        Hotbar hb = getCurrent(g);
        if (hb == null) return;

        hb.getIcon(slot).ifPresent(icon -> {
            val newItem = icon.getItem(g);
            g.getPlayer().getInventory().setItem(slot, newItem);
        });
    }

    public void apply(String id, Gamer gamer) {
        val optional = findHotbar(id);

        if (optional.isPresent()) {
            apply(optional.get(), gamer);
        } else if (defaultHotbar != null) {
            apply(defaultHotbar, gamer);
        } else {
            log.warn("No hotbar found with id: {} and no default hotbar set. Cannot apply hotbar to gamer: {}", id, gamer.getName());
        }
    }

    public void applyDefault(Gamer gamer) {
        if (defaultHotbar == null) {
            log.warn("No default hotbar set. Cannot apply default hotbar to gamer: {}", gamer.getName());
            return;
        }

        apply(defaultHotbar, gamer);
    }

    public void apply(Hotbar base, Gamer gamer) {
        var playerId = gamer.getPlayerId();
        var current = gamerHotbarMap.get(playerId);

        if (current != null) {
            if (Objects.equals(current.getId(), base.getId())) {
                return;
            }

            clear(gamer);
        }

        var providers = registry.forContext(base.getId());
        Hotbar snapshot;

        if (providers == null || providers.isEmpty()) {
            snapshot = base;
        } else {
            snapshot = composer.compose(base, gamer, providers);
        }

        snapshot.apply(gamer);
        gamerHotbarMap.put(playerId, snapshot);
    }

    public Hotbar getCurrent(Gamer gamer) {
        return gamerHotbarMap.get(gamer.getPlayerId());
    }

    public void clear(Iterable<Gamer> gamer) {
        for (val g : gamer) {
            clear(g);
        }
    }

    public void clear(Gamer gamer) {
        val player = gamer.getPlayer();
        val playerInventory = player.getInventory();
        val current = getCurrent(gamer);

        if (current == null) return;

        for (val icon : current.getIcons()) {
            val slot = icon.slot();
            playerInventory.setItem(slot, null);
        }

        player.updateInventory();

        gamerHotbarMap.remove(gamer.getPlayerId());
    }
}
