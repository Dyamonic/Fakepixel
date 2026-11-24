package club.abelix.core.votifer;

import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.core.app.api.plugin.CorePlugin;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import club.abelix.core.votifer.command.VotifierCommand;
import club.abelix.core.votifer.nats.VotifierNats;
import club.abelix.core.votifer.manager.VoteManager;
import lombok.val;

import java.util.HashMap;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class VotifierCorePlugin extends CorePlugin {

    @Getter
    VoteManager voteManager;

    private VotifierNats votifierHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        val configuration = getConfig();

        VoteSqlHandler.INSTANCE.loadPlatformList();

        voteManager = new VoteManager(this);

        //перезагружаем конфиг и запускаем таск
        voteManager.reloadConfiguration();
//        voteManager.runAutoMessageTask();

        val subject = configuration.getString("natsSubject");
        if (subject != null && !subject.isBlank()) {
            votifierHandler = new VotifierNats(this, subject);
            votifierHandler.start();
        }

        //регаем команду
        getCore().getCommandManager().addCommand(new VotifierCommand(voteManager));
    }

    @Override
    public void onDisable() {
        if (votifierHandler != null) {
            votifierHandler.stop();
            votifierHandler = null;
        }
    }

}
