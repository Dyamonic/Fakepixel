package club.abelix.api.server.lobby.mode.npc;

import club.abelix.api.AbstractPlugin;
import club.abelix.commons.data.skin.Skin;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.type.mode.ServerMode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModeNpcService {

    private final Map<ServerMode, ModeNpc> modeNpcInfoRecordMap = new HashMap<>();

    ModeNpcOnlineUpdater onlineUpdater;

    public ModeNpcService(
            final @NotNull AbstractPlugin plugin,
            final @NotNull StatusQueryExecutionService executionService
    ) {
        this.onlineUpdater = new ModeNpcOnlineUpdater(
                executionService,
                this
        );

        onlineUpdater.runTaskTimerAsynchronously(plugin, 20L, 20 * 10);
    }

    public ModeNpc spawn(
            final @NotNull Location location,
            final @NotNull Skin skin,
            final @NotNull ServerMode serverMode
    ) {
        log.info("Spawning new npc for mode {}", serverMode);

        val modeNpc = new ModeActionNpc(serverMode);

        modeNpc.createEntities(
                skin,
                location
        );

        modeNpcInfoRecordMap.put(modeNpc.getMode(), modeNpc);
        return modeNpc;
    }

    public void triggerOnlineUpdate() {
        onlineUpdater.update(true);
        log.info("Forcing online update for {} npcs", modeNpcInfoRecordMap.size());
    }

    public Map<ServerMode, ModeNpc> getNpcs() {
        return Map.copyOf(modeNpcInfoRecordMap);
    }

}
