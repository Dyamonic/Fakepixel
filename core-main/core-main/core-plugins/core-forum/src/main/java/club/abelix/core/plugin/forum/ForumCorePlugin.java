package club.abelix.core.plugin.forum;

import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.plugin.forum.command.ForumCommand;
import club.abelix.core.plugin.forum.config.ForumConfig;
import club.abelix.core.plugin.forum.listener.ForumListener;
import club.abelix.core.plugin.forum.mailer.Mailer;
import club.abelix.core.plugin.forum.manager.ForumSqlManager;
import club.abelix.core.plugin.forum.util.TempMailUtil;
import club.abelix.core.plugin.forum.util.XenApi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import w.util.Env;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class ForumCorePlugin extends CorePlugin {

    @Getter
    private Mailer mailer;

    @Getter
    private XenApi xenApi;

    @Getter
    private ForumSqlManager forumSqlManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ForumConfig config;
        try {
            config = loadConfiguration(new File(getDataFolder(), "config.yml").toPath());
        } catch (Exception e) {
            log.error("Unable to load Forum configuration. Plugin will be disabled!", e);
            getCore().getPluginManager().disablePlugin(this);
            return;
        }

        if (!config.getEnabled()) {
            getCore().getPluginManager().disablePlugin(this);
            return;
        }

        forumSqlManager = new ForumSqlManager();
        mailer = new Mailer();

        TempMailUtil.loadTempMailDomains();

        val host = Env.getString("TM_HOST");
        val mail = Env.getString("TM_MAIL");
        val pass = Env.getString("TM_PASSWORD");
        val user = Env.getString("TM_USERNAME");

        mailer.setSettings(mail, user, pass, host, config.getMailFrom());

        // xenforo api key
        xenApi = new XenApi(config.getForum().getServer(), Env.getString("FORUM_API_KEY"));
        xenApi.loadRanks(config.getRanks());

        //forum listener
        val core = getCore();

        core.getPluginManager().registerListener(new ForumListener(xenApi, forumSqlManager));
        core.getCommandManager().addCommand(new ForumCommand(this, xenApi));
    }

    public ForumConfig loadConfiguration(Path path) throws Exception {
        Constructor ctor = new Constructor(ForumConfig.class, new LoaderOptions());
        Yaml yaml = new Yaml(ctor);

        try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
            return yaml.load(in);
        }
    }
}
