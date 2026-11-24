package club.abelix.games.fakevival.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static Sound parse(String value) {
        if (value == null || value.isBlank()) return null;
        String raw = value.trim();

        NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        if (key == null && !raw.contains(":")) key = NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) return sound;
        }

        String legacy = raw.toUpperCase(Locale.ROOT).replace(':', '_').replace('.', '_').replace('-', '_');
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey soundKey = Registry.SOUNDS.getKey(sound);
            if (soundKey == null) continue;
            String enumStyle = soundKey.getKey().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
            if (legacy.equals(enumStyle)) return sound;
        }
        return null;
    }
}
