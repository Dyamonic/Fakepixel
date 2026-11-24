package club.abelix.antivpn;

import club.abelix.antivpn.commands.ASNWhitelistCommand;
import club.abelix.antivpn.commands.CheckVPNCommand;
import club.abelix.antivpn.listener.PlayerJoinListener;
import club.abelix.antivpn.manager.AntiVPNManager;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.CorePlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import w.util.Env;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Getter
@Slf4j
public class AntiVPNCorePlugin extends CorePlugin {

    private AntiVPNManager antiVPNManager;

    @Override
    public void onEnable() {
        if (!Boolean.parseBoolean(Env.getString("ANTIVPN_ENABLED", "false"))) {
            log.info("Not enabling AntiVPN (env ANTIVPN_ENABLED is set to false)");
            return;
        }

        antiVPNManager = new AntiVPNManager(Env.getString("GEO_API_URL"));
        antiVPNManager.loadWhitelist();

        registerListeners();
        registerCommands();
    }

    private void registerListeners() {
        getCore().getPluginManager().registerListener(
                this, new PlayerJoinListener(antiVPNManager)
        );
    }

    private void registerCommands() {
        CoreApplication.getInstance().getCommandManager().addCommand(this, new CheckVPNCommand(antiVPNManager));
        CoreApplication.getInstance().getCommandManager().addCommand(this, new ASNWhitelistCommand(antiVPNManager));
    }

}
