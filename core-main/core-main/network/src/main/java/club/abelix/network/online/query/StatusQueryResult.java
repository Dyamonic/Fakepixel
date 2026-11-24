package club.abelix.network.online.query;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class StatusQueryResult implements Comparator<StatusQueryResult> {

    public static final StatusQueryResult EMPTY =
            new StatusQueryResult(0, Collections.emptySet(), Collections.emptyMap(), false);

    int totalOnline;
    Set<String> lookedUpServers;
    Map<String, PerServer> perServer;
    boolean cached;

    public boolean isEmpty() {
        return lookedUpServers.isEmpty();
    }

    @Override
    public int compare(StatusQueryResult o1, StatusQueryResult o2) {
        return Integer.compare(o1.totalOnline, o2.totalOnline);
    }

    @Getter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    public static class PerServer implements Comparator<PerServer> {
        String id;
        String name;
        String mode;
        int online;
        int vanishedOnline;
        int slots;

        @Override
        public int compare(PerServer o1, PerServer o2) {
            return Integer.compare(o1.online, o2.online);
        }
    }

    @Getter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ArenaPerServer extends PerServer {

        public static ArenaPerServer ofParent(
                final @NotNull PerServer parent,
                final @NotNull String mapName,
                final @NotNull String arenaState
        ) {
            return new ArenaPerServer(
                    parent.getId(),
                    parent.getName(),
                    mapName,
                    parent.getMode(),
                    arenaState,
                    parent.getOnline(),
                    parent.getVanishedOnline(),
                    parent.getSlots()
            );

        }

        String id;
        String name;
        String mapName;
        String arenaState;
        int online;
        int vanishedOnline;
        int slots;

        private ArenaPerServer(
                final @NotNull String id,
                final @NotNull String name,
                final @NotNull String mapName,
                final @NotNull String mode,
                final @NotNull String arenaState,
                final int online,
                final int vanishedOnline,
                final int slots
        ) {
            super(
                    id,
                    name,
                    mode,
                    online,
                    vanishedOnline,
                    slots
            );

            this.id = id;
            this.name = name;
            this.mapName = mapName;
            this.arenaState = arenaState;
            this.online = online;
            this.vanishedOnline = vanishedOnline;
            this.slots = slots;
        }

    }
}
