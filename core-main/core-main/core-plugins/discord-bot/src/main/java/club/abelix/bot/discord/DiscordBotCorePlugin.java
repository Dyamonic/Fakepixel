package club.abelix.bot.discord;

import club.abelix.bot.discord.command.DiscordCommand;
import club.abelix.bot.discord.config.DiscordSyncBotConfig;
import club.abelix.bot.discord.listener.GameBotListener;
import club.abelix.bot.discord.listener.GamerNotifyListener;
import club.abelix.bot.discord.listener.StaffChatListener;
import club.abelix.core.app.api.plugin.CorePlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Getter
public class DiscordBotCorePlugin extends CorePlugin {

    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DiscordSyncBotConfig config;

        try {
            config = load(new File(getDataFolder(), "config.yml").toPath());
        } catch (Exception e) {
            log.error("Unable to load Discord Bot configuration. Plugin will be disabled!", e);

            getCore().getPluginManager().disablePlugin(this);

            return;
        }

        discordBot = new DiscordBot(config,
                getCore().getRanksService(),
                getCore().getUserRanksService());

        this.getCore().getPluginManager().registerListener(this, new GameBotListener(discordBot));
        this.getCore().getPluginManager().registerListener(this, new StaffChatListener(discordBot));
        this.getCore().getPluginManager().registerListener(this, new GamerNotifyListener(discordBot));

        this.getCore().getCommandManager().addCommand(this, new DiscordCommand(discordBot));

        discordBot.start();
        // discordBot.postInitialization();
    }

    @Override
    public void onDisable() {
        if (discordBot != null) {
            discordBot.stop();
        }
    }

    public DiscordSyncBotConfig load(Path path) throws Exception {
        Constructor ctor = new Constructor(DiscordSyncBotConfig.class, new LoaderOptions());
        Yaml yaml = new Yaml(ctor);

        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
            return yaml.load(in);
        }
    }

}
