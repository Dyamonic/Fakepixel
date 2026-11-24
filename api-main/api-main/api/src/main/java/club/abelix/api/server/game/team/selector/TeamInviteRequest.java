package club.abelix.api.server.game.team.selector;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.manager.GameGroupSelectorManager;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "task")
@Getter
public class TeamInviteRequest {

    private final Gamer sender, receiver;
    private BukkitTask task;

    public void startWatcher(@NonNull Plugin plugin, @NonNull GameGroupSelectorManager manager) {
        task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (manager.removeInvite(this)) {
                sender.sendLocalizedMessage("team_selector_invite_ignored_sender", "%name%", receiver.getDisplayName());
                receiver.sendLocalizedMessage("team_selector_invite_ignored_receiver", "%name%", sender.getDisplayName());
            }
        }, 30 * 20);
    }

    public void cancel() {
        task.cancel();
    }
}
