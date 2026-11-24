package club.abelix.network.util;

import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.type.mode.Unknown;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ModeUtil {

    private final Map<String, ServerMode> MODE_BY_NAME
            = new HashMap<>();

    static {
        for (Master master : Master.VALUES) {
            registerMode(master);
        }

        for (Lobby lobby : Lobby.VALUES) {
            registerMode(lobby);
        }

        for (Mini mini : Mini.VALUES) {
            registerMode(mini);
        }

        for (Uncommon uncommon : Uncommon.VALUES) {
            registerMode(uncommon);
        }

        for (Special special : Special.VALUES) {
            registerMode(special);
        }
    }

    private void registerMode(@NotNull ServerMode mode) {
        MODE_BY_NAME.put(mode.getName().toUpperCase(), mode);
    }

    @NotNull
    public ServerMode getServerMode(String text) {
        ServerMode mode = MODE_BY_NAME.get(text.toUpperCase());

        if (mode == null) {
            return new Unknown(text);
        }

        return mode;
    }

    @NotNull
    public ServerMode getServerModeByName(String name) {
        int i = name.indexOf('-');

        return getServerMode(i == -1 ? name : name.substring(0, i));
    }

}
