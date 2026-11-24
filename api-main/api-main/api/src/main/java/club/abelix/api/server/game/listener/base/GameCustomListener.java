package club.abelix.api.server.game.listener.base;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.event.UpdatePublicStatusEvent;
import club.abelix.api.nms.packet.IVisibleObject;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.rule.FakeTeamFlag;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.listener.GameListener;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class GameCustomListener extends GameListener<Game> {
    public GameCustomListener(Game game) {
        super(game);
    }

    @EventHandler
    public void onPublicStatusUpdate(UpdatePublicStatusEvent event) {
        if (!event.isPublicStatus()) return;

        IVisibleObject object = event.getObject();

        boolean add = false;

        if (object instanceof FakeBossbar) {
            FakeBossbar bossbar = (FakeBossbar) event.getObject();

            if (bossbar == game.getGamePrimaryBossbar()) {
                add = true;
            }
        } else if (object instanceof FakeEntity) {
            FakeEntity entity = (FakeEntity) event.getObject();

            if (entity.getLocation().getWorld() == game.getGameWorld()) {
                add = true;
            }
        }

        if (add) {
            for (Gamer gamer : game.getGamers()) {
                object.addReceiver(gamer.getPlayer());
            }
        }
    }

    @EventHandler
    public void onGameJoin(GameJoinEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();
            Player player = gamer.getPlayer();

            FakeBossbar bossbar = game.getGamePrimaryBossbar();

            if (bossbar != null) {
                bossbar.addReceiver(player);
            }

            for (FakeTeam team : game.getFakeTeams()) {
                if (team.isPublicStatus()) {
                    team.addReceiver(player);
                }

                if (team.hasFlag(FakeTeamFlag.ADD_PLAYER_ON_JOIN)) {
                    team.addPlayer(gamer.getName());
                }
            }

            for (FakeEntity entity : game.getFakeEntities()) {
                if (entity.isPublicStatus()) {
                    sendWithLatency(player, entity);
                }
            }
        });
    }

    @EventHandler
    public void onGameQuit(GameQuitEvent event) {
        checkGame(event, () -> {
            Player player = event.getGamer().getPlayer();

            FakeBossbar bossbar = game.getGamePrimaryBossbar();

            if (bossbar != null) {
                bossbar.removeReceiver(player);
            }

            for (FakeTeam team : game.getFakeTeams()) {
                team.removeReceiver(player);

                if (team.hasFlag(FakeTeamFlag.REMOVE_PLAYER_ON_QUIT)) {
                    team.removePlayer(player.getName());
                }
            }

            for (FakeEntity entity : game.getFakeEntities()) {
                entity.removeReceiver(player);
            }
        });
    }

    private void sendWithLatency(@NonNull Player player, @NonNull FakeEntity fakeEntity) {
        /*HeartBeat.runLaterSync(plugin, () -> */
        fakeEntity.addReceiver(player)/*, 10)*/;
    }

}
