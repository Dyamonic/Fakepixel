package club.abelix.network.online.info;

import club.abelix.network.online.data.Ids;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.ServerField;
import club.abelix.network.online.field.StandardServerFields;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArenaServerInfo extends AbstractServerInfo {

    public static ArenaServerInfo ofArena(
            final @NotNull ArenaServerInfo arenaServerInfo,
            final int online,
            final int vanishedOnline,
            final @NotNull String arenaState,
            final @NotNull String mapName
    ) {
        return new ArenaServerInfo(
                arenaServerInfo.getId(),
                arenaServerInfo.getServerName(),
                arenaServerInfo.getServerMode(),
                arenaServerInfo.getKind(),
                arenaServerInfo.getTags(),
                arenaServerInfo.getSlots(),
                online,
                vanishedOnline,
                arenaServerInfo.getParent(),
                arenaServerInfo.getArenaName(),
                arenaState,
                mapName
        );
    }

    public static ArenaServerInfo ofParent(
            final @NotNull ServerInfo parent,
            final @NotNull String mode,
            final @NotNull List<String> tags,
            final int slots,
            final int online,
            final int vanishedOnline,
            final @NotNull String arenaName,
            final @NotNull String arenaState,
            final @NotNull String mapName
    ) {
        val arenaNameParts = arenaName.split("-");
        val identifier = arenaName.split("-")[1];
        val identifierParts = identifier.split("(?<=\\D)(?=\\d)");

        return new ArenaServerInfo(
                Ids.arenaId(arenaNameParts[0], identifierParts[0], Integer.parseInt(identifierParts[1])),
                parent.getServerName(),
                mode,
                ServerKind.ARENA,
                tags,
                slots,
                online,
                vanishedOnline,
                parent.getServerName(),
                arenaName,
                arenaState,
                mapName
        );

    }

    String parent;
    String arenaName;
    String arenaState;
    String mapName;

    private ArenaServerInfo(
            final @NotNull String id,
            final @NotNull String serverName,
            final @NotNull String serverMode,
            final @NotNull ServerKind kind,
            final @NotNull List<@NotNull String> tags,
            final int slots,
            final int online,
            final int vanishedOnline, String parent,
            final @NotNull String arenaName,
            final @NotNull String arenaState,
            final @NotNull String mapName
    ) {
        super(
                id,
                serverName,
                serverMode,
                kind,
                tags,
                slots,
                online,
                vanishedOnline
        );

        this.parent = parent;
        this.arenaState = arenaState;
        this.mapName = mapName;
        this.arenaName = arenaName;
    }

    @Override
    public Map<ServerField, String> getDynamicData() {
        val data = new HashMap<>(super.getDynamicData());

        data.put(StandardServerFields.ARENA_STATE, arenaState);
        data.put(StandardServerFields.MAP_NAME, mapName);

        return data;
    }

    @Override
    public Map<ServerField, String> getStaticData() {
        val data = new HashMap<>(super.getStaticData());

        data.put(StandardServerFields.NAME, arenaName);
        data.put(StandardServerFields.PARENT, parent);

        return data;
    }

    @Override
    public String getServerName() {
        return arenaName;
    }
}
