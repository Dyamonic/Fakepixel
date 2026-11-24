package club.abelix.games.fastbridge;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.di.api.annotation.ScanEntryPoint;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ScanEntryPoint
public class FastBridge extends AbstractPlugin {

    @Getter
    private static FastBridge instance;

    @Override
    public void onLoad() {
        CoreUtil.initializeServerData();
        StatsManager.INSTANCE.loadStatsModes();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!CoreUtil.isUncommon()) {
            log.error("This plugin support only uncommon!");
            getServer().getPluginManager().disablePlugin(this);
        }

        GamerSessionManager.INSTANCE.register(FastBridgeGamer.class);
        new FastBridgeGame(instance).loadGame();
    }


}
