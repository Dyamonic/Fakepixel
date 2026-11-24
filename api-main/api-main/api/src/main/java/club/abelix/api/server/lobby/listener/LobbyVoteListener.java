package club.abelix.api.server.lobby.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.cron.CronService;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.vote.gui.VoteGui;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LobbyVoteListener extends YListener {

    CronService cronService;

    public LobbyVoteListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
        this.cronService = new CronService(StarshipBukkit.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(GamerJoinEvent event) {
        val gamer = event.getGamer();

        cronService.check(gamer, "vote_advertisement").thenAccept(result -> {
            if (!result) return;

            HeartBeat.runLaterSync(this.plugin, () -> new VoteGui(gamer.getPlayer()), 50L);
        });
    }
}
