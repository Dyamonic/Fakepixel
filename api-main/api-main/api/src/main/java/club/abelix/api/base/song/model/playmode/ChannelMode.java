package club.abelix.api.base.song.model.playmode;

import club.abelix.api.base.song.model.Layer;
import club.abelix.api.base.song.model.Note;
import club.abelix.api.base.song.model.Song;
import club.abelix.api.base.song.model.SoundCategory;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Decides how is {@link Note} played to {@link Player}
 */
public abstract class ChannelMode {

    public abstract void play(Player player, Location location, Song song, Layer layer, Note note,
                              SoundCategory soundCategory, float volume, float pitch);
}