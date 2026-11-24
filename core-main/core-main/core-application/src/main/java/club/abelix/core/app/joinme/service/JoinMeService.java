package club.abelix.core.app.joinme.service;

import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.joinme.JoinMeRecord;
import club.abelix.network.common.enums.GameState;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.val;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JoinMeService {

    private final Cache<Gamer, JoinMeRecord> joinMeCache = Caffeine.newBuilder()
            .removalListener(new RemoveListener())
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    public Optional<JoinMeRecord> findByArenaServer(ArenaServer server) {
        return joinMeCache.asMap()
                .values()
                .stream()
                .filter(record -> record.getServer().equals(server))
                .findFirst();
    }

    public void startJoinMe(Gamer gamer, AbstractServer server) {
        if (!server.getType().isMini()) {
            gamer.sendLocalizedMessage("joinme_error_not_mini");
            return;
        }

        if (server instanceof ArenaServer arenaServer) {
            if (arenaServer.getState().equals(GameState.GAME) || arenaServer.getState().equals(GameState.STARTING)) {
                gamer.sendLocalizedMessage("joinme_error_arena_in_game");
                return;
            }
        }

        val record = new JoinMeRecord();
        record.setGamer(gamer);
        record.setServer(server);

        joinMeCache.put(gamer, record);
        sendMessage(record);
    }

    public boolean hasActiveJoinMe(Gamer gamer) {
        return joinMeCache.getIfPresent(gamer) != null;
    }

    public void stopJoinMe(Gamer gamer) {
        val record = joinMeCache.getIfPresent(gamer);

        if (record == null) return;

        joinMeCache.invalidate(gamer);
    }

    public void join(Gamer gamer, Gamer target) {
        val record = joinMeCache.getIfPresent(target);

        if (record == null) {
            gamer.sendLocalizedMessage("joinme_error_no_active", "%lfp%", target.getDisplayName());
            return;
        }

        gamer.sendTo(record.getServer());
        gamer.sendLocalizedMessage("joinme_teleported", "%lfp%", target.getDisplayName());
        target.sendLocalizedMessage("joinme_someone_joined", "%lfp%", gamer.getDisplayName());
    }

    private void sendMessage(JoinMeRecord record) {
        ServerManager.IMP.broadcastChatLocalized("joinme_broadcast",
                "%lfp%", record.getGamer().getDisplayName(),
                "%join%", record.getGamer().getName(),
                "%mode%", record.getServer().getMode().getDisplayName(),
                "%master%", record.getServer().getMaster().getDisplayName(),
                "%server%", record.getServer().getInfo().getServerName()
        );
    }

    private static class RemoveListener implements RemovalListener<Gamer, JoinMeRecord> {

        @Override
        public void onRemoval(Gamer gamer, JoinMeRecord joinMeRecord, RemovalCause removalCause) {
            if (gamer == null || joinMeRecord == null) {
                return;
            }

            gamer.sendLocalizedMessage("joinme_ended");
        }
    }
}
