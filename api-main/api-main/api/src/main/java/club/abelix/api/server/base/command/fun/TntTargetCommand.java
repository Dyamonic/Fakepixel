package club.abelix.api.server.base.command.fun;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import lombok.val;
import org.bukkit.entity.TNTPrimed;

import java.util.concurrent.ThreadLocalRandom;

@NamedCommand("tnttarget")
public class TntTargetCommand {

    private final static String TNT_TASK_PROPERTY = "Fun/TNT";

    @Executor
    @WithPermission("command.tnt.target")
    @WithCondition("disableConsole")
    public static void executor(BukkitExecutionSession session) throws Exception {
        val gamer = session.getGamer();
        val game = gamer.getGame();
        val minTime = session.getIntArgument(0, "minTime");
        val maxTime = session.getIntArgument(1, "maxTime");

        if (game == null) {
            gamer.sendMessage("Only game command!");
            return;
        }

        // Проверка корректности диапазона времени
        if (minTime >= maxTime) {
            gamer.sendMessage("Invalid time range: minTime must be less than maxTime.");
            return;
        }

        val gameMetadata = game.getMetadata();

        for (val targetGamer : game.getGamers()) {

            if (gameMetadata.has(targetGamer.getPlayer(), TNT_TASK_PROPERTY)) {
                AbstractTask targetTask = gameMetadata.get(targetGamer.getPlayer(), TNT_TASK_PROPERTY);
                targetTask.cancel();
            }

            if (targetGamer.isSpectator()) continue;

            val targetTask = new AbstractTask(game) {

                private int spawnSecond = -1;

                private void spawnTnt() {
                    val player = targetGamer.getPlayer();
                    TNTPrimed tnt = player.getWorld().spawn(player.getLocation().clone().add(0, 0.5, 0), TNTPrimed.class);
                    FalconAPI.getNmsApi().getWorldApi().setTntSource(tnt, player);
                    gameMetadata.set(tnt, "owner", player);
                    tnt.setFuseTicks(45);
                    tnt.setYield(7);
                    tnt.setIsIncendiary(false);
                }

                @Override
                protected void run() {
                    if (spawnSecond < 0) {
                        spawnSecond = ThreadLocalRandom.current().nextInt(minTime, maxTime);
                    }

                    if (!targetGamer.getPlayer().isOnline()) handleCancel(CancelReason.END);
                    // Не заканчиваем тк это может быть возрождение
                    if (targetGamer.isSpectator()) return;

                    if (spawnSecond == 0) {
                        spawnTnt();
                    }

                    spawnSecond--;
                }
            }.timer(20, 20);

            gameMetadata.set(targetGamer.getPlayer(), TNT_TASK_PROPERTY, targetTask);
        }
    }
}
