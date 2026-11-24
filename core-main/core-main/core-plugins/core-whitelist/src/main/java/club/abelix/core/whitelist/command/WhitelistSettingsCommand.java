package club.abelix.core.whitelist.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.permission.Group;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.whitelist.WhitelistCorePlugin;
import club.abelix.core.whitelist.WhitelistUtility;
import club.abelix.core.whitelist.settings.WhitelistSettings;
import club.abelix.network.type.mode.ServerMode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhitelistSettingsCommand creation on 21.11.2022 at 19:51</p>
 */


@RequiredArgsConstructor
@WithHelp
@NamedCommand("whitelistsettings")
@WithAlias("wlsettings")
@WithAlias("wls")
public class WhitelistSettingsCommand {
    private final WhitelistCorePlugin whitelistCorePlugin;

    private WhitelistSettings getSettingsFromName(@NonNull CoreExecutionSession session) {
        String masterName = session.getArgument(0);

        WhitelistSettings settings;
        if (masterName.equalsIgnoreCase("global")) {
            settings = whitelistCorePlugin.getWhitelistGlobalSettings();
        } else {
            settings = whitelistCorePlugin.getSomeModesSettings().get(
                    WhitelistUtility.getServerMode(session.getArgument(0)));
        }

        if (settings == null) {
            session.sendLocalizedMessage("whitelist_some_modes_settings_not_found");
            return null;
        }

        return settings;
    }

    @SubcommandExecutor("enable")
    @Tabcomplete("[masterName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.whitelistsettings.enable")
    public void settingsEnable(CoreExecutionSession session) {
        WhitelistSettings settings = getSettingsFromName(session);

        if (settings == null) {
            return;
        }

        settings.setEnable(true);
        whitelistCorePlugin.updateConfiguration();
        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "enable", "%value%", true);
    }

    @SubcommandExecutor("disable")
    @Tabcomplete("[masterName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.whitelistsettings.disable")
    public void settingsDisable(CoreExecutionSession session) {
        WhitelistSettings settings = getSettingsFromName(session);

        if (settings == null) {
            return;
        }

        settings.setEnable(false);
        whitelistCorePlugin.updateConfiguration();
        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "enable", "%value%", false);
    }


    @SubcommandExecutor("adduser")
    @WithAlias("addgamer")
    @WithAlias("ag")
    @Tabcomplete("[masterName] [playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.whitelistsettings.adduser")
    public void settingsAddAccessGamer(CoreExecutionSession session) throws CommandTranslationException {
        WhitelistSettings settings = getSettingsFromName(session);

        Gamer gamer = session.getOfflineGamer(1, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadLanguage();
        });

        if (settings == null) {
            return;
        }

        settings.getAccessGamerIdList().add(gamer.getPlayerId());
        whitelistCorePlugin.updateConfiguration();

        if (gamer.isOnline()) {
            gamer.sendLocalizedMessage("whitelist_add_you_in_access_list", "%mode%", session.getArgument(0));
        }

        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "accessGamers", "%value%", settings.getAccessGamerIdList());
    }

    @SubcommandExecutor("removeuser")
    @WithAlias("removegamer")
    @WithAlias("rg")
    @Tabcomplete("[masterName] [playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.whitelistsettings.removeuser")
    public void settingsRemoveAccessGamer(CoreExecutionSession session) throws CommandTranslationException {
        WhitelistSettings settings = getSettingsFromName(session);

        Gamer gamer = session.getOfflineGamer(1, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadLanguage();
        });

        if (settings == null) {
            return;
        }

        settings.getAccessGamerIdList().remove(gamer.getPlayerId());
        whitelistCorePlugin.updateConfiguration();

        if (gamer.isOnline()) {
            gamer.sendLocalizedMessage("whitelist_remove_you_in_access_list", "%mode%", session.getArgument(0));
        }

        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "accessGamers", "%value%", settings.getAccessGamerIdList());
    }

    @SubcommandExecutor("setminimalgroup")
    @WithAlias("minimalgroup")
    @WithAlias("smg")
    @WithAlias("mg")
    @Tabcomplete("[masterName] [group]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.whitelistsettings.setminimalgroup")
    public void settingsSetMinimalGroup(CoreExecutionSession session) {
        WhitelistSettings settings = getSettingsFromName(session);

        Group group = Group.valueOf(session.getArgument(1));

        if (settings == null) {
            return;
        }

        settings.setMinimalAccessGroup(group);
        whitelistCorePlugin.updateConfiguration();

        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "minmalGroup", "%value%", settings.getMinimalAccessGroup().getName());
    }

    @SubcommandExecutor("setkickmsg")
    @Tabcomplete("[masterName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.whitelistsettings.setkickmsg")
    public void settingsSetLocalizationMessageFromKick(CoreExecutionSession session) {
        WhitelistSettings settings = getSettingsFromName(session);
        String localizationMessage = session.getArgument(1);

        if (settings == null) {
            return;
        }

        settings.setKickMessageLocalizationKey(localizationMessage);
        whitelistCorePlugin.updateConfiguration();

        session.sendLocalizedMessage("whitelist_settings_edit", "%settings%", "KickMessageLocalizationKey(", "%value%", settings.getKickMessageLocalizationKey());
    }

    @SubcommandExecutor("create")
    @Tabcomplete("[masterName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.whitelistsettings.create")
    public void settingsNewCreation(CoreExecutionSession session) {
        ServerMode serverMode = WhitelistUtility.getServerMode(session.getArgument(0));

        WhitelistSettings settings = new WhitelistSettings("whitelist_kick",
                new ArrayList<>(), Group.ADMINISTRATOR, false);

        whitelistCorePlugin.getSomeModesSettings().put(serverMode, settings);
        whitelistCorePlugin.updateConfiguration();

        session.sendLocalizedMessage("whitelist_create_somemode", "%mode%", serverMode.getDisplayName());
    }
}
