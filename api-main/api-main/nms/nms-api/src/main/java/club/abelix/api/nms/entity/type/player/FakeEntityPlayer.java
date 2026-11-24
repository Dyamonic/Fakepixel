package club.abelix.api.nms.entity.type.player;

import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.tablist.FakeEntityTablistEntry;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.commons.data.skin.Skin;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public interface FakeEntityPlayer extends FakeLivingEntity {

    /**
     * Дисплей нейм игрока
     */
    String getName();

    /**
     * Скин
     */
    Skin getSkin();

    /**
     * UUID
     */
    UUID getUUID();

    /**
     * Установить новую тиму для NPC
     */
    void setTeam(@NonNull FakeTeam team);

    /**
     * Цвет подсветки
     */
    ChatColor getGlowingColor();

    /**
     * Установить цвет подсветки
     */
    void setGlowingColor(ChatColor chatColor);

    /**
     * Установить/убрать видимость определённой части тела у игрока, по умолчанию всё включено
     */
    void setSkinPartStatus(@NonNull SkinPart part, boolean status);

    /**
     * Отключить удаление NPC из таба после отправки
     */
    void disableRemovingFromTab();

    /**
     * Сгенерировать рандомное имя для NPC
     */
    static String generateRandomName() {
        return "§8[NPC] " + ThreadLocalRandom.current().nextInt(9999);
    }

    void hideFromTab(final @NonNull Collection<? extends Player> players);

    void addTablistRemoveTask(final @NonNull Collection<? extends Player> players, long delay);

    void removeTablistEntry(final @NonNull FakeEntityTablistEntry entry);

    Collection<FakeEntityTablistEntry> getTablistEntries();

}
