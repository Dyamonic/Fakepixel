package club.abelix.api.base.addon.gadgetsmenu;

import club.abelix.api.base.gamer.Gamer;
import com.yapzhenyie.GadgetsMenu.economy.GEconomyProvider;
import com.yapzhenyie.GadgetsMenu.player.OfflinePlayerManager;
import org.bukkit.plugin.Plugin;

public class GadgetsMenuEconomyProvider extends GEconomyProvider {

    public GadgetsMenuEconomyProvider(Plugin plugin) {
        super(plugin, "Abelix");
        GEconomyProvider.setMysteryDustStorage(this);
    }

    @Override
    public int getMysteryDust(OfflinePlayerManager offlinePlayerManager) {
        return Gamer.getGamer(offlinePlayerManager.getName()).getMysteryDust();
    }

    @Override
    public boolean addMysteryDust(OfflinePlayerManager offlinePlayerManager, int i) {
        Gamer.getGamer(offlinePlayerManager.getName()).addMysteryDust(i);
        return true;
    }

    @Override
    public boolean setMysteryDust(OfflinePlayerManager offlinePlayerManager, int i) {
        Gamer gamer = Gamer.getGamer(offlinePlayerManager.getName());
        if (gamer.getMysteryDust() > 0) {
            gamer.takeMysteryDust(gamer.getMysteryDust());
        }
        gamer.addMysteryDust(i);
        return true;
    }

    @Override
    public boolean removeMysteryDust(OfflinePlayerManager offlinePlayerManager, int i) {
        Gamer.getGamer(offlinePlayerManager.getName()).takeMysteryDust(i);
        return true;
    }
}
