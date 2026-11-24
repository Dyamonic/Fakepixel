package club.abelix.core.automessage;

import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.automessage.manager.AutoMessageManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMessageCorePlugin extends CorePlugin {

    AutoMessageManager autoMessageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        autoMessageManager = new AutoMessageManager(this);
        autoMessageManager.reloadConfiguration();
    }

    @Override
    public void onDisable() {
        autoMessageManager.stop();
    }
}
