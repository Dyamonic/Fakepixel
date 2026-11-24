package club.abelix.api.nms;

import club.abelix.api.nms.bossbar.FakeBossbarApi;
import club.abelix.api.nms.command.CommandApi;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.sound.SoundApi;
import club.abelix.api.nms.team.FakeTeamApi;
import club.abelix.api.nms.world.WorldApi;
import club.abelix.commons.version.GameVersion;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import org.bukkit.plugin.Plugin;

import java.util.function.UnaryOperator;

public interface NmsApi {

    /**
     * Получить плагин от имени которого создается вся эта дичь
     *
     * @return - плагин bukkit
     */
    Plugin getPlugin();

    /**
     * Получить номер протокола используемый на сервере
     *
     * @return - номер протокола
     */
    int getProtocolNumber();

    default boolean isLegacy() {
        return getProtocolNumber() < GameVersion.MINECRAFT_1_16.getVersion();
    }

    /**
     * Получить апи для работы с командами
     */
    CommandApi getCommandApi();

    /**
     * Получить апи для работы с пакетами
     */
    PacketLibraryApi getPacketLibraryApi();

    /**
     * Получить апи для работы с фейковыми энтити
     */
    FakeEntityApi getFakeEntityApi();

    /**
     * Получить апи для работы с фейковыми тимами скорборда
     */
    FakeTeamApi getFakeTeamApi();

    /**
     * Получить апи для работы с игроками
     */
    PlayerApi getPlayerApi();

    /**
     * Получить апи для работы с звуками
     */
    SoundApi getSoundApi();

    /**
     * Получить апи для работы с босс барами
     */
    FakeBossbarApi getBossbarApi();

    /**
     * Получить апи для работу с миром
     */
    WorldApi getWorldApi();

    /**
     * Получить апи для работы с NBT тегами
     */
    NbtApi getNbtApi();

    /**
     * Получить тпс
     */
    double[] getTPS();

    void load();

    void enable();

    default <T> void redefine(Class<T> type,
                              ClassLoader systemClassLoader,
                              UnaryOperator<DynamicType.Builder<T>> operation) {

        DynamicType.Builder<T> builder = new ByteBuddy().redefine(type);

        operation
                .apply(builder)
                .make()
                .load(systemClassLoader, ClassReloadingStrategy.fromInstalledAgent());
    }

}
