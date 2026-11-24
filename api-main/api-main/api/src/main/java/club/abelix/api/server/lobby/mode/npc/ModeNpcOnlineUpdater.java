package club.abelix.api.server.lobby.mode.npc;

import club.abelix.network.online.data.FieldMatchMode;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.StandardServerFields;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.type.mode.Mini;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ModeNpcOnlineUpdater extends BukkitRunnable {

    StatusQueryExecutionService executionService;
    ModeNpcService modeNpcService;

    @Override
    public void run() {
        update(false);
    }

    public void update(boolean refresh) {
        val npcs = modeNpcService.getNpcs();

        npcs.forEach((mode, npc) -> {
            if (npc.getEntity().getViewers().isEmpty()) {
                return;
            }

            val arenaQuery = StatusQueries.parametrizedQuery()
                    .matchMode(StandardServerFields.TAG, FieldMatchMode.ALL)
                    .kind(mode instanceof Mini ? ServerKind.ARENA : ServerKind.PHYSICAL)
                    .tags(List.of(mode.getName()))
                    .build();

            val result = executionService.executeSync(arenaQuery, refresh);

            updateForNpc(npc, result);
        });
    }

    private void updateForNpc(
            final @NotNull ModeNpc npc,
            final @NotNull StatusQueryResult onlineQueryResult
    ) {
        npc.updateOnlineStatus(onlineQueryResult);

        log.info("Updated online status for {} with {}", npc.getMode().getName(), onlineQueryResult);
    }

}
