package club.abelix.api.base.song.songplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.song.model.Playlist;
import club.abelix.api.base.song.model.Song;
import club.abelix.api.base.song.model.SoundCategory;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * SongPlayer playing only in specified distance
 */

@Getter
public abstract class RangeSongPlayer extends SongPlayer {

    private int distance = 16;

    public RangeSongPlayer(AbstractPlugin plugin, Song song) {
        super(plugin, song);
    }

    public RangeSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory) {
        super(plugin, song, soundCategory);
    }

    public RangeSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory, boolean random) {
        super(plugin, song, soundCategory, random);
    }

    public RangeSongPlayer(AbstractPlugin plugin, Playlist playlist) {
        super(plugin, playlist);
    }

    public RangeSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory) {
        super(plugin, playlist, soundCategory);
    }

    public RangeSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory, boolean random) {
        super(plugin, playlist, soundCategory, random);
    }

    /**
     * Returns true if the Player is able to hear the current RangeSongPlayer
     *
     * @param player in range
     * @return ability to hear the current RangeSongPlayer
     */
    public abstract boolean isInRange(Player player);

}
