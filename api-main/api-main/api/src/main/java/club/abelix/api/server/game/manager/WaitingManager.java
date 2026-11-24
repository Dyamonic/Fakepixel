package club.abelix.api.server.game.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.chat.MessageSender;
import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.board.WaitingBoard;
import club.abelix.api.server.game.board.WaitingBoardBase;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.logging.Logging;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

@RequiredArgsConstructor
public class WaitingManager implements Runnable {

    private final Game game;

    /**
     * Таск старта игры
     */
    private BukkitTask task;

    @Setter
    private int startTime = 35;

    public void startTimer() {
        if (task != null) {
            return;
        }

        task = HeartBeat.runTimerSync(this, 0, 20);
    }

    public void cancelTimer() {
        if (task == null) {
            return;
        }

        task.cancel();

        task = null;
        startTime = 35;

        for (Gamer gamer : game.getGamers()) {
            Player player = gamer.getPlayer();

            player.setExp(0);
            player.setLevel(0);
        }
    }

    public void start() {
        int startDelay = game.<Integer>getSetting(GameSetting.START_DELAY, 5);

        if (startTime < startDelay) {
            return;
        }

        startTime = startDelay;

        if (task == null) {
            startTimer();
        }
    }

    public int getTime() {
        return startTime;
    }

    @Override
    public void run() {
        int startDelay = game.<Integer>getSetting(GameSetting.START_DELAY, 5);

        if (startTime <= startDelay && game.getGameState() != GameState.STARTING) {
            game.setGameState(GameState.STARTING);
        }

        if (startTime <= 0) {
            try {
                game.startGame();
            } catch (Exception e) {
                Logging.IMP.error("[WaitingManager] Game {} failed start! Details: ", game.getName());
                e.printStackTrace();

                game.broadcast("Внимание! Сделайте скриншот сообщений ниже и отправьте его администраторам!");
                game.broadcast("Произошла ошибка в §c" + e.getClass().getSimpleName() + " §fпри запуске: §c" + e.getMessage());

                StringBuilder result = new StringBuilder();

                for (StackTraceElement element : e.getStackTrace()) {
                    String error = element.toString();

                    if (error.contains("abelix")) {
                        result.append("\n§7 at  §f").append(element);
                    }
                }

                game.setGameState(GameState.ENDING);
                game.broadcast(result.toString());

                game.registerEndListeners();
                game.applyEndTasks(5);

                GameCrashManager.INSTANCE.onCrash(game);
            } finally {
                cancelTimer();
            }
            return;
        }

        // todo: подумать как это оптимизировать
        Board.forEach(
                game.<Class<? extends WaitingBoardBase>>getSetting(GameSetting.WAITING_BOARD_CLASS, WaitingBoard.class),
                board -> {
                    for (Player player : board.getReceivers()) {
                        if (game.getGamers().contains(Gamer.getGamer(player))) {
                            board.updateStartTime(startTime);
                            break;
                        }
                    }
                }
        );

        String timeColor = startTime == 10 ? "§6" : startTime <= 5 && startTime >= 4 ? "§e" : startTime <= 3 ? "§c" : "";
        boolean shouldNotifyGamers = startTime == 10 || startTime == 5 || (startTime <= 3 && startTime >= 1);
        for (Gamer gamer : game.getGamers()) {
            Player player = gamer.getPlayer();

            if (player == null) {
                continue;
            }

            if (startTime <= 5 && startTime >= 1 || startTime == 10) {
                MessageSender.sendTitle(player, timeColor+startTime, "", 0, 25, 0);
            }

            player.setExp((float) startTime / 60);
            player.setLevel(startTime);

            if (shouldNotifyGamers) {
                gamer.playSound(YSound.NOTE_PLING);
            }
        }
        if (shouldNotifyGamers) {
            game.broadcastLocalized(
                    startTime == 1 ? "game_start_timer_message_second" : "game_start_timer_message",
                    "%time%", timeColor + startTime
            );

        }

        startTime--;
    }

    public boolean isStarted() {
        return this.task != null;
    }

}
