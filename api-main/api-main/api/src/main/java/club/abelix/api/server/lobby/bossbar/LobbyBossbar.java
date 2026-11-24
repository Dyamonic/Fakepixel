package club.abelix.api.server.lobby.bossbar;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.commons.language.Language;
import com.google.common.collect.Iterables;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LobbyBossbar {

    private static final Iterator<String> COLORS = Iterables.cycle(
            "pink", "blue", "red", "green", "yellow", "purple", "white"
    ).iterator();

    private final AbstractPlugin plugin;
    private final Language language;
    private final List<String> messages = new ArrayList<>();
    private FakeBossbar bossbar;
    private Iterator<String> bossbars;

    public LobbyBossbar(@NonNull AbstractPlugin plugin, @NonNull Language language) {
        this.plugin = plugin;
        this.language = language;
    }

    public void send(@NonNull Gamer gamer) {
        if (bossbar != null) {
            bossbar.addReceiver(gamer.getPlayer());
        }
    }

    public void hide(@NonNull Gamer gamer) {
        if (bossbar != null) {
            bossbar.removeReceiver(gamer.getPlayer());
        }
    }

    public void reload() {
        messages.clear();
        messages.addAll(language.getMessageList("lobby_bossbar_messages"));

        bossbars = messages.iterator();
    }

    public LobbyBossbar start() {
        bossbar = FalconAPI.getNmsApi().getBossbarApi().createBossbar("§cЗагрузка данных...", 1);
        bossbar.setBarStyle("segmented_10");

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                if (bossbars == null || !bossbars.hasNext()) {
                    reload();
                }

                String next = bossbars.next();

                bossbar.setTitle(next);
                bossbar.setBarColor(COLORS.next());
            }
        }.timer(0, 20 * 10);

        return this;
    }

}
