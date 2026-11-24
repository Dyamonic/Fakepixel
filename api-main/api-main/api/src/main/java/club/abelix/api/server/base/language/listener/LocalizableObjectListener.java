package club.abelix.api.server.base.language.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerLanguageChangeEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.base.language.LocalizableObject;
import club.abelix.api.server.base.language.LocalizableObjectManager;
import club.abelix.api.server.base.language.event.LocalizableObjectCreateEvent;
import club.abelix.api.server.base.language.event.LocalizableObjectRemoveEvent;
import club.abelix.commons.language.Language;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LocalizableObjectListener extends YListener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        for (LocalizableObject object : LocalizableObjectManager.IMP.getObjects()) {
            object.send(gamer);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        for (LocalizableObject object : LocalizableObjectManager.IMP.getObjects()) {
            object.remove(gamer);
        }
    }

    @EventHandler
    public void onLocalizableObjectCreate(LocalizableObjectCreateEvent event) {
        LocalizableObject object = event.getObject();

        Gamer.getGamers().forEach(object::send);
    }

    @EventHandler
    public void onLocalizableObjectRemove(LocalizableObjectRemoveEvent event) {
        LocalizableObject object = event.getObject();

        Gamer.getGamers().forEach(object::remove);
    }

    @EventHandler
    public void onLanguageChange(GamerLanguageChangeEvent event) {
        Gamer gamer = event.getGamer();
        Player player = gamer.getPlayer();

        Language oldLanguage = event.getOldLanguage();

        // пересоздаем объекты для пользователя при обновлении языка
        for (LocalizableObject object : LocalizableObjectManager.IMP.getObjects()) {
            object.remove(oldLanguage, player);
            object.send(gamer);
        }
    }

}
