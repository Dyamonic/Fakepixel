package club.abelix.api.base.song.songplayer;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.song.NoteBlockManager;
import club.abelix.api.base.song.model.*;
import club.abelix.api.base.song.model.playmode.ChannelMode;
import club.abelix.api.base.song.model.playmode.MonoMode;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Plays a Song for a list of Players
 */
@ToString
@Data
public abstract class SongPlayer {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("SongPlayer-#%d")
                    .build());

    private final AbstractPlugin plugin;

    protected Song song;
    protected Playlist playlist;
    protected int actualSong = 0;

    protected boolean playing = false;
    protected short tick = -1;
    protected Set<Player> playerList = Sets.newConcurrentHashSet();

    protected boolean autoDestroy = false;
    protected boolean destroyed = false;

    protected byte volume = 100;
    protected Fade fadeIn;
    protected Fade fadeOut;
    protected RepeatMode repeat = RepeatMode.NO;
    protected boolean random;

    protected Map<Song, Boolean> songQueue = new ConcurrentHashMap<>(); //True if already played

    protected SoundCategory soundCategory;
    protected ChannelMode channelMode = new MonoMode();
    private ScheduledFuture<?> task;

    public SongPlayer(AbstractPlugin plugin, Song song) {
        this(plugin, new Playlist(song), SoundCategory.MASTER);
    }

    public SongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory) {
        this(plugin, new Playlist(song), soundCategory);
    }

    public SongPlayer(AbstractPlugin plugin, Song song, SoundCategory soundCategory, boolean random) {
        this(plugin, new Playlist(song), soundCategory, random);
    }

    public SongPlayer(AbstractPlugin plugin, Playlist playlist) {
        this(plugin, playlist, SoundCategory.MASTER);
    }

    public SongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory) {
        this(plugin, playlist, soundCategory, false);
    }

    public SongPlayer(AbstractPlugin plugin, Playlist playlist, SoundCategory soundCategory, boolean random) {
        this.plugin = plugin;

        this.playlist = playlist;
        this.random = random;
        this.soundCategory = soundCategory;

        fadeIn = new Fade(FadeType.NONE, 60);
        fadeIn.setFadeStart((byte) 0);
        fadeIn.setFadeTarget(volume);

        fadeOut = new Fade(FadeType.NONE, 60);
        fadeOut.setFadeStart(volume);
        fadeOut.setFadeTarget((byte) 0);

        if (random) {
            checkPlaylistQueue();

            actualSong = ThreadLocalRandom.current().nextInt(playlist.getCount());
        }

        setSong(playlist.get(actualSong));
    }

    public void setSong(Song song) {
        this.song = song;

        startTask();
    }

    private void stopTask() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    private void startTask() {
        stopTask();

        task = SCHEDULER.scheduleAtFixedRate(() -> {
            if (!playing) {
                return;
            }

            if (tick < fadeIn.getFadeDuration()) {
                val fade = fadeIn.calculateFade();

                if (fade != -1) {
                    volume = fade;
                }
            } else if (tick >= song.getLength() - fadeOut.getFadeDuration()) {
                val fade = fadeOut.calculateFade();

                if (fade != -1) {
                    volume = fade;
                }
            }

            tick++;

            if (tick > song.getLength()) {
                tick = -1;
                fadeIn.setFadeDone(0);
                fadeOut.setFadeDone(0);

                if (repeat == RepeatMode.ONE) {
                    return;
                } else {
                    if (random) {
                        songQueue.put(song, true);
                        checkPlaylistQueue();
                        ArrayList<Song> left = new ArrayList<>();
                        for (Song s : songQueue.keySet()) {
                            if (!songQueue.get(s)) {
                                left.add(s);
                            }
                        }

                        if (left.isEmpty()) {
                            left.addAll(songQueue.keySet());
                            songQueue.replaceAll((s, v) -> false);
                            setSong(left.get(ThreadLocalRandom.current().nextInt(left.size())));
                            actualSong = playlist.getIndex(song);
                            if (repeat == RepeatMode.ALL) {
                                return;
                            }
                        } else {
                            setSong(left.get(ThreadLocalRandom.current().nextInt(left.size())));
                            actualSong = playlist.getIndex(song);
                            return;
                        }
                    } else {
                        if (playlist.hasNext(actualSong)) {
                            actualSong++;
                            setSong(playlist.get(actualSong));
                            return;
                        } else {
                            actualSong = 0;
                            setSong(playlist.get(actualSong));
                            if (repeat == RepeatMode.ALL) {
                                return;
                            }
                        }
                    }
                }

                playing = false;

                if (autoDestroy) {
                    destroy();
                }
                return;
            }

            playerList.forEach(player -> playTick(player, tick));
        }, 0, (long) Math.ceil(song.getDelay() * 50), TimeUnit.MILLISECONDS);
    }

    private void checkPlaylistQueue() {
        for (val song : songQueue.keySet()) {
            if (!playlist.contains(song)) {
                songQueue.remove(song);
            }
        }

        for (val song : playlist.getSongList()) {
            if (!songQueue.containsKey(song)) {
                songQueue.put(song, false);
            }
        }
    }

    public synchronized void addPlayer(@NonNull Player player) {
        playerList.add(player);

        NoteBlockManager.IMP.startPlaying(player, this);
    }

    /**
     * Plays the Song for the specific player
     *
     * @param player to play this SongPlayer for
     * @param tick   to play at
     */
    public abstract void playTick(Player player, int tick);

    /**
     * SongPlayer will destroy itself
     */
    public synchronized void destroy() {
        stopTask();

        playerList.forEach(NoteBlockManager.IMP::stopPlaying);
        playerList.clear();

        destroyed = true;
        playing = false;
        setTick((short) -1);
    }

    public synchronized void removePlayer(Player player) {
        playerList.remove(player);

        NoteBlockManager.IMP.stopPlaying(player);

        if (playerList.isEmpty() && autoDestroy) {
            destroy();
        }
    }

    /**
     * Sets the current volume of this SongPlayer
     *
     * @param volume (0-100)
     */
    public void setVolume(byte volume) {
        if (volume > 100) {
            volume = 100;
        }

        this.volume = volume;

        fadeIn.setFadeTarget(volume);
        fadeOut.setFadeStart(volume);
    }

    public synchronized void playSong(int index) {
        if (playlist.exist(index)) {
            setSong(playlist.get(index));
            actualSong = index;
            tick = -1;
            fadeIn.setFadeDone(0);
            fadeOut.setFadeDone(0);
        }
    }

    /**
     * Start playing {@link Song} that is next in {@link Playlist} or random {@link Song} from {@link Playlist}
     */
    public synchronized void playNextSong() {
        tick = song.getLength();
    }

    /**
     * Gets whether the SongPlayer will loop
     *
     * @return is loop
     * @deprecated
     */
    @Deprecated
    public boolean isLoop() {
        return repeat == RepeatMode.ALL;
    }

    /**
     * Sets whether the SongPlayer will loop
     *
     * @param loop
     * @deprecated
     */
    @Deprecated
    public void setLoop(boolean loop) {
        this.repeat = RepeatMode.ALL;
    }

}
