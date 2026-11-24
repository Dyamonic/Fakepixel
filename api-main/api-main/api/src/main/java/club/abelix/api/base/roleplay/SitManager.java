package club.abelix.api.base.roleplay;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class SitManager {

    public static final SitManager INSTANCE = new SitManager();

    // -----------------------------------------------------

    private final Map<Gamer, SitSession> sitSessionMap = new HashMap<>();

    public void register(@NonNull AbstractPlugin plugin) {
        new SitListener(plugin);

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                sitSessionMap.keySet().forEach(gamer ->
                        gamer.sendLocalizedActionBar("sit_actionbar"));
            }
        }.timer(5, 5);

        plugin.getCommandManager().addCommand(SitCommand.class);
    }

    public boolean isSitting(@NonNull Gamer gamer) {
        return sitSessionMap.containsKey(gamer);
    }

    public void sit(@NonNull Gamer gamer) {
        gamer.sendLocalizedMessage("sit_down");

        sitSessionMap.put(gamer, new SitSession(gamer));
    }

    public void eject(@NonNull Gamer gamer) {
        SitSession session = sitSessionMap.remove(gamer);

        if (session != null) {
            gamer.sendLocalizedMessage("sit_up");

            session.remove();
        }
    }

}
