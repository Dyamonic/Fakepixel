package club.abelix.api.base.addon.impl;

import club.abelix.api.base.addon.PluginAddon;
import club.abelix.api.base.addon.PluginAddonService;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.Bukkit;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 16/01/2023 18:40
 */
public class PluginAddonServiceImpl implements PluginAddonService {

    @Override
    public void tryLoad(@NonNull Class<? extends PluginAddon> addonClass) {
        Logging.IMP.info("Try load addon in class " + addonClass.getName());

        try {
            PluginAddon addon = addonClass.newInstance();
            Logging.IMP.info("Success created new instance of " + addonClass.getName() + " class");

            if (Bukkit.getPluginManager().isPluginEnabled(addon.getRequiredPlugin())) {
                Logging.IMP.info("Try boot addon in class " + addonClass.getName());
                addon.boot();
                Logging.IMP.info("Success boot addon in class " + addonClass.getName());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
