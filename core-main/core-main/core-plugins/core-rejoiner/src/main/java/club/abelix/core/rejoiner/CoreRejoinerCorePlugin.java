package club.abelix.core.rejoiner;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.rejoiner.command.RejoinCommand;
import club.abelix.core.rejoiner.listener.RejoinListener;
import club.abelix.core.rejoiner.manager.RejoinManager;
import lombok.Getter;

public class CoreRejoinerCorePlugin extends CorePlugin {

    @Getter
    private final RejoinManager rejoinManager = new RejoinManager();

    @Override
    public void onEnable() {
        getCore().getPluginManager().registerListener(this, new RejoinListener(this));

        CoreApplication.getInstance().getCommandManager().addCommand(this, new RejoinCommand(this));
    }
}
