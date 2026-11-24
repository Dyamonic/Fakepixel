package club.abelix.api.base.addon.gadgetsmenu;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.addon.PluginAddon;
import club.abelix.api.server.base.command.DonateCommand;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 16/01/2023 18:40
 */
public class GMenuAddon extends PluginAddon {
    public GMenuAddon() {
        super("GadgetsMenu");
    }

    @Override
    public void boot() {
        new GadgetsMenuEconomyProvider(FalconAPI.getInstance());
        FalconAPI.getInstance().getCommandManager().addCommand(DonateCommand.class);
    }
}
