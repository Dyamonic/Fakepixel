package club.abelix.api.nms.sound;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.version.GameVersion;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.EnumMap;

public class SoundApiImpl implements SoundApi {

    private final NmsApi nmsApi;

    private final EnumMap<YSound, Sound> soundCache = new EnumMap<>(YSound.class);

    public SoundApiImpl(@NonNull NmsApi nmsApi) {
        this.nmsApi = nmsApi;

        initSoundCache();
    }

    private void initSoundCache() {
        int protocolVersion = nmsApi.getProtocolNumber();

        for (YSound verIndependentSound : YSound.VALUES) {
            String sound, fallbackSound;

            if (protocolVersion == GameVersion.MINECRAFT_1_8.getVersion()) {
                sound = verIndependentSound.getSound_1_8();
                fallbackSound = YSound.IRONGOLEM_DEATH.getSound_1_8();
            } else if (protocolVersion < GameVersion.MINECRAFT_1_13.getVersion()) {
                sound = verIndependentSound.getSound_1_9_to_1_12();
                fallbackSound = YSound.IRONGOLEM_DEATH.getSound_1_9_to_1_12();
            } else {
                sound = verIndependentSound.getSound_1_13();
                fallbackSound = YSound.IRONGOLEM_DEATH.getSound_1_13();
            }

            Sound bukkitSound = null;

            try {
                bukkitSound = Sound.valueOf(sound == null ? fallbackSound : sound);
            } catch (IllegalArgumentException exception) {
                Logging.IMP.warn("Can not cache sound for {} (fallback {})", sound, fallbackSound);
            }

            soundCache.put(verIndependentSound, bukkitSound);
        }

        Logging.IMP.info("[SoundApi] Cached {} sounds", soundCache.size());
    }

    @Deprecated
    @Override
    public void playSound(Player player, SoundType type) {
        playSound(player, player.getLocation(), type, 1F, 1F);
    }

    @Deprecated
    @Override
    public void playSound(Player player, SoundType type, float f, float f1) {
        playSound(player, player.getLocation(), type, f, f1);
    }

    @Deprecated
    @Override
    public void playSound(Player player, Location location, SoundType type) {
        playSound(player, location, type, 1F, 1F);
    }

    @Deprecated
    @Override
    public void playWorldSound(Location location, SoundType type) {
        playWorldSound(location, type, 1F, 1F);
    }

    @Deprecated
    @Override
    public void playWorldSound(Location location, SoundType type, float f, float f1) {
        location.getWorld().playSound(location, soundCache.get(type.toYSound()), f, f1);
    }

    @Deprecated
    @Override
    public void playSound(Player player, Location location, SoundType type, float f, float f1) {
        player.playSound(location, soundCache.get(type.toYSound()), f, f1);
    }

    @Override
    public void playSound(Player player, YSound sound) {
        playSound(player, player.getLocation(), sound, 1F, 1F);
    }

    @Override
    public void playSound(Player player, YSound sound, float f, float f1) {
        playSound(player, player.getLocation(), sound, f, f1);
    }

    @Override
    public void playSound(Player player, Location location, YSound sound) {
        playSound(player, location, sound, 1F, 1F);
    }

    @Override
    public void playWorldSound(Location location, YSound sound) {
        playWorldSound(location, sound, 1F, 1F);
    }

    @Override
    public void playWorldSound(Location location, YSound sound, float f, float f1) {
        location.getWorld().playSound(location, soundCache.get(sound), f, f1);
    }

    @Override
    public void playSound(Player player, Location location, YSound sound, float f, float f1) {
        player.playSound(location, soundCache.get(sound), f, f1);
    }
}
