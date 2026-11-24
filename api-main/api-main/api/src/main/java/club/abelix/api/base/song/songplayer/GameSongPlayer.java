package club.abelix.api.base.song.songplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.base.song.model.*;
import club.abelix.api.base.song.utils.NoteUtils;
import club.abelix.api.server.game.Game;
import club.abelix.network.common.enums.GameState;
import org.bukkit.entity.Player;

/**
 * @author WhileInside
 */
public class GameSongPlayer extends SongPlayer {

    private Game game;

    public GameSongPlayer(AbstractPlugin plugin, Song song) {
        super(plugin, song);
    }

    public GameSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory) {
        super(plugin, song, soundCategory);
    }

    public GameSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory, boolean random) {
        super(plugin, song, soundCategory, random);
    }

    public GameSongPlayer(AbstractPlugin plugin, Playlist playlist) {
        super(plugin, playlist);
    }

    public GameSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory) {
        super(plugin, playlist, soundCategory);
    }

    public GameSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory, boolean random) {
        super(plugin, playlist, soundCategory, random);
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;

        for (Gamer gamer : game.getAlivePlayers()) {
            addPlayer(gamer.getPlayer());
        }
    }

    @Override
    public void playTick(Player player, int tick) {
        if (game.getGameState() != GameState.GAME && game.getGameState() != GameState.STARTING) {
            if (autoDestroy) {
                destroy();
            } else {
                setPlaying(false);
            }
        }

        Gamer gamer = Gamer.getGamer(player);

        if (gamer.isSpectator() || gamer.getGame() != game) {
            return;
        }

        byte playerVolume = NoteBlockManager.IMP.getPlayerVolume(player);

        for (Layer layer : song.getLayerHashMap().values()) {
            Note note = layer.getNote(tick);

            if (note == null) {
                continue;
            }

            float volume = ((layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F);
            float pitch = NoteUtils.getPitch(note);

            channelMode.play(player, player.getLocation(), song, layer, note, soundCategory, volume, pitch);
        }
    }
}
