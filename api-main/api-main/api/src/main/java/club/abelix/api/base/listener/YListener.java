package club.abelix.api.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import lombok.NonNull;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class YListener implements Listener {

    protected final AbstractPlugin plugin;
    protected boolean registered = false;

    /**
     * <br>Лучше не использовать этот конструктор
     * <br>На всех режимах в конструкторах у нас либо плагин (SkyBlockPlugin, AnarchyPlugin),
     * <br>либо игра (BedWarsGame, SkyWarsGame).
     * <br>
     * <br>Если игра вдруг не запустится, то листенеры не удалятся и будет дофига
     * <br>путающих разработчика ошибок. короче говоря - я хз. я оставлю временно, но с @Deprecated
     */
    @Deprecated
    public YListener() {
        this(FalconAPI.getInstance());
    }

    @Deprecated
    public YListener(@NonNull Plugin plugin) {
        this((AbstractPlugin) plugin);
    }

    public YListener(@NonNull AbstractPlugin plugin) {
        this.plugin = plugin;

        register();
    }

    public boolean isRegistered() {
        return registered;
    }

    public void register() {
        if (registered) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registered = true;
    }

    public void unregister() {
        if (!registered) {
            return;
        }

        HandlerList.unregisterAll(this);
        registered = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
