package club.abelix.core.whitelist;

import club.abelix.commons.data.permission.Group;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.Async;
import club.abelix.core.app.api.config.Configuration;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.whitelist.command.WhitelistCommand;
import club.abelix.core.whitelist.command.WhitelistSettingsCommand;
import club.abelix.core.whitelist.listeners.GlobalWhitelistListener;
import club.abelix.core.whitelist.listeners.SomeModesWhitelistListener;
import club.abelix.core.whitelist.settings.WhitelistSettings;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.common.thread.ThreadUncaughtExceptionHandler;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.ServerMode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhitelistCorePlugin creation on 21.11.2022 at 17:46</p>
 */

@NoArgsConstructor
public class WhitelistCorePlugin extends CorePlugin {
    // Настройки вайтлиста, на разных режимах работы
    @Getter
    private final HashMap<ServerMode, WhitelistSettings> someModesSettings = new HashMap<>();
    @Getter
    private final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("[Core/FM] Whitelist plugin executor #%1$d")
                    .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
                    .build()
    );
    @Getter
    private WhitelistMode currentlyMode;
    @Getter
    private WhitelistSettings whitelistGlobalSettings;

    /**
     * Проверяет может ли игрок находится тут по настройке
     *
     * @param settings настройка
     * @param gamer    игрок
     * @return true - может, false - не может
     */
    public static boolean getGamerAvailability(@NonNull WhitelistSettings settings, Gamer gamer) {
        if (!settings.isEnable()) {
            return true;
        }

        return gamer != null && gamer.hasPermission("core.whitelist.bypass");
    }

    private void reloadConfiguration() {
        this.currentlyMode = WhitelistMode.valueOf(this.getConfig().getString("mode"));

        this.whitelistGlobalSettings = this.getWhitelistSettingsFromConfigurationSection(this.getConfig().getSection("global"));

        Configuration modesSettings = this.getConfig().getSection("modes");
        for (String serverModeType : modesSettings.getKeys()) {
            ServerMode serverMode;

            try {
                serverMode = WhitelistUtility.getServerMode(serverModeType);
            } catch (IllegalArgumentException exception) {
                Logging.IMP.error("[Whitelist] Unknown server mode {}", serverModeType);
                continue;
            }

            someModesSettings.put(serverMode, this.getWhitelistSettingsFromConfigurationSection(
                    modesSettings.getSection(serverModeType)
            ));
        }
    }

    private WhitelistSettings getWhitelistSettingsFromConfigurationSection(@NonNull Configuration configurationSection) {
        return new WhitelistSettings(
                configurationSection.getString("kick_message_key"),
                configurationSection.getIntList("access_users"),
                Group.valueOf(configurationSection.getString("minimal_group")),
                configurationSection.getBoolean("enable"));
    }

    private void updateWhiteListSettingsInSection(@NonNull Configuration configurationSection, @NonNull WhitelistSettings whitelistSettings) {
        configurationSection.set("kick_message_key", whitelistSettings.getKickMessageLocalizationKey());
        configurationSection.set("access_users", whitelistSettings.getAccessGamerIdList());
        configurationSection.set("minimal_group", whitelistSettings.getMinimalAccessGroup().name());
        configurationSection.set("enable", whitelistSettings.isEnable());
    }

    private void registrationCommands() {
        this.getCore().getCommandManager().addCommand(this, new WhitelistCommand(this));
        this.getCore().getCommandManager().addCommand(this, new WhitelistSettingsCommand(this));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfiguration();
        registrationCommands();
        updateModeListeners();

        // Запускается немного позже,тк после загрузки кора, игроки загружаются не моментально
        Async.runLater(this::checkGamersToAvailableInMode, 15, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        // На всякий сохраним конфигурацию
        updateConfiguration();
    }

    /**
     * Кикает игроков, в зависимости от режима вайтлиста
     */
    public void checkGamersToAvailableInMode() {
        this.executorService.submit(() -> {
            switch (this.currentlyMode) {
                case SOME_MODES -> {
                    for (ServerMode serverMode : this.getSomeModesSettings().keySet()) {
                        WhitelistSettings settings = this.getSomeModesSettings().get(serverMode);

                        if (!settings.isEnable()) {
                            continue;
                        }

                        ServerManager.IMP.getServers(serverMode)
                                .forEach(abstractServer -> abstractServer.getGamers().stream()
                                        .filter(gamer -> !WhitelistCorePlugin.getGamerAvailability(settings, gamer))
                                        .forEach(gamer -> {
                                            gamer.sendTo(Lobby.HUB);
                                            gamer.sendLocalizedMessage(settings.getKickMessageLocalizationKey());
                                        }));
                    }
                }

                case GLOBAL -> ServerManager.IMP.getProxies()
                        .forEach(proxy -> proxy.getGamers()
                                .stream()
                                .filter(gamer -> !WhitelistCorePlugin.getGamerAvailability(this.whitelistGlobalSettings, gamer))
                                .forEach(gamer -> gamer.localizedKick(this.whitelistGlobalSettings.getKickMessageLocalizationKey())));
            }

        });
    }

    /**
     * Переключить режим вайтлиста
     *
     * @param newMode новый режим вайтлиста
     */
    public void switchMode(@NonNull WhitelistMode newMode) {
        if (this.currentlyMode == newMode) {
            return;
        }
        this.currentlyMode = newMode;
        updateModeListeners();

        this.updateConfiguration();

        // Кикаем пользователей, в зависимости от выбранного режима
        this.checkGamersToAvailableInMode();
    }

    /**
     * Изменить режим, с оповещением игроков
     *
     * @param newMode новый режим вайтлиста
     * @param seconds таймер
     */
    public void timerSwitchMode(@NonNull WhitelistMode newMode, int seconds) {
        this.executorService.submit(() -> {
            int remainedSeconds = seconds;

            while (remainedSeconds >= 0) {
                remainedSeconds -= 1;

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignore) {
                }


                if (remainedSeconds != 60 &&
                        remainedSeconds != 45 &&
                        remainedSeconds != 30 &&
                        remainedSeconds > 15) {
                    continue;
                }

                int finalRemainedSeconds = remainedSeconds;

                switch (newMode) {
                    case SOME_MODES -> {
                        for (ServerMode serverMode : this.getSomeModesSettings().keySet()) {
                            WhitelistSettings settings = this.getSomeModesSettings().get(serverMode);

                            if (!settings.isEnable()) {
                                continue;
                            }

                            ServerManager.IMP.getServers(serverMode)
                                    .stream().map(AbstractConnectable::getGamers)
                                    .forEach(gamers -> gamers
                                            .forEach(gamer -> {
                                                String secondsWord = Duration.durationWordsSec(gamer.getLanguage(), finalRemainedSeconds);
                                                gamer.sendLocalizedMessage("whitelist_current_mode_start_technical_works",
                                                        "%duration%", "%d %s".formatted(finalRemainedSeconds, secondsWord));
                                            }));
                        }

                    }

                    case GLOBAL -> ServerManager.IMP.getProxies().forEach(proxy -> proxy.getGamers()
                            .forEach(gamer -> {
                                String secondsWord = Duration.durationWordsSec(gamer.getLanguage(), finalRemainedSeconds);
                                gamer.sendLocalizedMessage("whitelist_project_start_technical_works",
                                        "%duration%", "%d %s".formatted(finalRemainedSeconds, secondsWord));
                            }));
                }
            }

            switchMode(newMode);
        });

    }

    /**
     * Регистрирует листенеры в зависимости от текущего типа вайтлиста
     */
    public void updateModeListeners() {
        this.getCore().getPluginManager().unregisterListeners(this);
        this.getWhitelistGlobalSettings().setEnable(false);

        switch (this.currentlyMode) {
            case GLOBAL -> {
                this.getWhitelistGlobalSettings().setEnable(true);
                this.getCore().getPluginManager().registerListener(this, new GlobalWhitelistListener(this));
            }
            case SOME_MODES ->
                    this.getCore().getPluginManager().registerListener(this, new SomeModesWhitelistListener(this));
        }
    }

    /**
     * Обновляет конфигурацию вайтлиста в файле настроек
     */
    public void updateConfiguration() {
        this.getConfig().set("mode", this.currentlyMode.name());

        updateWhiteListSettingsInSection(this.getConfig().getSection("global"), this.whitelistGlobalSettings);

        Configuration newModesConfigurationSettings = new Configuration(this.getConfig());

        for (ServerMode serverMode : this.getSomeModesSettings().keySet()) {
            Configuration modeSettings = new Configuration();
            this.updateWhiteListSettingsInSection(modeSettings, this.getSomeModesSettings().get(serverMode));
            newModesConfigurationSettings.set(serverMode.getName(), modeSettings.getRawMap());
        }

        this.getConfig().set("modes", newModesConfigurationSettings.getRawMap());

        saveConfig();
    }

}
