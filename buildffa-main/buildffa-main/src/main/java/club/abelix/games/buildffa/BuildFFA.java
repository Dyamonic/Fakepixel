package club.abelix.games.buildffa;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.di.api.annotation.ScanEntryPoint;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.network.common.logging.Logging;

@ScanEntryPoint
public final class BuildFFA extends AbstractPlugin {

    @Override
    public void onLoad() {
        CoreUtil.initializeServerData();
        StatsManager.INSTANCE.loadStatsModes();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!CoreUtil.isUncommon()) {
            Logging.IMP.error("This plugin support only uncommon!");
            getServer().getPluginManager().disablePlugin(this);
        }

        GamerSessionManager.INSTANCE.register(BuildFFAGamer.class);
        new BuildFFAGame(this).loadGame();
    }
}
