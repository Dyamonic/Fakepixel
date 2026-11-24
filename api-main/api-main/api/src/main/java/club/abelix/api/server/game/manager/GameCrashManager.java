package club.abelix.api.server.game.manager;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.server.game.Game;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>GameCrashManager creation on 22.10.2023 at 15:31</p>
 */

@Getter
public class GameCrashManager {

    // ===========================================================
    public static final GameCrashManager INSTANCE = new GameCrashManager();
    // ===========================================================

    private boolean crash = false;
    private Game initialGame ;


    private void stopServer() {
        Logging.IMP.info("[CrashManager] Stop server...");
        // todo: Добавить пакет на кор, о том, что сервер крашнулся, и выводить это в канале дс или чёт такое
        Bukkit.shutdown();
    }


    public void onCrash(@NonNull Game game) {
        if (crash) return;

        this.crash = true;
        this.initialGame = game;

        Logging.IMP.info("[CrashManager] Start crash task...");

        new AbstractTask(FalconAPI.getInstance()) {
            @Override
            protected void run() {
                // Если на сервере никого нету, то можем спокойно офнуть
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    stopServer();
                    return;
                }

                // Если ещё есть игры которые ждут, отключаем им очередь
                for (Game game : GameManager.INSTANCE.getRegisteredGames()) {
                    val gameWorld = game.getGameWorld();
                    if (game.getGameState() == GameState.STAND_BY
                            && gameWorld != null && gameWorld.getPlayers().isEmpty()) {
                        game.getGameQueueManager().deactivate();
                        Logging.IMP.info("[CrashManager] Deactivate {} queue!", game.getName());
                    }
                }
            }
        }.timer(20L, 20L);
    }
}
