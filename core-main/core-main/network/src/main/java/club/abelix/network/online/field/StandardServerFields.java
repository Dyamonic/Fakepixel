package club.abelix.network.online.field;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public enum StandardServerFields implements ServerField {

    ID("id", false, false),
    NAME("name", false, false),
    MODE("mode", false, true),
    KIND("kind", false, true),
    PARENT("parentServer", false, true),

    TAG("tag", false, true) {
        @Override
        public boolean isMultiValued() {
            return true;
        }
    },

    SLOTS("slots", false, false),

    ONLINE("online", true, false),
    VANISHED_ONLINE("vanishedOnline", true, false),
    LAST_HEARTBEAT("lastHeartbeat", true, false),

    ARENA_STATE("arenaState", true, true),
    MAP_NAME("mapName", true, true)
    ;

    String fieldName;
    boolean dynamic;
    boolean indexed;

}
