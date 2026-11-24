package net.fakepixel.housing.jukebox;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.util.Compat;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class JukeboxService {
    private final HousingPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> recordLocations = new HashMap<>();

    public JukeboxService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Song> songs() {
        List<Song> songs = new ArrayList<>();
        songs.add(new Song("13", "&a13", "GOLD_RECORD", "RECORD_13"));
        songs.add(new Song("cat", "&aCat", "GREEN_RECORD", "RECORD_CAT"));
        songs.add(new Song("blocks", "&aBlocks", "RECORD_3", "RECORD_BLOCKS"));
        songs.add(new Song("chirp", "&aChirp", "RECORD_4", "RECORD_CHIRP"));
        songs.add(new Song("far", "&aFar", "RECORD_5", "RECORD_FAR"));
        songs.add(new Song("mall", "&aMall", "RECORD_6", "RECORD_MALL"));
        songs.add(new Song("mellohi", "&aMellohi", "RECORD_7", "RECORD_MELLOHI"));
        songs.add(new Song("stal", "&aStal", "RECORD_8", "RECORD_STAL"));
        songs.add(new Song("strad", "&aStrad", "RECORD_9", "RECORD_STRAD"));
        songs.add(new Song("ward", "&aWard", "RECORD_10", "RECORD_WARD"));
        songs.add(new Song("11", "&a11", "RECORD_11", "RECORD_11"));
        songs.add(new Song("wait", "&aWait", "RECORD_12", "RECORD_WAIT"));
        return songs;
    }

    public Song song(String id) {
        if (id == null) return null;
        String clean = id.trim().toLowerCase(Locale.ENGLISH);
        for (Song song : songs()) if (song.id.equals(clean)) return song;
        return null;
    }

    public Song randomSong() {
        List<Song> songs = songs();
        return songs.get(random.nextInt(songs.size()));
    }

    public void playHouseMusic(Player player, House house) {
        if (player == null || house == null || !house.isJukeboxEnabled()) return;
        Song song = house.isJukeboxShuffle() ? randomSong() : song(house.getJukeboxSong());
        if (song == null) return;
        play(player, song, house.getJukeboxVolume());
    }

    public void playHouseMusic(House house) {
        if (house == null) return;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        for (Player player : world.getPlayers()) playHouseMusic(player, house);
    }

    public void stopHouseMusic(Player player) {
        if (player == null) return;
        stop(player);
    }

    public void stopHouseMusic(House house) {
        if (house == null) return;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(house.getWorldName());
        if (world == null) return;
        for (Player player : world.getPlayers()) stop(player);
    }

    public void restartHouseMusic(House house) {
        stopHouseMusic(house);
        playHouseMusic(house);
    }

    @SuppressWarnings("deprecation")
    private void play(Player player, Song song, int volume) {
        Material disc = song.material();
        Location location = player.getLocation();
        stop(player);
        try {
            player.playEffect(location, Effect.RECORD_PLAY, disc.getId());
            recordLocations.put(player.getUniqueId(), location.clone());
        } catch (Throwable ignored) {
            playSoundFallback(player, song, volume);
        }
    }

    @SuppressWarnings("deprecation")
    private void stop(Player player) {
        try {
            Location location = recordLocations.remove(player.getUniqueId());
            if (location == null) location = player.getLocation();
            player.playEffect(location, Effect.RECORD_PLAY, 0);
        } catch (Throwable ignored) {
        }
    }

    private void playSoundFallback(Player player, Song song, int volume) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(song.soundName), Math.max(1, volume) / 100.0F, 1.0F);
        } catch (Throwable ignored) {
            plugin.getLogger().fine("Could not play jukebox song " + song.id + " for " + player.getName());
        }
    }

    public static class Song {
        private final String id;
        private final String displayName;
        private final String materialName;
        private final String soundName;

        private Song(String id, String displayName, String materialName, String soundName) {
            this.id = id;
            this.displayName = displayName;
            this.materialName = materialName;
            this.soundName = soundName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Material material() { return Compat.mat(materialName); }
    }
}
