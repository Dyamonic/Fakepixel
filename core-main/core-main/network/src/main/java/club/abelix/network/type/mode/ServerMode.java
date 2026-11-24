package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.NonNull;

import java.util.List;

public interface ServerMode {

    /**
     * Master - 0, Lobby - 1, Mini - 2, Uncommon - 3
     */
    int getTypeId();

    /**
     * Имя
     * <br>
     * <br>Master
     * <ul>
     *   <li>BW</li>
     *   <li>EW</li>
     *   <li>SW</li>
     *   <li>etc...</li>
     * </ul>
     * <br>Mini
     * <ul>
     *   <li>BWS</li>
     *   <li>EWS</li>
     *   <li>SWS</li>
     *   <li>etc...</li>
     * </ul>
     * <br>Uncommon
     * <ul>
     *   <li>SKYBLOCK</li>
     *   <li>ANARCHY</li>
     *   <li>etc...</li>
     * </ul>
     */
    String getName();

    /**
     * Видимое имя (Например в борде)
     * <br>
     * <br>Master
     * <ul>
     *   <li>BedWars</li>
     *   <li>EggWars</li>
     *   <li>SkyWars</li>
     *   <li>etc...</li>
     * </ul>
     * <br>Mini
     * <ul>
     *   <li>BedWars Solo</li>
     *   <li>EggWars Solo</li>
     *   <li>SkyWars Solo</li>
     *   <li>etc...</li>
     * </ul>
     * <br>Uncommon
     * <ul>
     *   <li>SkyBlock</li>
     *   <li>Anarchy</li>
     *   <li>etc...</li>
     * </ul>
     */
    String getDisplayName();

    String getServerName();

    List<String> getDescription(@NonNull UserLocale locale);

    default boolean isSpecial() {
        return false;
    }

    default boolean isUnknown() {
        return false;
    }

    default boolean isGameLobby() {
        return false;
    }

    default boolean isLobby() {
        return false;
    }

    default boolean isUncommon() {
        return false;
    }

    default boolean isMini() {
        return false;
    }

    default boolean isMaster() {
        return false;
    }

}
