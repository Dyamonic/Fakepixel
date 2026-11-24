package club.abelix.commons.data.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.stream.Stream;

@Getter
@ToString
@AllArgsConstructor
public enum Group {

    DEFAULT(1, 0, "§7Default", "§7", "§7"),

    /**
     * Donate
     */
    IRON(10, 10, "§aIRON", "§a§lIRON §a", "§f"),
    GOLD(15, 15, "§6GOLD", "§6§lGOLD §6", "§f"),
    DELUXE(20, 20, "§bDELUXE", "§b§lDELUXE §b", "§f"),
    MASTER(25, 25, "§cMASTER", "§c§lMASTER §c", "§e"),
    RUBIUM(28, 28, "§4RUBIUM", "§4§lRUBIUM §4", "§e"),
    ULTRA(30, 30, "§dULTRA", "§d§lULTRA §d", "§e"),
    SPONSOR(40, 40, "§2SPECIAL", "§2§lSPECIAL §2§l", "§6"),

    /**
     * Media
     */
    YOUTUBE(50, 45, "§c§lYOU§f§lTUBE", "§c§lYOU§f§lTUBE §f", "§6"),

    /**
     * Beta testers
     */
    BETA(60, 60, "§7BETA TESTER", "§7§lBETA ", "§6"),

    /**
     * Builders
     */
    BUILD(80, 79, "§3BUILD TEAM", "§3§lBUILD ", "§6"),

    /**
     * Staff
     */
    HELPER(95, 75, "§7§lHELPER", "§7§lHELPER ", "§6"),
    MODERATOR(100, 80, "§7§lMODER", "§7§lMODER ", "§6"),
    SR_MODER(103, 85, "§9§lSR. MODER", "§9§lSR. MODER ", "§6"),
    HEAD_MODERATOR(104, 90, "§9§lH. MODER", "§9§lH. MODER ", "§6"),

    /**
     * High Staff
     */
    AX_TEAM(105, 110, "§5§lTEAM", "§5§lTEAM ", "§6"),
    ADMINISTRATOR(110, 100, "§4§lADMIN", "§4§lADMIN ", "§6"),
    DEVELOPER(150, 150, "§3§lDEV", "§3§lDEV ", "§6"),
    OWNER(200, 300, "§4OWNER", "§4§lOWNER ", "§6"),
    HIDE_OWNER(210, 300, "§4HIDE OWNER", "§7", "§7");

    public static final Group[] VALUES = Group.values();
    private final int id, level;
    private final String name, prefix, suffix, prefixColor;

    Group(int id, int level, String name, String prefix, String suffix) {
        this(id, level, name, prefix, suffix, prefix.substring(0, 2));
    }

    /**
     * Получить группу по ее ID. Если такой группы нет,
     * вернется группа по умолчанию (DEFAULT)
     *
     * @param groupIdentifier - id группы
     * @return - группа по этому идентификатору
     */
    public static Group getByIdentifier(int groupIdentifier) {
        return Arrays.stream(values())
                .filter(group -> group.getId() == groupIdentifier)
                .findFirst()
                .orElse(DEFAULT);
    }

    public static Group getById(int id) {
        return _getById(id).findAny().orElse(DEFAULT);
    }

    /**
     * Получить группу по ее ID. Если такой группы нет,
     * вернется группа по умолчанию (DEFAULT)
     *
     * @param id - id группы
     * @return - группа по этому идентификатору
     */
    private static Stream<Group> _getById(int id) {
        return Arrays.stream(VALUES).filter(group -> group.getId() == id);
    }

    /**
     * Получить группу по ее названию. Если такой группы нет,
     * вернется группа по умолчанию (DEFAULT)
     *
     * @param legacyName - название группы со старого сервера
     * @return - группа по этому имени
     */
    public static Group fromLegacyGroup(@NonNull String legacyName) {
        return Arrays.stream(VALUES)
                .filter(group -> group.name().equalsIgnoreCase(legacyName))
                .findFirst().orElse(DEFAULT);
    }

    /**
     * Получить отображаемое имя группы
     *
     * @return - отображаемое имя группы
     */
    public String getDisplayName() {
        return isAprilFools() ? "§4§lADMIN " : name;
    }

    public String getPrefix() {
        return isAprilFools() ? "§4§lADMIN " : prefix;
    }

    /**
     * Get Default <plus> formatted group display name
     *
     * @return - Default <plus> formatted group display name
     */
    public String getDefaultFormattedPlusDisplayName() {
        if (name.contains("<plus>")) {
            return name.replace("<plus>", "§c+");
        }
        return name;
    }

    public boolean isStaff() {
        return level >= HELPER.getLevel();
    }

    private boolean isAprilFools() {
        val now = LocalDateTime.now();
        return now.getMonth() == Month.APRIL && now.getDayOfMonth() == 1;
    }
}
