package club.abelix.api.base.song;

import club.abelix.api.base.song.songplayer.SongPlayer;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.util.collection.CollectionUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectByteMap;
import gnu.trove.map.hash.TObjectByteHashMap;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;


public class NoteBlockManager {

    // ==============================================================
    @Dependency
    public static final NoteBlockManager IMP = new NoteBlockManager();
    // ==============================================================

    @Getter
    private final ConcurrentMap<Player, SongPlayer> playingSongs = Maps.newConcurrentMap();

    private final TObjectByteMap<Player> playerVolume = TCollections.synchronizedMap(
            new TObjectByteHashMap<>(10, 0.5F, (byte) 100)
    );

    public boolean isReceivingSong(Player player) {
        return playingSongs.containsKey(player);
    }

    public void stopPlaying(Player player) {
        playingSongs.remove(player);
    }

    public void startPlaying(Player player, SongPlayer songPlayer) {
        playingSongs.put(player, songPlayer);
    }

    public void setPlayerVolume(Player player, byte volume) {
        playerVolume.put(player, volume);
    }

    public byte getPlayerVolume(Player player) {
        return playerVolume.get(player);
    }

}
