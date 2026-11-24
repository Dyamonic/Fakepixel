package club.abelix.api.nms.sound;

import club.abelix.api.nms.sound.type.YSound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SoundApi {

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playSound(Player player, SoundType type);

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playSound(Player player, SoundType type, float f, float f1);

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playSound(Player player, Location location, SoundType type);

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playWorldSound(Location location, SoundType type);

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playWorldSound(Location location, SoundType type, float f, float f1);

    /**
     * @deprecated - use method with YSound
     */
    @Deprecated
    void playSound(Player player, Location location, SoundType type, float f, float f1);

    // ---------------------------------------------------------------------------------

    void playSound(Player player, YSound sound);

    void playSound(Player player, YSound sound, float f, float f1);

    void playSound(Player player, Location location, YSound sound);

    void playWorldSound(Location location, YSound sound);

    void playWorldSound(Location location, YSound sound, float f, float f1);

    void playSound(Player player, Location location, YSound sound, float f, float f1);
}
