package club.abelix.api.server.game.manager;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.state.GamerState;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.board.SpectatorBoard;
import club.abelix.api.server.game.event.GameSpectateToggle;
import club.abelix.api.server.game.hotbar.GameHotbar;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SpectatorManager {

    private final Game game;
    private final FakeTeam spectatorTeam;

    public SpectatorManager(@NonNull Game game) {
        this.game = game;

        this.spectatorTeam = game.createTeam(api -> api.createTeam("0ASpectator", "§7§o", ""));
        this.spectatorTeam.setSeeInvisible(true);
    }

    public static boolean isSpectator(@NonNull Entity entity) {
        Gamer gamer = Gamer.getGamer(entity.getName());

        return gamer != null && gamer.isSpectator();
    }

    public void reset() {
        spectatorTeam.remove();
    }

    public void setSpectator(@NonNull Gamer gamer, boolean spectator) {
        if (spectator) {
            addSpectator(gamer);
        } else {
            removeSpectator(gamer);
        }
    }

    @Deprecated
    public void setSpectator(@NonNull Player player, boolean spectator) {
        setSpectator(Gamer.getGamer(player), spectator);
    }

    @Deprecated
    public void addSpectator(@NonNull Player player) {
        addSpectator(player, false);
    }

    public void addSpectator(@NonNull Gamer gamer) {
        addSpectator(gamer, false);
    }

    @Deprecated
    public void addSpectator(@NonNull Player player, boolean temporary) {
        addSpectator(Gamer.getGamer(player), temporary);
    }

    public void addSpectator(@NonNull Gamer gamer, boolean temporary) {
        Player player = gamer.getPlayer();

        if (gamer.isSpectator()) {
            return;
        }

        //скрываем или показываем другим игрокам
        for (Gamer anotherGamer : game.getGamers()) {
            Player other = anotherGamer.getPlayer();

            if (anotherGamer == gamer) {
                continue;
            }

            if (!temporary && anotherGamer.isSpectator()) {
                if (!anotherGamer.isVanished()) {
                    player.showPlayer(other);
                }

                other.showPlayer(player);
            } else {
                other.hidePlayer(player);
            }
        }

        player.setExp(0);
        player.setLevel(0);
//        player.setMaxHealth(20);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setVelocity(new Vector(0, 0, 0));

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();

        player.setAllowFlight(true);
        HeartBeat.runSync(() -> player.setFlying(true));

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        player.spigot().setCollidesWithEntities(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

        FalconAPI.getNmsApi().getPlayerApi().removeArrows(player);

        if (!temporary) {
            if (game.getSetting(GameSetting.REMOVE_SPECTATOR_FROM_GAME, true)) {
                //делаем игроку тег спектатора
                game.getFakeTeams().forEach(team -> team.removePlayer(player.getName()));

                spectatorTeam.addPlayer(gamer.getName());
                spectatorTeam.addReceiver(player);

                if (game.getSetting(GameSetting.AUTO_SHOW_SPECTATOR_BOARD, true)) {
                    //показываем борд спектатора игроку
                    new SpectatorBoard(game, gamer);
                }

            }

            gamer.setGamerState(GamerState.SPECTATOR);

            HeartBeat.runSync(() -> FalconAPI.getInstance().getHotbarService().apply(GameHotbar.SPECTATOR_HOTBAR_ID, gamer));
        }

        //вызываем эвент
        HeartBeat.callEvent(new GameSpectateToggle(game, player, true));
    }

    @Deprecated
    public void removeSpectator(@NonNull Player player) {
        removeSpectator(Gamer.getGamer(player));
    }

    public void removeSpectator(@NonNull Gamer gamer) {
        if (!gamer.isSpectator()) {
            return;
        }

        Player player = gamer.getPlayer();

        //скрываем или показываем другим игрокам
        for (Gamer another : game.getGamers()) {
            if (another == gamer) {
                continue;
            }

            Player other = another.getPlayer();
            other.showPlayer(player);

            if (another.isSpectator()) {
                player.hidePlayer(other);
            } else {
                player.showPlayer(other);
            }
        }

        FalconAPI.getInstance().getHotbarService().clear(gamer);

        //очищаем игрока
        player.setFallDistance(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.getInventory().setHeldItemSlot(0);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.spigot().setCollidesWithEntities(true);
        player.setGameMode(GameMode.SURVIVAL);

        //убираем тег спектатора у игрока
        spectatorTeam.removeReceiver(player);
        spectatorTeam.removePlayer(player.getName());

        //вызываем эвент
        HeartBeat.callEvent(new GameSpectateToggle(game, player, false));
    }
}
