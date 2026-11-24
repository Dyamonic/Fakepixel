package club.abelix.api.server.base.entity;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Bashmak created this file on 07.02.2024
 **/
public final class FakeEntityTablistTask extends AbstractTask {

    private final NmsApi nmsApi;

    public FakeEntityTablistTask(
            final @NotNull AbstractPlugin plugin,
            final @NotNull NmsApi nmsApi
    ) {
        super(plugin);

        this.nmsApi = nmsApi;

        async();
        timer(0, 20);
    }

    @Override
    protected void run() {
        handleTablistRemove();
    }

    private void handleTablistRemove() {
        val entities = nmsApi.getFakeEntityApi().getTrackedEntities().stream()
                .filter(entity -> entity instanceof FakeEntityPlayer)
                .map(entity -> (FakeEntityPlayer) entity)
                .toList();

        if (entities.isEmpty()) {
            return;
        }

        for (val entity : entities) {
            val tablistEntries = entity.getTablistEntries();

            if (tablistEntries.isEmpty()) {
                continue;
            }

            for (val entry : new ArrayList<>(tablistEntries)) {
                if (!entry.isExpired()) {
                    continue;
                }

                entity.removeTablistEntry(entry);
                entity.hideFromTab(entry.players());
            }
        }
    }
}