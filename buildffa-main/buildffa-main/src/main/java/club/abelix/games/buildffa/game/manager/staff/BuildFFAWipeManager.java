package club.abelix.games.buildffa.game.manager.staff;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.board.BuildFFABoard;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.dto.dao.impl.KitDAOImpl;
import club.abelix.games.buildffa.game.dto.dao.impl.SkinDAOImpl;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Uncommon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public final class BuildFFAWipeManager {
    private static final long CONFIRMATION_MILLIS = 30_000L;
    private final BuildFFAGame game;
    private final UUID serverSession = UUID.randomUUID();
    private final Map<UUID, Confirmation> confirmations = new HashMap<>();
    private final Map<Integer, CompletableFuture<Void>> wipes = new HashMap<>();

    public BuildFFAWipeManager(BuildFFAGame game) {
        this.game = game;
    }

    public void execute(Gamer staff, Gamer target, boolean confirm) {
        Player staffPlayer = staff.getPlayer();
        UUID staffId = staffPlayer.getUniqueId();
        UUID targetId = resolveUuid(target);
        if (targetId == null) {
            confirmations.remove(staffId);
            staff.sendLocalizedMessage("buildffa_wipe_player_not_found", "%player%", target.getPlayerName());
            return;
        }
        long now = System.currentTimeMillis();
        Confirmation pending = confirmations.get(staffId);
        if (!confirm) {
            confirmations.put(staffId, new Confirmation(targetId, serverSession, now + CONFIRMATION_MILLIS));
            staff.sendLocalizedMessage("buildffa_wipe_confirmation", "%player%", target.getPlayerName());
            return;
        }
        confirmations.remove(staffId);
        if (pending == null || pending.expiresAt < now || !pending.serverSession.equals(serverSession)) {
            staff.sendLocalizedMessage("buildffa_wipe_confirmation_expired", "%player%", target.getPlayerName());
            return;
        }
        if (!pending.targetId.equals(targetId)) {
            staff.sendLocalizedMessage("buildffa_wipe_confirmation", "%player%", target.getPlayerName());
            return;
        }
        if (wipes.containsKey(target.getPlayerId())) {
            staff.sendLocalizedMessage("buildffa_wipe_started", "%player%", target.getPlayerName());
            return;
        }
        staff.sendLocalizedMessage("buildffa_wipe_started", "%player%", target.getPlayerName());
        CompletableFuture<Void> wipe = stage("quests",
                () -> game.getQuestManager().resetPlayerProgress(targetId, target.getPlayerId()))
                .thenCompose(ignored -> stage("kits", () -> KitDAOImpl.INSTANCE.wipePlayer(target.getPlayerId())))
                .thenCompose(ignored -> stage("skins", () -> SkinDAOImpl.getInstance().wipePlayer(target.getPlayerId())))
                .thenCompose(ignored -> stage("profile", () -> applySync(target, targetId)));
        wipes.put(target.getPlayerId(), wipe);
        wipe.whenComplete((ignored, throwable) -> Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            wipes.remove(target.getPlayerId(), wipe);
            if (throwable == null) {
                staff.sendLocalizedMessage("buildffa_wipe_success", "%player%", target.getPlayerName());
                Logging.IMP.info("BuildFFA wipe staff={} target={} timestamp={} result=success", staffId, targetId, System.currentTimeMillis());
            } else {
                staff.sendLocalizedMessage("buildffa_wipe_failed", "%player%", target.getPlayerName());
                Throwable failure = unwrap(throwable);
                String stage = failure instanceof WipeStageException ? ((WipeStageException) failure).stage : "unknown";
                Logging.IMP.error("BuildFFA wipe staff={} target={} timestamp={} result=failure stage={}",
                        staffId, targetId, System.currentTimeMillis(), stage);
            }
        }));
    }

    private CompletableFuture<Void> stage(String name, Supplier<CompletableFuture<Void>> operation) {
        try {
            return operation.get().handle((ignored, throwable) -> {
                if (throwable != null) throw new CompletionException(new WipeStageException(name, unwrap(throwable)));
                return null;
            });
        } catch (Throwable throwable) {
            CompletableFuture<Void> failure = new CompletableFuture<>();
            failure.completeExceptionally(new WipeStageException(name, unwrap(throwable)));
            return failure;
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    private void applyOnlineState(Gamer gamer, UUID targetId) {
        setStat(gamer, "rating", AbstractDivision.NOVICE.getRequiredRating());
        setStat(gamer, "division", AbstractDivision.NOVICE.ordinal());
        setStat(gamer, "kills", 0);
        setStat(gamer, "deaths", 0);
        gamer.setProperty(BuildFFAGamer.PRESTIGE_PROPERTY_NAME, 0);
        gamer.setProperty(BuildFFAGamer.BEST_KILL_STREAK_PROPERTY_NAME, 0);
        gamer.setProperty(BuildFFAGamer.SELECTED_KIT_PROPERTY_NAME, 0);
        game.getChatMessageManager().resetForProgressionWipe(gamer);
        game.getKillEffectManager().resetForProgressionWipe(gamer);
        game.getKillSoundManager().resetForProgressionWipe(gamer);
        int coins = gamer.getCoins(Uncommon.BUILDFFA);
        if (coins > 0) gamer.takeCoins(Uncommon.BUILDFFA, coins);
        Player player = Bukkit.getPlayer(targetId);
        if (player == null || Gamer.getGamer(player) != gamer) return;
        BuildFFAGamer.get(gamer).resetKillStreak();
        game.getBountyManager().clear(player);
        game.resetPlayer(player);
        BuildFFABoard.refresh(gamer);
    }

    private CompletableFuture<Void> applySync(Gamer gamer, UUID targetId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            try {
                applyOnlineState(gamer, targetId);
                result.complete(null);
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });
        return result;
    }

    private void setStat(Gamer gamer, String stat, int value) {
        gamer.addStat(stat, value - gamer.getLifetimeStat(stat));
    }

    private UUID resolveUuid(Gamer gamer) {
        if (gamer.getPlayer() != null) return gamer.getPlayer().getUniqueId();
        for (String methodName : new String[]{"getUniqueId", "getUuid", "getUUID"}) {
            try {
                Method method = gamer.getClass().getMethod(methodName);
                Object value = method.invoke(gamer);
                if (value instanceof UUID) return (UUID) value;
                if (value instanceof String) return UUID.fromString((String) value);
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private static final class Confirmation {
        final UUID targetId;
        final UUID serverSession;
        final long expiresAt;

        Confirmation(UUID targetId, UUID serverSession, long expiresAt) {
            this.targetId = targetId;
            this.serverSession = serverSession;
            this.expiresAt = expiresAt;
        }
    }

    private static final class WipeStageException extends RuntimeException {
        final String stage;

        WipeStageException(String stage, Throwable cause) {
            super(cause);
            this.stage = stage;
        }
    }
}
