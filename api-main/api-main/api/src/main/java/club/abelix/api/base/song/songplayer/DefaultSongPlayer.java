package club.abelix.api.base.song.songplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.base.song.model.*;
import club.abelix.api.base.song.utils.NoteUtils;
import org.bukkit.entity.Player;

public class DefaultSongPlayer extends SongPlayer {

    public DefaultSongPlayer(AbstractPlugin plugin, Song song) {
        super(plugin, song);
    }

    public DefaultSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory) {
        super(plugin, song, soundCategory);
    }

    public DefaultSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory, boolean random) {
        super(plugin, song, soundCategory, random);
    }

    public DefaultSongPlayer(AbstractPlugin plugin, Playlist playlist) {
        super(plugin, playlist);
    }

    public DefaultSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory) {
        super(plugin, playlist, soundCategory);
    }

    public DefaultSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory, boolean random) {
        super(plugin, playlist, soundCategory, random);
    }

    @Override
    public void playTick(Player player, int tick) {
        byte playerVolume = NoteBlockManager.IMP.getPlayerVolume(player);

        for (Layer layer : song.getLayerHashMap().values()) {
            Note note = layer.getNote(tick);

            if (note == null) continue;

            float volume = ((layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F);
            float pitch = NoteUtils.getPitch(note);

            channelMode.play(player, player.getLocation(), song, layer, note, soundCategory, volume, pitch);
        }
    }
}
