package club.abelix.api.base.song.songplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.base.song.model.Playlist;
import club.abelix.api.base.song.model.Song;
import club.abelix.api.base.song.model.SoundCategory;
import club.abelix.api.base.song.utils.NoteUtils;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * SongPlayer created at a specified Location
 */
public class PositionSongPlayer extends RangeSongPlayer {

    private Location targetLocation;

    public PositionSongPlayer(AbstractPlugin plugin, Song song) {
        super(plugin, song);
    }

    public PositionSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory) {
        super(plugin, song, soundCategory);
    }

    public PositionSongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory, boolean random) {
        super(plugin, song, soundCategory, random);
    }

    public PositionSongPlayer(AbstractPlugin plugin, Playlist playlist) {
        super(plugin, playlist);
    }

    public PositionSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory) {
        super(plugin, playlist, soundCategory);
    }

    public PositionSongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory, boolean random) {
        super(plugin, playlist, soundCategory, random);
    }

    /**
     * Gets location on which is the PositionSongPlayer playing
     *
     * @return {@link Location}
     */
    public Location getTargetLocation() {
        return targetLocation;
    }

    /**
     * Sets location on which is the PositionSongPlayer playing
     */
    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    @Override
    public void playTick(Player player, int tick) {
        if (player.getWorld() != targetLocation.getWorld()) {
            return;
        }

        if (!isInRange(player)) {
            return;
        }

        val playerVolume = NoteBlockManager.IMP.getPlayerVolume(player);

        for (val layer : song.getLayerHashMap().values()) {
            val note = layer.getNote(tick);

            if (note == null) {
                continue;
            }

            val volume = ((layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F)
                    * ((1F / 16F) * getDistance());

            val pitch = NoteUtils.getPitch(note);

            channelMode.play(player, targetLocation, song, layer, note, soundCategory, volume, pitch);
        }
    }

    /**
     * Returns true if the Player is able to hear the current PositionSongPlayer
     *
     * @param player in range
     * @return ability to hear the current PositionSongPlayer
     */
    @Override
    public boolean isInRange(Player player) {
        return player.getLocation().distance(targetLocation) <= getDistance();
    }
}
